package com.griff.subscriptions.presentation.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.theme.Spacing

/**
 * The reminder switch shown in the add and edit forms.
 *
 * One line and one sentence: the form is about the record, not about reminder configuration, so it
 * states which schedule will apply rather than asking the user to build one. The schedule itself is
 * a default of the category and is edited on the reminders screen.
 */
@Composable
fun RemindersToggleField(
    enabled: Boolean,
    hint: String,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
) {
    val switchDescription = stringResource(R.string.reminder_section_switch_description)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = stringResource(R.string.form_reminders_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = isEditable,
            modifier = Modifier.semantics { contentDescription = switchDescription },
        )
    }
}
