package com.griff.keeper.presentation.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.griff.keeper.application.reminder.ItemReminderState
import com.griff.keeper.application.reminder.ReminderItemStatus
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.DetailsInfoRow
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.reminders.ReminderPhrases
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.Spacing

/**
 * The reminder block shown on a details screen.
 *
 * Shared by subscriptions and obligations because the question is the same one - "will I be told
 * about this, and when?" - while the words for *not* being told differ, so the two sentences that
 * are genuinely record specific are passed in rather than branched on here.
 *
 * The switch governs this record only; the app-wide switch and the Android permission are reported
 * underneath rather than folded into it, so the user can see which of the three is in their way.
 */
@Composable
internal fun ItemReminderSection(
    state: ItemReminderState,
    systemNotificationsEnabled: Boolean,
    disabledText: String,
    noDateText: String,
    noDateHint: String,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
) {
    val switchDescription = stringResource(R.string.reminder_section_switch_description)

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reminder_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.itemEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = isEditable,
                    modifier = Modifier.semantics { contentDescription = switchDescription },
                )
            }

            if (!state.itemEnabled) {
                Text(
                    text = disabledText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            when (state.status) {
                ReminderItemStatus.SCHEDULED -> ScheduledReminder(state)

                ReminderItemStatus.NO_DATE -> {
                    Text(
                        text = noDateText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = noDateHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ReminderItemStatus.PASSED -> {
                    TargetRow(state)
                    Text(
                        text = stringResource(R.string.reminder_section_passed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Unreachable while the record's own switch is on, but a `when` that pretends
                // otherwise would break silently if the mapping ever changed.
                ReminderItemStatus.DISABLED -> Text(
                    text = disabledText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Two blockers the record itself cannot fix, and which the user would otherwise only
            // discover by never hearing anything.
            if (!state.globalEnabled) {
                BlockerNote(stringResource(R.string.reminder_section_global_off))
            } else if (!systemNotificationsEnabled) {
                BlockerNote(stringResource(R.string.reminder_section_system_off))
            }
        }
    }
}

@Composable
private fun ScheduledReminder(state: ItemReminderState) {
    TargetRow(state)

    val next = state.nextReminder ?: return
    HorizontalDivider()
    DetailsInfoRow(
        label = stringResource(R.string.reminder_section_next_label),
        value = DateFormatter.formatFullDate(next.fireDate),
    )
    Text(
        text = ReminderPhrases.offsetExplanation(next.kind, next.daysBefore),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TargetRow(state: ItemReminderState) {
    val kind = state.kind ?: return
    val target = state.targetDate ?: return
    DetailsInfoRow(
        label = stringResource(ReminderPhrases.targetLabel(kind)),
        value = DateFormatter.formatFullDate(target),
    )
}

@Composable
private fun BlockerNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = GriffTheme.colors.warning,
    )
}
