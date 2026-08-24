package com.griff.keeper.presentation.reminders.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.reminder.ReminderDefaults
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.GriffCard
import com.griff.keeper.presentation.common.component.GriffHeroCard
import com.griff.keeper.presentation.reminders.ReminderPhrases
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.HairlineWidth
import com.griff.keeper.presentation.theme.Spacing

/** The master switch, and the one sentence that says what it does. */
@Composable
internal fun GlobalRemindersCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val switchDescription = stringResource(R.string.reminders_global_switch_description)

    GriffHeroCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = GriffGradients.OnAccent,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = stringResource(R.string.reminders_global_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = GriffGradients.OnAccent,
                )
                Text(
                    text = stringResource(R.string.reminders_global_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GriffGradients.OnAccent.copy(alpha = 0.85f),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GriffGradients.OnAccent,
                    checkedTrackColor = GriffGradients.OnAccent.copy(alpha = 0.35f),
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = GriffGradients.OnAccent.copy(alpha = 0.85f),
                    uncheckedTrackColor = GriffGradients.OnAccent.copy(alpha = 0.2f),
                    uncheckedBorderColor = Color.Transparent,
                ),
                modifier = Modifier.semantics { contentDescription = switchDescription },
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
        shape = GriffShapes.Container,
        colors = CardDefaults.cardColors(
            containerColor = GriffTheme.colors.warning.copy(alpha = WarningWashAlpha),
        ),
        border = BorderStroke(
            width = HairlineWidth,
            color = GriffTheme.colors.warning.copy(alpha = WarningBorderAlpha),
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
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (canRequestPermission) {
                    Button(
                        onClick = onRequestPermission,
                        shape = GriffShapes.Interactive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarningGradient, GriffShapes.Interactive),
                    ) {
                        Text(stringResource(R.string.reminders_permission_grant))
                    }
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = GriffShapes.Interactive,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GriffTheme.colors.warning,
                    ),
                    border = BorderStroke(
                        width = HairlineWidth,
                        color = GriffTheme.colors.warning.copy(alpha = WarningBorderAlpha * 1.5f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.reminders_permission_settings))
                }
            }
        }
    }
}

/** The warning-family gradient reserved for this one call-to-action, distinct from the brand navy. */
private val WarningGradient = Brush.linearGradient(listOf(Color(0xFF92400E), Color(0xFFD97706)))

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
    GriffCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(
                text = stringResource(R.string.reminders_off_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.reminders_off_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onEnable, shape = GriffShapes.Interactive) {
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
private const val WarningWashAlpha = 0.1f
private const val WarningBorderAlpha = 0.3f
