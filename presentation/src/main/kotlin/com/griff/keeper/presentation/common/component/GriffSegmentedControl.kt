package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/** One option of a [GriffSegmentedControl], labeled by whatever string the caller resolves. */
data class SegmentOption<T>(
    val value: T,
    val label: String,
)

/**
 * The redesign's replacement for an ad hoc row of [androidx.compose.material3.FilterChip]s or
 * [androidx.compose.material3.SegmentedButton]s: a track on `surfaceContainer` with the active
 * segment carrying the navy gradient, used for the statistics scope/period switches and the
 * obligations month/year toggle.
 *
 * Single-select and always has an active segment - unlike [TagFilterRow], "none selected" is not a
 * meaningful state for a scope or period switch.
 */
@Composable
fun <T> GriffSegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GriffShapes.Container,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.ExtraSmall / 2)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = SegmentHeight)
                    .clip(GriffShapes.Interactive)
                    .then(
                        if (isSelected) {
                            Modifier.background(GriffGradients.accent())
                        } else {
                            Modifier
                        },
                    )
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        onClick = { onSelect(option.value) },
                    )
                    .padding(horizontal = Spacing.Small),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        GriffGradients.OnAccent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

private val SegmentHeight = 34.dp

@ThemePreviews
@Composable
private fun GriffSegmentedControlPreview() {
    GriffThemePreview {
        GriffSegmentedControl(
            options = listOf(
                SegmentOption(0, "Wszystkie"),
                SegmentOption(1, "Subskrypcje"),
                SegmentOption(2, "Opłaty"),
            ),
            selected = 1,
            onSelect = {},
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
