package com.griff.keeper.presentation.datatransfer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.SharedBackupFile
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.EmptyState
import com.griff.keeper.presentation.common.component.GriffSnackbarHost
import com.griff.keeper.presentation.common.component.showMessage
import com.griff.keeper.presentation.common.resolve
import com.griff.keeper.presentation.datatransfer.components.BackupHistoryRow
import com.griff.keeper.presentation.datatransfer.components.BackupPasswordDialog
import com.griff.keeper.presentation.datatransfer.components.DataLocalityCard
import com.griff.keeper.presentation.datatransfer.components.DataSecurityCard
import com.griff.keeper.presentation.datatransfer.components.DataTransferAction
import com.griff.keeper.presentation.datatransfer.components.DataTransferActionCard
import com.griff.keeper.presentation.datatransfer.components.ImportPasswordDialog
import com.griff.keeper.presentation.datatransfer.components.ImportPreviewDialog
import com.griff.keeper.presentation.datatransfer.components.OfflineShareWarningDialog
import com.griff.keeper.presentation.datatransfer.components.ReplaceConfirmationDialog
import com.griff.keeper.presentation.datatransfer.components.ShareUnavailableCard
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.ZoneId

/**
 * Entry point of the Import / Export screen.
 *
 * This is where the platform lives. The three system interactions - create a document, open a
 * document, hand a file to another app - are launched from here in response to events, so the
 * ViewModel below never touches a `Uri`, an `Intent` or an `ActivityResultLauncher`, and the picked
 * document is handed down as a plain stream abstraction.
 *
 * No storage permission is requested, and none is needed: a document the user selects in the system
 * picker arrives with its own access grant for that one file. The screen says so in as many words
 * instead of asking for something it does not require.
 */
@Composable
fun DataTransferRoute(
    onOpenDrawer: () -> Unit,
    viewModel: DataTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resolver = context.contentResolver
    val fallbackFileName = stringResource(R.string.data_transfer_history_export)

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFormat.MIME_TYPE),
    ) { uri ->
        // A null result is the user pressing Back in the picker. Nothing was attempted, so it is not
        // an error and nothing is recorded.
        if (uri == null) {
            viewModel.onExportCancelled()
        } else {
            viewModel.onExportDestinationChosen(
                sink = uri.asBackupSink(resolver),
                // The picker may have renamed the file, so the history records what was actually
                // written rather than what was suggested.
                fileName = uri.documentDisplayName(resolver) ?: fallbackFileName,
            )
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            viewModel.onImportCancelled()
        } else {
            viewModel.onImportFileChosen(uri.asBackupSource(resolver))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DataTransferEvent.CreateDocument ->
                    createDocument.launch(event.suggestedFileName)

                // Anything at all, because the extension is private to the app and no provider knows
                // it; the file's own contents are what identify a real backup.
                DataTransferEvent.OpenDocument -> openDocument.launch(arrayOf(ANY_MIME_TYPE))

                is DataTransferEvent.ShareBackup -> {
                    val launched = context.startBackupShare(event.file, event.recipient)
                    if (!launched) viewModel.onShareTargetUnavailable()
                }
            }
        }
    }

    DataTransferScreen(
        state = state,
        zone = remember { ZoneId.systemDefault() },
        onOpenDrawer = onOpenDrawer,
        onExportRequested = viewModel::onExportRequested,
        onShareRequested = viewModel::onShareRequested,
        onImportRequested = viewModel::onImportRequested,
        onExportPasswordConfirmed = { password, _ ->
            viewModel.onExportPasswordConfirmed(password)
        },
        onSharePasswordConfirmed = viewModel::onSharePasswordConfirmed,
        onImportPasswordConfirmed = viewModel::onImportPasswordConfirmed,
        onPasswordDialogDismissed = viewModel::onExportCancelled,
        onImportPasswordDismissed = viewModel::onImportPasswordCancelled,
        onPreviewDismissed = viewModel::onImportPreviewDismissed,
        onMergeSelected = viewModel::onMergeSelected,
        onReplaceSelected = viewModel::onReplaceSelected,
        onReplaceConfirmed = viewModel::onReplaceConfirmed,
        onReplaceCancelled = viewModel::onReplaceCancelled,
        onOfflineShareConfirmed = viewModel::onOfflineShareConfirmed,
        onOfflineShareCancelled = viewModel::onOfflineShareCancelled,
        onShareUnavailableDismissed = viewModel::onShareUnavailableDismissed,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataTransferScreen(
    state: DataTransferUiState,
    zone: ZoneId,
    onOpenDrawer: () -> Unit,
    onExportRequested: () -> Unit,
    onShareRequested: () -> Unit,
    onImportRequested: () -> Unit,
    onExportPasswordConfirmed: (CharArray, String?) -> Unit,
    onSharePasswordConfirmed: (CharArray, String?) -> Unit,
    onImportPasswordConfirmed: (CharArray) -> Unit,
    onPasswordDialogDismissed: () -> Unit,
    onImportPasswordDismissed: () -> Unit,
    onPreviewDismissed: () -> Unit,
    onMergeSelected: () -> Unit,
    onReplaceSelected: () -> Unit,
    onReplaceConfirmed: () -> Unit,
    onReplaceCancelled: () -> Unit,
    onOfflineShareConfirmed: () -> Unit,
    onOfflineShareCancelled: () -> Unit,
    onShareUnavailableDismissed: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.resolve()

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showMessage(message)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.data_transfer_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.open_menu),
                        )
                    }
                },
            )
        },
        snackbarHost = { GriffSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            DataTransferContent(
                state = state,
                zone = zone,
                onExportRequested = onExportRequested,
                onShareRequested = onShareRequested,
                onImportRequested = onImportRequested,
                onShareUnavailableDismissed = onShareUnavailableDismissed,
            )

            // Sits above the content rather than replacing it: an export takes a moment, and a
            // screen that vanishes mid-operation feels like something went wrong.
            if (state.isBusy) {
                BusyOverlay(stage = state.stage)
            }
        }
    }

    DataTransferDialogs(
        state = state,
        zone = zone,
        onExportPasswordConfirmed = onExportPasswordConfirmed,
        onSharePasswordConfirmed = onSharePasswordConfirmed,
        onImportPasswordConfirmed = onImportPasswordConfirmed,
        onPasswordDialogDismissed = onPasswordDialogDismissed,
        onImportPasswordDismissed = onImportPasswordDismissed,
        onPreviewDismissed = onPreviewDismissed,
        onMergeSelected = onMergeSelected,
        onReplaceSelected = onReplaceSelected,
        onReplaceConfirmed = onReplaceConfirmed,
        onReplaceCancelled = onReplaceCancelled,
        onOfflineShareConfirmed = onOfflineShareConfirmed,
        onOfflineShareCancelled = onOfflineShareCancelled,
    )
}

