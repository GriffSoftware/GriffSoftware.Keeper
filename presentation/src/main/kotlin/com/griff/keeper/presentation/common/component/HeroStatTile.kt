package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * A small translucent stat chip laid over a [GriffHeroCard]'s gradient.
 *
 * Shared across every hero card that surfaces a secondary figure alongside its headline number
 * (subscriptions/obligations active counts, the drawer's monthly/yearly pair) - a `veil()`
 * background rather than a solid one, since it sits on top of a gradient that already carries the
 * screen's color.
 */
@Composable
fun HeroStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    Column(
        modifier = modifier
            .background(GriffGradients.veil(), GriffShapes.Interactive)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GriffGradients.OnAccent.copy(alpha = 0.82f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = GriffGradients.OnAccent,
        )
        // What kind of number this is - an estimate or something that already happened - matters
        // enough in this app that a stat tile is never allowed to drop it silently.
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = GriffGradients.OnAccent.copy(alpha = 0.72f),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun HeroStatTilePreview() {
    GriffThemePreview {
        GriffHeroCard {
            HeroStatTile(label = "Aktywne", value = "2")
        }
    }
}
