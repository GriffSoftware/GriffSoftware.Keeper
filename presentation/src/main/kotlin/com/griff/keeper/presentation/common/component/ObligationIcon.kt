package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.Tags
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * Leading icon of an obligation: the category glyph on a tinted circle.
 *
 * The circle takes the same accent as the record's tag, so the icon and the badge on the same row
 * agree with each other instead of introducing a second color code. It is drawn at the tag's
 * container/content pair rather than at a saturated brand color, which keeps a list of six records
 * calm.
 *
 * The mapping lives here, in the presentation layer: the domain knows categories, not drawables.
 */
@Composable
fun ObligationIcon(
    category: ObligationCategory,
    modifier: Modifier = Modifier,
    size: Dp = ObligationIconDefaults.Size,
) {
    val colors = GriffTheme.colors.tagColors.getValue(Tags.of(category).accent)

    Surface(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ObligationGlyph(
                category = category,
                modifier = Modifier.size(size * GlyphFraction),
            )
        }
    }
}

/**
 * The bare category glyph, without the circle behind it.
 *
 * Used inside chips and buttons, where the surrounding component already provides the container.
 */
@Composable
fun ObligationGlyph(
    category: ObligationCategory,
    modifier: Modifier = Modifier,
) {
    when (category) {
        // Material Icons has no drone; every near miss reads as a different object.
        ObligationCategory.DRONE_INSURANCE -> Icon(
            painter = painterResource(R.drawable.ic_drone),
            contentDescription = null,
            modifier = modifier,
        )

        else -> Icon(
            imageVector = when (category) {
                ObligationCategory.VEHICLE_INSURANCE -> Icons.Default.DirectionsCar
                ObligationCategory.HOME_INSURANCE -> Icons.Default.Home
                ObligationCategory.LAND_INSURANCE -> Icons.Default.Landscape
                ObligationCategory.PROPERTY_TAX -> Icons.Default.Apartment
                ObligationCategory.LAND_TAX -> Icons.Default.Terrain
                ObligationCategory.DRONE_INSURANCE,
                ObligationCategory.OTHER,
                -> Icons.AutoMirrored.Filled.ReceiptLong
            },
            contentDescription = null,
            modifier = modifier,
        )
    }
}

object ObligationIconDefaults {
    val Size: Dp = 44.dp
    val SmallSize: Dp = 36.dp
    val LargeSize: Dp = 88.dp
}

/** Same optical weight as the provider glyphs the icon sits next to on other screens. */
private const val GlyphFraction = 0.52f

@ThemePreviews
@Composable
private fun ObligationIconPreview() {
    GriffThemePreview {
        Row(
            modifier = Modifier.padding(Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            ObligationCategory.entries.forEach { ObligationIcon(category = it) }
        }
    }
}
