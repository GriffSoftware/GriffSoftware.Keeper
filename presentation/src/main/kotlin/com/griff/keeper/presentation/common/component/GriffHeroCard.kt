package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * The navy gradient surface: the one recurring "hero" moment of the redesign, used for the totals
 * card atop the subscriptions/obligations/statistics lists, the reminders master-toggle card, and
 * the subscription details / About headers.
 *
 * Content is a [Box] scope rather than a fixed layout, since a summary card stacks vertically while
 * the reminders master card lays out horizontally - the caller owns its own arrangement, this just
 * supplies the gradient, the sheen highlight and the shape.
 */
@Composable
fun GriffHeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = GriffShapes.Hero,
    contentPadding: PaddingValues = PaddingValues(Spacing.Large),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = Spacing.Small, shape = shape, clip = false)
            .clip(shape)
            .background(GriffGradients.accent()),
    ) {
        Box(modifier = Modifier.matchParentSize().background(GriffGradients.sheen()))
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@ThemePreviews
@Composable
private fun GriffHeroCardPreview() {
    GriffThemePreview {
        GriffHeroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "65,48 zł",
                style = MaterialTheme.typography.headlineLarge,
                color = GriffGradients.OnAccent,
            )
        }
    }
}
