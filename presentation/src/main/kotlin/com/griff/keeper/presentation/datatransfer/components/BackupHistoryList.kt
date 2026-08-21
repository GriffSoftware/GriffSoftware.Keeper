package com.griff.keeper.presentation.datatransfer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.datatransfer.BackupErrorLabels
import com.griff.keeper.presentation.datatransfer.BackupHistoryItemUi
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.Instant
import java.time.ZoneId

/**
 * One entry of the device's import/export log.
 *
 * The status is carried three ways at once - an icon, a colour and a sentence - so it survives being
 * read by someone who cannot distinguish the colours, and so it still makes sense in a screenshot
 * printed in black and white. Colour alone is never the message.
 *
 * The counts are spelled out rather than totalled: "12 subscriptions, 6 fees" is what the user
 * recognizes as their data, while "18 entities" is the database's way of putting it.
 */
@Composable
internal fun BackupHistoryRow(
    item: BackupHistoryItemUi,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Icon(
            imageVector = when (item.type) {
                BackupOperationType.EXPORT -> Icons.Default.FileUpload
                BackupOperationType.IMPORT -> Icons.Default.FileDownload
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TypeIconSize),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(text = title(item), style = MaterialTheme.typography.titleSmall)

            Text(
                text = stringResource(
                    R.string.data_transfer_history_timestamp,
                    DateFormatter.formatFullDate(item.finishedAt, zone),
                    DateFormatter.formatTime(item.finishedAt, zone),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (item.isSuccess) {
                Text(
                    text = counts(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StatusLine(item)
        }
    }
}

@Composable
private fun StatusLine(item: BackupHistoryItemUi) {
    val isSuccess = item.status == BackupOperationStatus.SUCCESS
    val color: Color =
        if (isSuccess) GriffTheme.colors.success else MaterialTheme.colorScheme.error
    val text = if (isSuccess) {
        stringResource(R.string.data_transfer_history_success)
    } else {
        stringResource(BackupErrorLabels.historyRes(item.errorType ?: fallbackError()))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = stringResource(
                if (isSuccess) R.string.severity_success else R.string.severity_error,
            ),
            tint = color,
            modifier = Modifier.size(StatusIconSize),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun title(item: BackupHistoryItemUi): String {
    val type = stringResource(
        when (item.type) {
            BackupOperationType.EXPORT -> R.string.data_transfer_history_export
            BackupOperationType.IMPORT -> R.string.data_transfer_history_import
        },
    )
    val mode = item.importMode?.let {
        stringResource(
            when (it) {
                ImportMode.MERGE -> R.string.data_transfer_history_mode_merge
                ImportMode.REPLACE -> R.string.data_transfer_history_mode_replace
            },
        )
    }
    return if (mode == null) type else "$type • $mode"
}

/**
 * "12 subscriptions • 6 fees", or a plain record count when neither kind was involved.
 *
 * The obligation label is the short one here: a list row has to stay one line, and the full name of
 * the section does not fit next to a subscription count.
 */
@Composable
private fun counts(item: BackupHistoryItemUi): String {
    if (item.subscriptionCount == 0 && item.obligationCount == 0) {
        return stringResource(R.string.data_transfer_history_no_records)
    }

    val parts = buildList {
        if (item.subscriptionCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.data_transfer_count_subscriptions,
                    item.subscriptionCount,
                    item.subscriptionCount,
                ),
            )
        }
        if (item.obligationCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.data_transfer_count_obligations_short,
                    item.obligationCount,
                    item.obligationCount,
                ),
            )
        }
    }
    return parts.joinToString(separator = " • ")
}

/** A failed entry with no category is still a failure; it degrades rather than disappearing. */
private fun fallbackError() = com.griff.keeper.domain.backup.BackupErrorType.UNKNOWN

private val TypeIconSize = 20.dp
private val StatusIconSize = 14.dp

@ThemePreviews
@Composable
private fun BackupHistoryRowPreview() {
    GriffThemePreview {
        Column {
            BackupHistoryRow(
                item = BackupHistoryItemUi(
                    id = "1",
                    type = BackupOperationType.EXPORT,
                    status = BackupOperationStatus.SUCCESS,
                    importMode = null,
                    finishedAt = Instant.parse("2026-08-21T00:43:00Z"),
                    subscriptionCount = 12,
                    obligationCount = 6,
                    errorType = null,
                ),
                zone = ZoneId.of("Europe/Warsaw"),
            )
            BackupHistoryRow(
                item = BackupHistoryItemUi(
                    id = "2",
                    type = BackupOperationType.IMPORT,
                    status = BackupOperationStatus.SUCCESS,
                    importMode = ImportMode.MERGE,
                    finishedAt = Instant.parse("2026-08-18T19:21:00Z"),
                    subscriptionCount = 15,
                    obligationCount = 0,
                    errorType = null,
                ),
                zone = ZoneId.of("Europe/Warsaw"),
            )
            BackupHistoryRow(
                item = BackupHistoryItemUi(
                    id = "3",
                    type = BackupOperationType.IMPORT,
                    status = BackupOperationStatus.FAILED,
                    importMode = null,
                    finishedAt = Instant.parse("2026-08-19T14:20:00Z"),
                    subscriptionCount = 0,
                    obligationCount = 0,
                    errorType = com.griff.keeper.domain.backup.BackupErrorType.INVALID_FILE,
                ),
                zone = ZoneId.of("Europe/Warsaw"),
            )
        }
    }
}
