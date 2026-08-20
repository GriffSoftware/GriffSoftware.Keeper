package com.griff.subscriptions.presentation.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.subscriptions.presentation.theme.Spacing

/**
 * One option of a [CategorySelector].
 *
 * The label is the full category name, not the short badge text: a form has room for it, and two
 * categories can share one badge - both taxes read "Podatek" - which would make them
 * indistinguishable here.
 */
data class CategoryOption<T>(
    val value: T,
    @param:StringRes val labelRes: Int,
)

/**
 * Wrapping grid of single-choice category chips.
 *
 * A segmented button row cannot hold seven labelled options and a dropdown would hide the icons that
 * make the categories recognizable, so the options are laid out as chips that wrap onto as many lines
 * as they need. One component serves both forms; the caller decides whether an option gets a glyph
 * through [leadingIcon].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> CategorySelector(
    label: String,
    options: List<CategoryOption<T>>,
    selected: T?,
    enabled: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    leadingIcon: (@Composable (T) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.Small),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.value == selected,
                    onClick = { onSelect(option.value) },
                    enabled = enabled,
                    label = { Text(stringResource(option.labelRes)) },
                    leadingIcon = leadingIcon?.let { icon -> { icon(option.value) } },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }
    }
}
