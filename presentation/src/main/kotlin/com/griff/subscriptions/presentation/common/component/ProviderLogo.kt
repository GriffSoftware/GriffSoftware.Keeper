package com.griff.subscriptions.presentation.common.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.griff.subscriptions.presentation.theme.ChartPalette

/**
 * Renders the logo of a provider.
 *
 * Brand logos are trademarks and cannot be bundled without a license, so the app ships with a
 * neutral monogram by default: a tonal circle whose color is derived deterministically from
 * [logoKey], plus one or two initials of the service name. Licensed assets can be added to
 * [ProviderLogoAssets] without touching any other layer - the domain only knows the abstract
 * [logoKey].
 */
@Composable
fun ProviderLogo(
    logoKey: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = ProviderLogoDefaults.Size,
) {
    val drawable = remember(logoKey) { ProviderLogoAssets.drawableFor(logoKey) }

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (drawable != null) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        } else {
            MonogramLogo(
                logoKey = logoKey,
                name = name,
                size = size,
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
    val color = remember(logoKey) { monogramColor(logoKey) }
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
 * Maps an abstract logo key to a bundled drawable.
 *
 * Empty on purpose: the project does not ship third party trademarks. Adding a licensed asset is a
 * one line change here.
 */
internal object ProviderLogoAssets {

    private val assets: Map<String, Int> = emptyMap()

    @DrawableRes
    fun drawableFor(logoKey: String): Int? = assets[logoKey]
}

private const val MonogramBackgroundAlpha = 0.18f

private fun monogramColor(logoKey: String): Color {
    val index = (logoKey.hashCode().toLong() and 0xFFFFFFFFL) % ChartPalette.size
    return ChartPalette[index.toInt()]
}

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
