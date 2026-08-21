package com.griff.keeper.presentation.common.locale

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.MinTouchTarget
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * Picks the language of the interface.
 *
 * A dialog with two radio buttons rather than a screen: there are exactly two options, and making
 * the user navigate somewhere to choose between two things is a screen too many. There is no
 * "system default" entry - not choosing *is* following the system, which is what a fresh install
 * already does, and a third option that means "the same as one of the other two" is only a puzzle.
 *
 * The language names are self-names in both languages ("Polski", "English"), so someone who
 * switched by accident can still find their way back.
 */
@Composable
internal fun LanguagePickerDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    LanguageOption(
                        language = language,
                        isSelected = language == selected,
                        onSelect = { onSelect(language) },
                    )
                }
            }
        },
        // No confirm button: picking a language *is* the confirmation, and an extra "OK" would only
        // let the dialog show a language the app is not in yet.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun LanguageOption(
    language: AppLanguage,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            // On the row rather than on the radio button, so the whole line is the target and
            // TalkBack reads one selectable item instead of a button next to a label.
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
            text = stringResource(language.displayNameRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.Medium),
        )
    }
}

@ThemePreviews
@Composable
private fun LanguagePickerDialogPreview() {
    GriffThemePreview {
        LanguagePickerDialog(
            selected = AppLanguage.POLISH,
            onSelect = {},
            onDismiss = {},
        )
    }
}
