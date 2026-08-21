package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffTheme

/**
 * Renders the logo of a provider.
 *
 * A handful of well known services ([ProviderLogoAssets]) get a recognizable glyph: a simplified,
 * single-color brand mark (sourced from the community-maintained, CC0-licensed Simple Icons
 * project) tinted on a tonal circle. Bundling the *official* multi-color logotypes would require a
 * license this project does not have, so every other service - including every custom "Other"
 * entry - falls back to a neutral monogram: a tonal circle whose color is derived deterministically
 * from [logoKey], plus one or two initials of the service name. Adding a glyph for another provider
 * is a one line change in [ProviderLogoAssets]; the domain never knows [logoKey] is anything more
 * than an opaque string.
 */
@Composable
fun ProviderLogo(
    logoKey: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = ProviderLogoDefaults.Size,
) {
    val asset = remember(logoKey) { ProviderLogoAssets.of(logoKey) }

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (asset != null) {
            BrandLogo(asset = asset, size = size)
        } else {
            MonogramLogo(logoKey = logoKey, name = name, size = size)
        }
    }
}

@Composable
private fun BrandLogo(asset: ProviderLogoAsset, size: Dp) {
    val tint = asset.tintColor()

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = tint.copy(alpha = BrandBackgroundAlpha),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(asset.drawableRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier
                    .size(size)
                    .padding(size * BrandGlyphPaddingFraction),
            )
        }
    }
}

@Composable
private fun MonogramLogo(
    logoKey: String,
    name: String,
    size: Dp,
) {
    val palette = GriffTheme.colors.monogramPalette
    val color = remember(logoKey, palette) { palette[monogramColorIndex(logoKey, palette.size)] }
    val initials = remember(name) { initialsOf(name) }

    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = color.copy(alpha = MonogramBackgroundAlpha),
        contentColor = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontSize = monogramFontSize(size),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

object ProviderLogoDefaults {
    val Size: Dp = 44.dp
    val LargeSize: Dp = 88.dp
}

/**
 * A bundled brand glyph.
 *
 * [brandColor] is `null` for marks whose official color is effectively black (HBO Max, Tidal,
 * Apple TV+, GitHub Copilot, JetBrains, EA) - tinting those with pure black would turn invisible
 * against a dark theme background, so they render with the current [ColorScheme]'s neutral
 * `onSurfaceVariant` instead and stay readable in both themes.
 */
internal data class ProviderLogoAsset(
    val drawableRes: Int,
    val brandColor: Color?,
)

@Composable
private fun ProviderLogoAsset.tintColor(): Color =
    brandColor ?: MaterialTheme.colorScheme.onSurfaceVariant

/**
 * Maps an abstract logo key to a bundled brand glyph.
 *
 * Only providers with an unambiguous, freely licensed Simple Icons glyph are listed here; every
 * other [com.griff.keeper.domain.model.Provider] - including every catalog entry without a
 * match and the "Other" fallback - is rendered as a monogram instead. Adding a provider here is a
 * one line addition once a suitable glyph exists; nothing else in the app needs to change.
 */
internal object ProviderLogoAssets {

    private val assets: Map<String, ProviderLogoAsset> = mapOf(
        "netflix" to brand(R.drawable.logo_netflix, 0xE50914),
        "hbo_max" to neutral(R.drawable.logo_hbo_max),
        "spotify" to brand(R.drawable.logo_spotify, 0x1ED760),
        "youtube_music" to brand(R.drawable.logo_youtube_music, 0xFF0000),
        "apple_music" to brand(R.drawable.logo_apple_music, 0xFA243C),
        "tidal" to neutral(R.drawable.logo_tidal),
        "apple_tv_plus" to neutral(R.drawable.logo_apple_tv_plus),
        "claude" to brand(R.drawable.logo_claude, 0xD97757),
        "google_gemini" to brand(R.drawable.logo_google_gemini, 0x8E75B2),
        "perplexity_pro" to brand(R.drawable.logo_perplexity_pro, 0x1FB8CD),
        "github_copilot" to neutral(R.drawable.logo_github_copilot),
        "google_workspace" to brand(R.drawable.logo_google_workspace, 0x4285F4),
        "icloud_plus" to brand(R.drawable.logo_icloud_plus, 0x3693F3),
        "dropbox" to brand(R.drawable.logo_dropbox, 0x0061FF),
        "jetbrains" to neutral(R.drawable.logo_jetbrains),
        "ovhcloud" to brand(R.drawable.logo_ovhcloud, 0x123F6D),
        "allegro_smart" to brand(R.drawable.logo_allegro_smart, 0xFF5A00),
        "glovo_prime" to brand(R.drawable.logo_glovo_prime, 0xF2CC38),
        "playstation_plus" to brand(R.drawable.logo_playstation_plus, 0x0070D1),
        "ea_play" to neutral(R.drawable.logo_ea_play),
    )

    fun of(logoKey: String): ProviderLogoAsset? = assets[logoKey]

    private fun brand(drawableRes: Int, colorHex: Long) =
        ProviderLogoAsset(drawableRes, Color(0xFF000000 or colorHex))

    private fun neutral(drawableRes: Int) = ProviderLogoAsset(drawableRes, brandColor = null)
}

private const val MonogramBackgroundAlpha = 0.18f
private const val BrandBackgroundAlpha = 0.16f
private const val BrandGlyphPaddingFraction = 0.28f

/**
 * Stable index into the monogram palette, so a service keeps its color between launches and between
 * themes; the palette itself is theme dependent because the color is used as text.
 */
private fun monogramColorIndex(logoKey: String, paletteSize: Int): Int =
    ((logoKey.hashCode().toLong() and 0xFFFFFFFFL) % paletteSize).toInt()

private fun monogramFontSize(size: Dp): TextUnit = (size.value * 0.36f).sp

/** One or two initials, e.g. `Google Workspace` becomes `GW` and `Spotify` becomes `S`. */
internal fun initialsOf(name: String): String {
    val words = name
        .split(' ', '-', '_', '.', '/')
        .filter { it.isNotBlank() }
        .map { it.trim() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
