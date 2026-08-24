package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * The list/section container of the "Graphite Precision" design system: a vertical surface
 * gradient with a hairline edge, in place of a flat [androidx.compose.material3.Card].
 *
 * The gradient is one tonal step, top to bottom - just enough that a card reads as a lit surface
 * rather than a solid color, without competing with the navy gradient reserved for hero content.
 * [GriffTheme.containerBorder] supplies the hairline, so the edge is defined identically to every
 * other container in the app.
 */
@Composable
fun GriffCard(
    modifier: Modifier = Modifier,
    shape: Shape = GriffShapes.Container,
    contentPadding: PaddingValues = PaddingValues(Spacing.Large),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradient = Brush.verticalGradient(
        listOf(colorScheme.surfaceContainerLowest, colorScheme.surfaceContainer),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Spacing.ExtraSmall / 2, shape = shape, clip = false)
            .clip(shape)
            .background(gradient)
            .border(GriffTheme.containerBorder, shape)
            .padding(contentPadding),
        content = content,
    )
}

@ThemePreviews
@Composable
private fun GriffCardPreview() {
    GriffThemePreview {
        GriffCard(modifier = Modifier.fillMaxWidth()) {
            Text("HBO Max", style = MaterialTheme.typography.titleMedium)
            Text(
                "27,49 zł / mies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
