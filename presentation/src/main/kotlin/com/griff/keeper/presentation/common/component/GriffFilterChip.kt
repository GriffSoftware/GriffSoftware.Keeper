package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * The redesign's chip: an active filter carries the navy gradient with white text instead of
 * Material's tonal `primaryContainer`, an inactive one is just an outline. Signature mirrors
 * [androidx.compose.material3.FilterChip] closely enough to drop straight into [TagFilterRow].
 */
@Composable
fun GriffFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    val shape = GriffShapes.Interactive
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ChipHeight)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(GriffGradients.accent())
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                },
            )
            .alpha(if (enabled) 1f else DisabledAlpha)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (selected) GriffGradients.OnAccent else MaterialTheme.colorScheme.onSurfaceVariant
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                if (leadingIcon == null) {
                    label()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                    ) {
                        leadingIcon()
                        label()
                    }
                }
            }
        }
    }
}

private const val DisabledAlpha = 0.5f

private val ChipHeight = 34.dp

@ThemePreviews
@Composable
private fun GriffFilterChipPreview() {
    GriffThemePreview {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = Modifier.padding(Spacing.Large),
        ) {
            GriffFilterChip(selected = true, onClick = {}, label = { Text("Wszystkie") })
            GriffFilterChip(selected = false, onClick = {}, label = { Text("Video") })
        }
    }
}