@Composable
private fun DataTransferContent(
    state: DataTransferUiState,
    zone: ZoneId,
    onExportRequested: () -> Unit,
    onShareRequested: () -> Unit,
    onImportRequested: () -> Unit,
    onShareUnavailableDismissed: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        item(key = "intro") {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.Large),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
            ) {
                Text(
                    text = stringResource(R.string.data_transfer_intro_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.data_transfer_intro_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "export") {
            DataTransferActionCard(
                action = DataTransferAction.EXPORT,
                enabled = state.areActionsEnabled,
                onClick = onExportRequested,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }

        item(key = "share") {
            DataTransferActionCard(
                action = DataTransferAction.SHARE,
                enabled = state.areActionsEnabled,
                onClick = onShareRequested,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }

        item(key = "import") {
            DataTransferActionCard(
                action = DataTransferAction.IMPORT,
                enabled = state.areActionsEnabled,
                onClick = onImportRequested,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }

        item(key = "picker-hint") {
            Text(
                text = stringResource(R.string.data_transfer_picker_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }

        if (state.shareUnavailable) {
            item(key = "share-unavailable") {
                ShareUnavailableCard(
                    onSaveToFile = onExportRequested,
                    onDismiss = onShareUnavailableDismissed,
                    modifier = Modifier.padding(horizontal = Spacing.Large),
                )
            }
        }

        item(key = "security") {
            DataSecurityCard(modifier = Modifier.padding(horizontal = Spacing.Large))
        }

        item(key = "locality") {
            DataLocalityCard(modifier = Modifier.padding(horizontal = Spacing.Large))
        }

        item(key = "history-divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))
        }

        item(key = "history-header") {
            Text(
                text = stringResource(R.string.data_transfer_history_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }

        when {
            state.isHistoryLoading -> item(key = "history-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.ExtraLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.isHistoryEmpty -> item(key = "history-empty") {
                EmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.data_transfer_history_empty_title),
                    description = stringResource(
                        R.string.data_transfer_history_empty_description,
                    ),
                )
            }

            else -> items(items = state.history, key = { it.id }) { item ->
                BackupHistoryRow(item = item, zone = zone)
            }
        }
    }
}

@Composable
private fun DataTransferDialogs(
    state: DataTransferUiState,
    zone: ZoneId,
    onExportPasswordConfirmed: (CharArray, String?) -> Unit,
    onSharePasswordConfirmed: (CharArray, String?) -> Unit,
    onImportPasswordConfirmed: (CharArray) -> Unit,
    onPasswordDialogDismissed: () -> Unit,
    onImportPasswordDismissed: () -> Unit,
    onPreviewDismissed: () -> Unit,
    onMergeSelected: () -> Unit,
    onReplaceSelected: () -> Unit,
    onReplaceConfirmed: () -> Unit,
    onReplaceCancelled: () -> Unit,
    onOfflineShareConfirmed: () -> Unit,
    onOfflineShareCancelled: () -> Unit,
) {
    when (state.dialog) {
        DataTransferDialog.NONE -> Unit

        DataTransferDialog.EXPORT_PASSWORD -> BackupPasswordDialog(
            confirmLabelRes = R.string.data_transfer_password_create,
            recipientField = false,
            onConfirm = onExportPasswordConfirmed,
            onDismiss = onPasswordDialogDismissed,
        )

        DataTransferDialog.SHARE_PASSWORD -> BackupPasswordDialog(
            confirmLabelRes = R.string.data_transfer_share_action,
            recipientField = true,
            onConfirm = onSharePasswordConfirmed,
            onDismiss = onPasswordDialogDismissed,
        )

        DataTransferDialog.IMPORT_PASSWORD -> ImportPasswordDialog(
            errorText = state.passwordError?.resolve()?.text,
            onConfirm = onImportPasswordConfirmed,
            onDismiss = onImportPasswordDismissed,
        )

        DataTransferDialog.IMPORT_PREVIEW -> state.preview?.let { preview ->
            ImportPreviewDialog(
                preview = preview,
                zone = zone,
                onMerge = onMergeSelected,
                onReplace = onReplaceSelected,
                onDismiss = onPreviewDismissed,
            )
        }

        DataTransferDialog.REPLACE_CONFIRMATION -> ReplaceConfirmationDialog(
            onConfirm = onReplaceConfirmed,
            onDismiss = onReplaceCancelled,
        )

        DataTransferDialog.OFFLINE_SHARE_WARNING -> OfflineShareWarningDialog(
            onConfirm = onOfflineShareConfirmed,
            onDismiss = onOfflineShareCancelled,
        )
    }
}

/**
 * Says which part of the work is running.
 *
 * No percentage: reading, deriving a key and writing rows are three different kinds of work, and a
 * bar that jumps between them in made-up proportions tells the user less than naming the step does.
 */
@Composable
private fun BusyOverlay(stage: DataTransferStage) {
    val label = when (stage) {
        DataTransferStage.EXPORTING,
        DataTransferStage.SHARING,
        -> R.string.data_transfer_progress_exporting

        DataTransferStage.READING_FILE -> R.string.data_transfer_progress_reading
        DataTransferStage.OPENING_BACKUP -> R.string.data_transfer_progress_validating
        DataTransferStage.IMPORTING -> R.string.data_transfer_progress_importing
        DataTransferStage.IDLE -> null
    } ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.ExtraLarge),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = OverlayElevation,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.ExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(ProgressSize))
                Text(text = stringResource(label), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Hands the staged backup to whichever application the user picks.
 *
 * `ACTION_SEND` through a chooser, with a `content://` URI and only
 * [Intent.FLAG_GRANT_READ_URI_PERMISSION]. Griff does not send the mail: it has no SMTP client, no
 * provider API and no `INTERNET` permission, and it does not assume any particular mail app is
 * installed. The password is deliberately absent from the subject, the body and the extras - a
 * secret sent alongside the thing it protects is not a secret.
 *
 * @return false when nothing on the device can take the intent, which the caller turns into an
 * explanation and an alternative rather than a crash.
 */
private fun Context.startBackupShare(file: SharedBackupFile, recipient: String?): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = BackupFormat.MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, file.uri.toUri())
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.data_transfer_email_subject))
        putExtra(Intent.EXTRA_TEXT, getString(R.string.data_transfer_email_body))
        if (recipient != null) putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(
        send,
        getString(R.string.data_transfer_email_chooser_title),
    ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    // Checked before launching, not after: `createChooser` succeeds even with nothing behind it and
    // would show its own generic "no apps" sheet, which cannot offer the alternative that does work.
    // The lookup needs the <queries> declaration in this module's manifest to see anything at all.
    if (send.resolveActivity(packageManager) == null) return false

    return try {
        startActivity(chooser)
        true
    } catch (error: ActivityNotFoundException) {
        // Belt and braces: an app can disappear between the lookup and the launch, and a device with
        // sharing disabled by policy must produce an explanation rather than an exception.
        false
    }
}

/** The picker filters on nothing: a renamed backup still has to be selectable. */
private const val ANY_MIME_TYPE = "*/*"

private val ProgressSize = 36.dp
private val OverlayElevation = 6.dp

@ThemePreviews
@Composable
private fun DataTransferScreenPreview() {
    GriffThemePreview {
        DataTransferScreen(
            state = DataTransferUiState(isHistoryLoading = false),
            zone = ZoneId.of("Europe/Warsaw"),
            onOpenDrawer = {},
            onExportRequested = {},
            onShareRequested = {},
            onImportRequested = {},
            onExportPasswordConfirmed = { _, _ -> },
            onSharePasswordConfirmed = { _, _ -> },
            onImportPasswordConfirmed = {},
            onPasswordDialogDismissed = {},
            onImportPasswordDismissed = {},
            onPreviewDismissed = {},
            onMergeSelected = {},
            onReplaceSelected = {},
            onReplaceConfirmed = {},
            onReplaceCancelled = {},
            onOfflineShareConfirmed = {},
            onOfflineShareCancelled = {},
            onShareUnavailableDismissed = {},
            onMessageShown = {},
        )
    }
}
