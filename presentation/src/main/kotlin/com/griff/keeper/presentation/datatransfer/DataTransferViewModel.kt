package com.griff.keeper.presentation.datatransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.keeper.application.backup.BackupPreview
import com.griff.keeper.application.backup.ClearSharedBackupsUseCase
import com.griff.keeper.application.backup.ExportBackupUseCase
import com.griff.keeper.application.backup.ImportBackupUseCase
import com.griff.keeper.application.backup.IsNetworkAvailableUseCase
import com.griff.keeper.application.backup.ObserveBackupHistoryUseCase
import com.griff.keeper.application.backup.PreviewBackupUseCase
import com.griff.keeper.application.backup.ShareBackupUseCase
import com.griff.keeper.application.backup.ValidateBackupFileUseCase
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupOperation
import com.griff.keeper.domain.backup.BackupSink
import com.griff.keeper.domain.backup.BackupSource
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.SharedBackupFile
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.time.ClockProvider
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Things only the platform can do, which the ViewModel asks for rather than performs.
 *
 * Every one of them is an `Intent` in disguise. Keeping them as events is what lets the ViewModel
 * stay free of `Uri`, `ActivityResultLauncher` and `Intent`, and it is also what makes them happen
 * exactly once: nothing is replayed on a rotation, so the picker cannot open twice.
 */
sealed interface DataTransferEvent {

    /** Open the system "create document" picker with [suggestedFileName] pre-filled. */
    data class CreateDocument(val suggestedFileName: String) : DataTransferEvent

    /** Open the system document picker to choose a backup to import. */
    data object OpenDocument : DataTransferEvent

    /** Hand [file] to the system chooser, optionally addressed to [recipient]. */
    data class ShareBackup(val file: SharedBackupFile, val recipient: String?) : DataTransferEvent
}

/**
 * Drives the Import / Export screen.
 *
 * ### The password never lives here for longer than one operation
 *
 * A backup password is held in a plain field as a [CharArray], is overwritten the moment the
 * operation that needed it finishes - success or failure - and is never written to a
 * `SavedStateHandle`, a `Bundle`, DataStore, the database or a log. It exists between two callbacks
 * for one reason: it has to survive the trip through the system file picker. If the process is killed
 * during that trip the password is simply gone, and the export reports that it did not complete
 * rather than quietly carrying on without it.
 *
 * ### Nothing overlaps
 *
 * [DataTransferStage] is a single value and every entry point returns early while it is busy, so an
 * export cannot start during an import, an import cannot start twice, and a double tap is a no-op.
 */
