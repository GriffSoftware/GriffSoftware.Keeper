package com.griff.keeper.presentation.reminders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.griff.keeper.application.reminder.ReminderItemStatus
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.ObligationIcon
import com.griff.keeper.presentation.common.component.ObligationIconDefaults
import com.griff.keeper.presentation.common.component.ProviderLogo
import com.griff.keeper.presentation.common.component.ProviderLogoDefaults
import com.griff.keeper.presentation.common.component.TagChip
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.reminders.ReminderPhrases
import com.griff.keeper.presentation.reminders.ReminderRowUi
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.Spacing

/**
 * One record on the reminders screen: what it is, when its date falls, and when it will speak up.
 *
 * Deliberately taller than a list row. The screen exists to answer "what happens next?", and that
 * answer needs three separate facts, none of which is an amount - so the compact
 * [com.griff.keeper.presentation.common.component.EntryRow] shape used by the money screens
 * would be the wrong one here.
 */
@Composable
internal fun ReminderRow(
    row: ReminderRowUi,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetLabel = row.kind?.let { stringResource(ReminderPhrases.targetLabel(it)) }
    val targetDate = row.targetDate?.let { DateFormatter.formatFullDate(it) }
    val relativeTarget = row.daysUntilTarget?.let { ReminderPhrases.relativeDays(it) }
    val statusText = row.statusText()
    val contentDescription = listOfNotNull(
        row.title,
        targetLabel?.let { label -> targetDate?.let { "$label $it" } },
        relativeTarget,
        statusText,
    ).joinToString(separator = ", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Large, vertical = Spacing.Medium)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.Top,
    ) {
        when {
            row.obligationCategory != null -> ObligationIcon(
                category = row.obligationCategory,
                size = ObligationIconDefaults.Size,
            )

            else -> ProviderLogo(
                logoKey = row.logoKey.orEmpty(),
                name = row.title,
                size = ProviderLogoDefaults.Size,
            )
        }

        Spacer(Modifier.width(Spacing.Medium))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(Spacing.Small))
                TagChip(style = row.tag)
            }

            if (targetLabel != null && targetDate != null) {
                Text(
                    text = "$targetLabel • $targetDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (relativeTarget != null) {
                Text(
                    text = relativeTarget,
                    style = MaterialTheme.typography.labelLarge,
                    color = row.targetTint(),
                )
            }

            NextReminderLine(row = row, enabled = enabled, statusText = statusText)
        }
    }
}

/**
 * The line the screen exists for: when the user will actually be told.
 *
 * The bell is always accompanied by words - it is an accent, never the only carrier of the state -
 * and the same line explains the silence when there is nothing to announce.
 */
@Composable
private fun NextReminderLine(row: ReminderRowUi, enabled: Boolean, statusText: String?) {
    val isScheduled = row.status == ReminderItemStatus.SCHEDULED && row.nextReminderDate != null
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isScheduled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .padding(top = Spacing.ExtraSmall)
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Icon(
            imageVector = if (isScheduled && enabled) {
                Icons.Default.NotificationsActive
            } else {
                Icons.Default.NotificationsOff
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(BellSize),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
            if (isScheduled) {
                val date = requireNotNull(row.nextReminderDate)
                Text(
                    text = stringResource(R.string.reminders_next_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.reminders_next_value,
                        DateFormatter.formatFullDate(date),
                        ReminderPhrases.relativeDaysInline(row.daysUntilReminder ?: 0L),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                )
            } else if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                )
            }
        }
    }
}

/**
 * A deadline that is upon the user is worth a warning color; anything further out is not.
 *
 * The color only ever repeats what the text already says, so nothing is lost when it cannot be
 * perceived.
 */
@Composable
private fun ReminderRowUi.targetTint(): Color = when {
    daysUntilTarget == null -> MaterialTheme.colorScheme.onSurfaceVariant
    daysUntilTarget < 0L -> MaterialTheme.colorScheme.error
    daysUntilTarget <= URGENT_DAYS -> GriffTheme.colors.warning
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ReminderRowUi.statusText(): String? = when (status) {
    ReminderItemStatus.SCHEDULED -> null
    ReminderItemStatus.DISABLED -> stringResource(R.string.reminders_status_disabled)
    ReminderItemStatus.NO_DATE -> stringResource(R.string.reminders_status_no_date)
    ReminderItemStatus.PASSED -> stringResource(R.string.reminders_status_passed)
}

/** Today or tomorrow: the only two cases where the user may still have to act at once. */
private const val URGENT_DAYS = 1L

private val BellSize = 16.dp
