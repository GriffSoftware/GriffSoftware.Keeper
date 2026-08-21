package com.griff.keeper.presentation.datatransfer

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.presentation.common.UiMessage
import java.time.Instant

/**
 * What the screen is doing right now.
 *
 * A single value rather than a handful of booleans: an export and an import must never overlap, and
 * "which one is running" is one question with one answer. Every control on the screen is disabled
 * while this is anything but [IDLE], which is also what makes a double tap a no-op.
 */
enum class DataTransferStage {
    IDLE,

    /** Collecting, serializing, encrypting and writing to the document the user chose. */
    EXPORTING,

    /** The same, but staging the result for another app to pick up. */
    SHARING,

    /** Reading the picked file and checking that it is a backup at all. */
    READING_FILE,

    /** Deriving the key, decrypting, validating - everything the password unlocks. */
    OPENING_BACKUP,

    /** Writing the records. The only stage that changes local data. */
    IMPORTING,
    ;

    val isBusy: Boolean get() = this != IDLE
}

/** Which modal the screen is showing. Only one can be open at a time. */
enum class DataTransferDialog {
    NONE,
    EXPORT_PASSWORD,
    SHARE_PASSWORD,
    IMPORT_PASSWORD,
    IMPORT_PREVIEW,
    REPLACE_CONFIRMATION,
    OFFLINE_SHARE_WARNING,
}

/** One row of the operation history. */
data class BackupHistoryItemUi(
    val id: String,
    val type: BackupOperationType,
    val status: BackupOperationStatus,
    val importMode: ImportMode?,
    val finishedAt: Instant,
    val subscriptionCount: Int,
    val obligationCount: Int,
    val errorType: BackupErrorType?,
) {
    val isSuccess: Boolean get() = status == BackupOperationStatus.SUCCESS
}

/**
 * What a backup says about itself, ready to be shown before the user decides.
 *
 * A projection of the preview rather than the preview itself: the decrypted records stay in the
 * ViewModel and never enter the state that Compose compares on every recomposition.
 */
data class ImportPreviewUi(
    val fileName: String?,
    val createdAt: Instant,
    val appVersion: String,
    val subscriptionCount: Int,
    val obligationCount: Int,
    val hasSettings: Boolean,
    val hasLocalData: Boolean,
    val possibleDuplicates: Int,
) {
    val recordCount: Int get() = subscriptionCount + obligationCount
}

data class DataTransferUiState(
    val stage: DataTransferStage = DataTransferStage.IDLE,
    val dialog: DataTransferDialog = DataTransferDialog.NONE,
    val isHistoryLoading: Boolean = true,
    val history: List<BackupHistoryItemUi> = emptyList(),
    val preview: ImportPreviewUi? = null,
    /** Shown inside the import password dialog, so a typo is corrected without losing the file. */
    val passwordError: UiMessage? = null,
    /** Transient feedback, handed to the shared snackbar host. */
    val message: UiMessage? = null,
    /**
     * Set when no application can take the share intent.
     *
     * Kept in the state rather than shown as a snackbar: the answer includes an alternative - save
     * the file instead - and an action the user may want a moment to consider does not belong in
     * something that disappears on its own.
     */
    val shareUnavailable: Boolean = false,
) {
    val isBusy: Boolean get() = stage.isBusy

    val areActionsEnabled: Boolean get() = !isBusy

    val isHistoryEmpty: Boolean get() = !isHistoryLoading && history.isEmpty()
}
