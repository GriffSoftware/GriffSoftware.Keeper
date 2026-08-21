package com.griff.keeper.presentation.datatransfer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.datatransfer.ImportPreviewUi
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.Instant
import java.time.ZoneId

/**
 * What the file turned out to contain, and the decision that follows from it.
 *
 * Shown only once the backup has been fully opened - decrypted, authenticated and validated - so
 * every number here is a fact rather than a promise. Nothing has been written at this point, which is
 * what makes the choice below it real.
 *
 * The choice itself depends on the device. With nothing to lose there is one obvious action,
 * "restore"; with existing records there are two, and they are not interchangeable, so they are
 * offered as two labelled decisions instead of one button with a hidden mode.
 */
@Composable
internal fun ImportPreviewDialog(
    preview: ImportPreviewUi,
    zone: ZoneId,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
        title = { Text(stringResource(R.string.data_transfer_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                PreviewRow(
                    label = stringResource(R.string.data_transfer_preview_created_label),
                    value = stringResource(
                        R.string.data_transfer_history_timestamp,
                        DateFormatter.formatFullDate(preview.createdAt, zone),
                        DateFormatter.formatTime(preview.createdAt, zone),
                    ),
                )
                PreviewRow(
                    label = stringResource(R.string.data_transfer_preview_app_version_label),
                    value = preview.appVersion,
                )

                Text(
                    text = stringResource(R.string.data_transfer_preview_contents_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.data_transfer_count_subscriptions,
                            preview.subscriptionCount,
                            preview.subscriptionCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.data_transfer_count_obligations,
                            preview.obligationCount,
                            preview.obligationCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (preview.hasSettings) {
                        Text(
                            text = stringResource(R.string.data_transfer_preview_settings),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Reported, never acted on: two records with the same name can be two deliberate
                // rows, and only a stable identifier is evidence that they are the same thing.
                if (preview.possibleDuplicates > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = GriffTheme.colors.warning,
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.data_transfer_preview_duplicates,
                                preview.possibleDuplicates,
                                preview.possibleDuplicates,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = if (preview.hasLocalData) {
                        stringResource(R.string.data_transfer_preview_local_data)
                    } else {
                        pluralStringResource(
                            R.plurals.data_transfer_preview_restore_question,
                            preview.recordCount,
                            preview.recordCount,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onMerge) {
                    Text(
                        stringResource(
                            if (preview.hasLocalData) {
                                R.string.data_transfer_preview_merge
                            } else {
                                R.string.data_transfer_preview_restore
                            },
                        ),
                    )
                }
                if (preview.hasLocalData) {
                    TextButton(
                        onClick = onReplace,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(R.string.data_transfer_preview_replace))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * The second gate in front of a destructive import.
 *
 * Replacing data cannot be undone, so choosing it in the preview is treated as intent rather than as
 * a decision. This dialog spells out what disappears, and its confirming button carries the error
 * colour - the one place on this screen where a button is coloured to warn rather than to invite.
 */
@Composable
internal fun ReplaceConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.data_transfer_replace_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(stringResource(R.string.data_transfer_replace_message))
                Text(
                    text = stringResource(R.string.data_transfer_replace_backup_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.data_transfer_replace_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Says what the app knows about connectivity, and then gets out of the way.
 *
 * The backup already exists at this point, and the message is sent by a different application which
 * may well queue it until the device is back online. Blocking here would be the app claiming
 * authority over something it does not control, so this warns and lets the user carry on.
 */
@Composable
internal fun OfflineShareWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = GriffTheme.colors.warning,
            )
        },
        title = { Text(stringResource(R.string.data_transfer_offline_title)) },
        text = { Text(stringResource(R.string.data_transfer_offline_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.data_transfer_offline_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@ThemePreviews
@Composable
private fun ImportPreviewDialogPreview() {
    GriffThemePreview {
        ImportPreviewDialog(
            preview = ImportPreviewUi(
                fileName = "griff-backup-2026-08-18-2143.griffbackup",
                createdAt = Instant.parse("2026-08-18T19:43:00Z"),
                appVersion = "1.3.0",
                subscriptionCount = 12,
                obligationCount = 6,
                hasSettings = true,
                hasLocalData = true,
                possibleDuplicates = 2,
            ),
            zone = ZoneId.of("Europe/Warsaw"),
            onMerge = {},
            onReplace = {},
            onDismiss = {},
        )
    }
}
