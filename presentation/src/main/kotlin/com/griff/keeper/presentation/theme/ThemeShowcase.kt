package com.griff.keeper.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Palette reference, shown only in previews.
 *
 * Exists so the two themes can be compared as palettes and not only as screens - a wrong accent or
 * a surface ramp that stopped being monotonic is obvious here and easy to miss on a busy screen.
 */
@ThemePreviews
@Composable
private fun GriffPalettePreview() {
    GriffThemePreview {
        val scheme = MaterialTheme.colorScheme

        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            SwatchRow(
                title = "Accent",
                swatches = listOf(
                    Swatch("primary", scheme.primary, scheme.onPrimary),
                    Swatch("container", scheme.primaryContainer, scheme.onPrimaryContainer),
                    Swatch("tertiary", scheme.tertiary, scheme.onTertiary),
                ),
            )
            SwatchRow(
                title = "Surfaces",
                swatches = listOf(
                    Swatch("background", scheme.background, scheme.onBackground),
                    Swatch("surface", scheme.surface, scheme.onSurface),
                    Swatch("container", scheme.surfaceContainer, scheme.onSurface),
                    Swatch("high", scheme.surfaceContainerHigh, scheme.onSurface),
                    Swatch("variant", scheme.surfaceVariant, scheme.onSurfaceVariant),
                ),
            )
            SwatchRow(
                title = "Status",
                swatches = listOf(
                    Swatch("success", GriffTheme.colors.success, scheme.surface),
                    Swatch("warning", GriffTheme.colors.warning, scheme.surface),
                    Swatch("info", GriffTheme.colors.info, scheme.surface),
                    Swatch("error", scheme.error, scheme.onError),
                ),
            )
            SwatchRow(
                title = "Chart",
                swatches = ChartPalette.map { Swatch(label = null, color = it, onColor = it) },
            )
        }
    }
}

private data class Swatch(val label: String?, val color: Color, val onColor: Color)

@Composable
private fun SwatchRow(title: String, swatches: List<Swatch>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            swatches.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(SwatchHeight)
                        .background(swatch.color, RoundedCornerShape(Spacing.Small)),
                ) {
                    if (swatch.label != null) {
                        Text(
                            text = swatch.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = swatch.onColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.ExtraSmall),
                        )
                    }
                }
            }
        }
    }
}

private val SwatchHeight = 48.dp
