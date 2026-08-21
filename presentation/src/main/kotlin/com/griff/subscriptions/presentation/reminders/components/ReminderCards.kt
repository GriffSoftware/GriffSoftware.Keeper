package com.griff.subscriptions.presentation.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.domain.reminder.ReminderDefaults
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.reminders.ReminderPhrases
import com.griff.subscriptions.presentation.theme.GriffTheme
import com.griff.subscriptions.presentation.theme.Spacing

/** The master switch, and the one sentence that says what it does. */
@Composable
internal fun GlobalRemindersCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val switchDescription = stringResource(R.string.reminders_global_switch_description)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = stringResource(R.string.reminders_global_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.reminders_global_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier
                    .padding(start = Spacing.Medium)
                    .semantics { contentDescription = switchDescription },
            )
        }
    }
}

/**
 * Shown when Android is blocking what the app has been asked to do.
 *
 * The app must not claim reminders are working while the system silently drops them, so this card
 * states the situation plainly and offers the only two ways out: ask again, or go to the settings.
 */
@Composable
internal fun SystemNotificationsBlockedCard(
    canRequestPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = GriffTheme.colors.warning,
                    modifier = Modifier.size(BannerIconSize),
                )
                Text(
                    text = stringResource(R.string.reminders_permission_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = GriffTheme.colors.warning,
                )
            }
            Text(
                text = stringResource(R.string.reminders_permission_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (canRequestPermission) {
                    OutlinedButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.reminders_permission_grant))
                    }
                }
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.reminders_permission_settings))
                }
            }
        }
    }
}

/**
 * Shown instead of the switch's silence when reminders are off.
 *
 * The list below stays visible on purpose - the user can see exactly what turning them back on would
 * bring, which is more useful than an empty screen.
 */
@Composable
internal fun RemindersDisabledCard(
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(
                text = stringResource(R.string.reminders_off_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.reminders_off_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onEnable) {
                Text(stringResource(R.string.reminders_off_action))
            }
        }
    }
}

/**
 * The rules currently in force, one line per kind of record.
 *
 * Read-only for now, but read from [ReminderDefaults] rather than written into the layout, so making
 * them editable later is a change of source and not of screen.
 */
@Composable
internal fun ReminderDefaultsSection(
    defaults: ReminderDefaults,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.reminders_defaults_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.reminders_defaults_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DefaultRow(
            label = stringResource(R.string.reminders_defaults_insurance),
            value = ReminderPhrases.scheduleSummary(defaults.insurance),
        )
        DefaultRow(
            label = stringResource(R.string.reminders_defaults_payments),
            value = ReminderPhrases.scheduleSummary(defaults.payment),
        )
        DefaultRow(
            label = stringResource(R.string.reminders_defaults_subscriptions),
            value = ReminderPhrases.scheduleSummary(defaults.subscription),
        )
    }
}

@Composable
private fun DefaultRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private val BannerIconSize = 18.dp
