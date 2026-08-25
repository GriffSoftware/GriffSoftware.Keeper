package com.griff.keeper.presentation.common.currency

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
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.MinTouchTarget
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * Picks the app's global currency.
 *
 * Mirrors [com.griff.keeper.presentation.common.locale.LanguagePickerDialog] deliberately: two radio
 * buttons in a dialog rather than a screen. Unlike the language picker, choosing an option here is
 * *not* itself the confirmation - switching currency can mean converting real amounts, so picking a
 * value only closes this dialog and hands the choice to the caller, which decides whether anything
 * else needs asking.
 */
@Composable
internal fun CurrencyPickerDialog(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_dialog_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                Currency.entries.forEach { currency ->
                    CurrencyOption(
                        currency = currency,
                        isSelected = currency == selected,
                        onSelect = { onSelect(currency) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun CurrencyOption(
    currency: Currency,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
            text = "${currency.code} — ${stringResource(currency.displayNameRes)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.Medium),
        )
    }
}

@ThemePreviews
@Composable
private fun CurrencyPickerDialogPreview() {
    GriffThemePreview {
        CurrencyPickerDialog(
            selected = Currency.PLN,
            onSelect = {},
            onDismiss = {},
        )
    }
}