@HiltViewModel
class DataTransferViewModel @Inject constructor(
    observeHistory: ObserveBackupHistoryUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val shareBackup: ShareBackupUseCase,
    private val clearSharedBackups: ClearSharedBackupsUseCase,
    private val validateBackupFile: ValidateBackupFileUseCase,
    private val previewBackup: PreviewBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val isNetworkAvailable: IsNetworkAvailableUseCase,
    private val clock: ClockProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataTransferUiState())
    val uiState: StateFlow<DataTransferUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DataTransferEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<DataTransferEvent> = _events.asSharedFlow()

    /** Alive only between the password dialog and the end of the operation it belongs to. */
    private var pendingPassword: CharArray? = null

    /** The file the user picked, kept until the import it belongs to is decided or abandoned. */
    private var pendingSource: BackupSource? = null

    /** The decrypted payload behind [DataTransferUiState.preview]. Never part of the state. */
    private var pendingPreview: BackupPreview? = null

    /** Set while a share is in flight, so the address survives the encryption step. */
    private var pendingRecipient: String? = null

    /** Staged file waiting for the chooser, kept so the offline warning can still hand it over. */
    private var pendingShare: SharedBackupFile? = null

    init {
        viewModelScope.launch {
            observeHistory()
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update {
                        it.copy(
                            isHistoryLoading = false,
                            message = UiMessage(
                                R.string.error_load_failed,
                                severity = MessageSeverity.ERROR,
                            ),
                        )
                    }
                }
                .collect(::onHistoryLoaded)
        }
    }

    // --- Export to a file ------------------------------------------------------------------

    fun onExportRequested() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(dialog = DataTransferDialog.EXPORT_PASSWORD) }
    }

    fun onExportPasswordConfirmed(password: CharArray) {
        if (_uiState.value.isBusy) return
        clearPendingPassword()
        pendingPassword = password
        _uiState.update { it.copy(dialog = DataTransferDialog.NONE) }
        _events.tryEmit(DataTransferEvent.CreateDocument(suggestedFileName()))
    }

    /**
     * The user backed out of the system picker.
     *
     * Not a failure: nothing was attempted, so nothing goes into the history and nothing is said on
     * screen. The password is dropped, because the operation it was for is over.
     */
    fun onExportCancelled() {
        clearPendingPassword()
        _uiState.update { it.copy(stage = DataTransferStage.IDLE) }
    }

    fun onExportDestinationChosen(sink: BackupSink, fileName: String) {
        if (_uiState.value.isBusy) return
        val password = pendingPassword
        if (password == null) {
            // The password did not survive the trip through the picker, which in practice means the
            // process was killed. Saying the export did not finish is better than guessing.
            failExport(BackupErrorType.UNKNOWN)
            return
        }

        _uiState.update {
            it.copy(stage = DataTransferStage.EXPORTING, dialog = DataTransferDialog.NONE)
        }
        viewModelScope.launch {
            try {
                exportBackup(password, fileName, sink)
                    .onSuccess {
                        _uiState.update { current ->
                            current.copy(
                                stage = DataTransferStage.IDLE,
                                message = UiMessage(
                                    R.string.data_transfer_export_success,
                                    severity = MessageSeverity.SUCCESS,
                                ),
                            )
                        }
                    }
                    .onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        failExport(throwable.backupErrorType)
                    }
            } finally {
                clearPendingPassword()
            }
        }
    }

    // --- Export by e-mail -----------------------------------------------------------------

    fun onShareRequested() {
        if (_uiState.value.isBusy) return
        _uiState.update {
            it.copy(dialog = DataTransferDialog.SHARE_PASSWORD, shareUnavailable = false)
        }
    }

    fun onSharePasswordConfirmed(password: CharArray, recipient: String?) {
        if (_uiState.value.isBusy) return
        pendingRecipient = recipient
        _uiState.update {
            it.copy(stage = DataTransferStage.SHARING, dialog = DataTransferDialog.NONE)
        }

        viewModelScope.launch {
            try {
                shareBackup(password, suggestedFileName())
                    .onSuccess { result ->
                        pendingShare = result.file
                        _uiState.update { it.copy(stage = DataTransferStage.IDLE) }
                        // The file exists either way. The warning is about the message leaving,
                        // which is not something this app controls.
                        if (isNetworkAvailable()) {
                            emitShareEvent()
                        } else {
                            _uiState.update {
                                it.copy(dialog = DataTransferDialog.OFFLINE_SHARE_WARNING)
                            }
                        }
                    }
                    .onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        discardStagedShare()
                        _uiState.update {
                            it.copy(
                                stage = DataTransferStage.IDLE,
                                message = throwable.backupErrorType.exportMessage(),
                            )
                        }
                    }
            } finally {
                password.fill(' ')
            }
        }
    }

    /** The user read the offline warning and still wants to open their mail app. */
    fun onOfflineShareConfirmed() {
        _uiState.update { it.copy(dialog = DataTransferDialog.NONE) }
        emitShareEvent()
    }

    fun onOfflineShareCancelled() {
        _uiState.update { it.copy(dialog = DataTransferDialog.NONE) }
        discardStagedShare()
    }

    /**
     * No installed application can take the share intent.
     *
     * The backup itself was created successfully, so the message says so and offers the way out that
     * still works: save it to a file and send it by hand.
     */
    fun onShareTargetUnavailable() {
        discardStagedShare()
        _uiState.update { it.copy(shareUnavailable = true) }
    }

    fun onShareUnavailableDismissed() = _uiState.update { it.copy(shareUnavailable = false) }

    // --- Import ---------------------------------------------------------------------------

    fun onImportRequested() {
        if (_uiState.value.isBusy) return
        _events.tryEmit(DataTransferEvent.OpenDocument)
    }

    /** The user backed out of the picker. Nothing happened, so nothing is reported. */
    fun onImportCancelled() {
        discardPendingImport()
        _uiState.update { it.copy(stage = DataTransferStage.IDLE) }
    }

    /**
     * A file was picked, and is checked for being a backup at all *before* the password is asked for.
     *
     * Asking first would turn "this is a photo" into "wrong password", which sends the user looking
     * for a mistake they did not make - and would spend the deliberately slow key derivation on a
     * file that was never going to open.
     */
    fun onImportFileChosen(source: BackupSource) {
        if (_uiState.value.isBusy) return
        pendingSource = source
        _uiState.update { it.copy(stage = DataTransferStage.READING_FILE) }

        viewModelScope.launch {
            validateBackupFile(source)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            stage = DataTransferStage.IDLE,
                            dialog = DataTransferDialog.IMPORT_PASSWORD,
                            passwordError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    discardPendingImport()
                    _uiState.update {
                        it.copy(
                            stage = DataTransferStage.IDLE,
                            message = throwable.backupErrorType.importMessage(),
                        )
                    }
                }
        }
    }

    fun onImportPasswordConfirmed(password: CharArray) {
        val source = pendingSource
        if (source == null || _uiState.value.isBusy) return

        _uiState.update {
            it.copy(
                stage = DataTransferStage.OPENING_BACKUP,
                dialog = DataTransferDialog.NONE,
                passwordError = null,
            )
        }

        viewModelScope.launch {
            try {
                previewBackup(source, password)
                    .onSuccess { preview ->
                        pendingPreview = preview
                        _uiState.update {
                            it.copy(
                                stage = DataTransferStage.IDLE,
                                dialog = DataTransferDialog.IMPORT_PREVIEW,
                                preview = preview.toUi(),
                            )
                        }
                    }
                    .onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        onPreviewFailed(throwable.backupErrorType)
                    }
            } finally {
                password.fill(' ')
            }
        }
    }

    fun onImportPasswordCancelled() {
        discardPendingImport()
        _uiState.update {
            it.copy(
                dialog = DataTransferDialog.NONE,
                stage = DataTransferStage.IDLE,
                passwordError = null,
            )
        }
    }

    fun onImportPreviewDismissed() {
        discardPendingImport()
        _uiState.update { it.copy(dialog = DataTransferDialog.NONE, preview = null) }
    }

    /** Chosen from the preview. With no local data this is simply "restore". */
    fun onMergeSelected() = startImport(ImportMode.MERGE)

    /** Destructive, so the preview only asks for it - the second dialog is what confirms it. */
    fun onReplaceSelected() {
        if (pendingPreview == null) return
        _uiState.update { it.copy(dialog = DataTransferDialog.REPLACE_CONFIRMATION) }
    }

    fun onReplaceCancelled() {
        // Back to the preview rather than out of the flow: dismissing the warning is a step back, not
        // an abandoned import, and the user must not lose the file they picked over it.
        _uiState.update { it.copy(dialog = DataTransferDialog.IMPORT_PREVIEW) }
    }

    fun onReplaceConfirmed() = startImport(ImportMode.REPLACE)

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    override fun onCleared() {
        clearPendingPassword()
        super.onCleared()
    }

    // --- Internals ------------------------------------------------------------------------

    private fun startImport(mode: ImportMode) {
        val preview = pendingPreview
        if (preview == null || _uiState.value.isBusy) return

        _uiState.update {
            it.copy(stage = DataTransferStage.IMPORTING, dialog = DataTransferDialog.NONE)
        }

        viewModelScope.launch {
            importBackup(preview.payload, mode, preview.fileName)
                .onSuccess {
                    discardPendingImport()
                    _uiState.update {
                        it.copy(
                            stage = DataTransferStage.IDLE,
                            preview = null,
                            message = UiMessage(
                                textRes = when (mode) {
                                    ImportMode.MERGE ->
                                        R.string.data_transfer_import_merge_success

                                    ImportMode.REPLACE ->
                                        R.string.data_transfer_import_replace_success
                                },
                                severity = MessageSeverity.SUCCESS,
                            ),
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    discardPendingImport()
                    _uiState.update {
                        it.copy(
                            stage = DataTransferStage.IDLE,
                            preview = null,
                            message = throwable.backupErrorType.importMessage(),
                        )
                    }
                }
        }
    }

    /**
     * A wrong password keeps the dialog open with the file still selected; anything else ends the
     * attempt. Making the user find the file again because of a typo would be a punishment for a typo.
     */
    private fun onPreviewFailed(errorType: BackupErrorType) {
        if (errorType == BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED) {
            _uiState.update {
                it.copy(
                    stage = DataTransferStage.IDLE,
                    dialog = DataTransferDialog.IMPORT_PASSWORD,
                    passwordError = UiMessage(
                        R.string.data_transfer_import_wrong_password,
                        severity = MessageSeverity.ERROR,
                    ),
                )
            }
            return
        }

        discardPendingImport()
        _uiState.update {
            it.copy(
                stage = DataTransferStage.IDLE,
                dialog = DataTransferDialog.NONE,
                message = errorType.importMessage(),
            )
        }
    }

    private fun emitShareEvent() {
        val file = pendingShare ?: return
        _events.tryEmit(DataTransferEvent.ShareBackup(file, pendingRecipient))
        // The address has been handed over and there is no path that needs it again; an e-mail
        // address the user typed is not something to keep around once it has been used.
        pendingRecipient = null
    }

    private fun discardStagedShare() {
        pendingShare = null
        pendingRecipient = null
        viewModelScope.launch { clearSharedBackups() }
    }

    private fun failExport(errorType: BackupErrorType) {
        clearPendingPassword()
        _uiState.update {
            it.copy(stage = DataTransferStage.IDLE, message = errorType.exportMessage())
        }
    }

    private fun discardPendingImport() {
        pendingSource = null
        pendingPreview = null
    }

    private fun clearPendingPassword() {
        pendingPassword?.fill(' ')
        pendingPassword = null
    }

    private fun suggestedFileName(): String =
        // atZone rather than LocalDateTime.ofInstant, which needs API 31.
        BackupFormat.fileName(clock.now().atZone(clock.zone()).toLocalDateTime())

    private fun onHistoryLoaded(operations: List<BackupOperation>) {
        _uiState.update { current ->
            current.copy(
                isHistoryLoading = false,
                history = operations.map { operation ->
                    BackupHistoryItemUi(
                        id = operation.id,
                        type = operation.type,
                        status = operation.status,
                        importMode = operation.importMode,
                        finishedAt = operation.finishedAt,
                        subscriptionCount = operation.subscriptionCount,
                        obligationCount = operation.obligationCount,
                        errorType = operation.errorType,
                    )
                },
            )
        }
    }

    private fun BackupPreview.toUi() = ImportPreviewUi(
        fileName = fileName,
        createdAt = summary.createdAt,
        appVersion = summary.appVersion,
        subscriptionCount = summary.subscriptionCount,
        obligationCount = summary.obligationCount,
        hasSettings = summary.hasSettings,
        hasLocalData = hasLocalData,
        possibleDuplicates = possibleDuplicates,
    )

    private fun BackupErrorType.exportMessage() = UiMessage(
        textRes = when (this) {
            BackupErrorType.INSUFFICIENT_STORAGE -> R.string.data_transfer_error_no_space_export
            else -> R.string.data_transfer_error_export_failed
        },
        severity = MessageSeverity.ERROR,
    )

    private fun BackupErrorType.importMessage() = UiMessage(
        textRes = BackupErrorLabels.messageRes(this),
        severity = MessageSeverity.ERROR,
    )
}
