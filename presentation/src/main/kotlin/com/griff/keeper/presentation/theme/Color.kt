package com.griff.keeper.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Griff brand palette, following the "Graphite Precision" design system.
 *
 * The accent pivots with the interface mode instead of being inverted: light is a warm off-white
 * surface set driven by blue, dark is a neutral graphite set driven by cyan. Pivoting rather than
 * inverting is what keeps the accent readable at both ends - the blue that carries actions on
 * near-white would sit too dark on graphite, and the cyan that reads on graphite glares on white.
 *
 * Dark surfaces are tiered graphite rather than pure black, so nested containers can appear closer
 * to the user and OLED panels do not smear on scroll.
 *
 * Every Material 3 color role is spelled out, including the ones the app does not use directly
 * (`*Fixed*`, `inverse*`): whatever is left out falls back to the baseline Material purple, which
 * would leak into components such as the navigation drawer or a tonally elevated surface.
 */
internal object BrandColors {

    // --- Light: warm off-white + Griff Blue ---
    val PrimaryLight = Color(0xFF004AC6)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val PrimaryContainerLight = Color(0xFF2563EB)
    val OnPrimaryContainerLight = Color(0xFFEEEFFF)
    val InversePrimaryLight = Color(0xFFB4C5FF)
    val SecondaryLight = Color(0xFF006877)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val SecondaryContainerLight = Color(0xFF3FE1FD)
    val OnSecondaryContainerLight = Color(0xFF00616F)
    val TertiaryLight = Color(0xFF943700)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val TertiaryContainerLight = Color(0xFFBC4800)
    val OnTertiaryContainerLight = Color(0xFFFFEDE6)
    val ErrorLight = Color(0xFFBA1A1A)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val OnErrorContainerLight = Color(0xFF93000A)
    val BackgroundLight = Color(0xFFFCF9F8)
    val OnBackgroundLight = Color(0xFF1C1B1B)
    val SurfaceLight = Color(0xFFFCF9F8)
    val OnSurfaceLight = Color(0xFF1C1B1B)
    val SurfaceVariantLight = Color(0xFFE5E2E1)
    val OnSurfaceVariantLight = Color(0xFF434655)
    val InverseSurfaceLight = Color(0xFF313030)
    val InverseOnSurfaceLight = Color(0xFFF3F0EF)
    val OutlineLight = Color(0xFF737686)
    val OutlineVariantLight = Color(0xFFC3C6D7)
    val ScrimLight = Color(0xFF000000)
    val SurfaceTintLight = Color(0xFF0053DB)

    // Neutral surface ramp, from the brightest card to the dimmest backdrop.
    val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowLight = Color(0xFFF6F3F2)
    val SurfaceContainerLight = Color(0xFFF0EDED)
    val SurfaceContainerHighLight = Color(0xFFEAE7E7)
    val SurfaceContainerHighestLight = Color(0xFFE5E2E1)
    val SurfaceBrightLight = Color(0xFFFCF9F8)
    val SurfaceDimLight = Color(0xFFDCD9D9)

    // --- Dark: graphite + Griff Cyan ---
    val PrimaryDark = Color(0xFF22D3EE)
    val OnPrimaryDark = Color(0xFF00363F)
    val PrimaryContainerDark = Color(0xFF0E7490)
    val OnPrimaryContainerDark = Color(0xFFCFFAFE)
    val InversePrimaryDark = Color(0xFF004AC6)

    // Secondary stays neutral slate in dark instead of mirroring the light teal: with cyan on
    // primary, a teal secondary would read as a slightly-off copy of the accent rather than as a
    // second role. The slate is the same family as the light onSurfaceVariant / outlineVariant.
    val SecondaryDark = Color(0xFFC3C6D7)
    val OnSecondaryDark = Color(0xFF2C2F3E)
    val SecondaryContainerDark = Color(0xFF434655)
    val OnSecondaryContainerDark = Color(0xFFE1E2F0)

    val TertiaryDark = Color(0xFFFFB596)
    val OnTertiaryDark = Color(0xFF561C00)
    val TertiaryContainerDark = Color(0xFF7D2D00)
    val OnTertiaryContainerDark = Color(0xFFFFDBCD)
    val ErrorDark = Color(0xFFFFB4AB)
    val OnErrorDark = Color(0xFF690005)
    val ErrorContainerDark = Color(0xFF93000A)
    val OnErrorContainerDark = Color(0xFFFFDAD6)
    val BackgroundDark = Color(0xFF191919)
    val OnBackgroundDark = Color(0xFFE5E2E1)
    val SurfaceDark = Color(0xFF191919)
    val OnSurfaceDark = Color(0xFFE5E2E1)
    val SurfaceVariantDark = Color(0xFF2D2D2D)
    val OnSurfaceVariantDark = Color(0xFFC3C6D7)
    val InverseSurfaceDark = Color(0xFFE5E2E1)
    val InverseOnSurfaceDark = Color(0xFF313030)
    val OutlineDark = Color(0xFF8D909F)

    // Dividers and hairlines on graphite: the design system's `graphite-muted`.
    val OutlineVariantDark = Color(0xFF2D2D2D)
    val ScrimDark = Color(0xFF000000)
    val SurfaceTintDark = Color(0xFF22D3EE)

    val SurfaceContainerLowestDark = Color(0xFF141414)
    val SurfaceContainerLowDark = Color(0xFF1E1E1E)
    val SurfaceContainerDark = Color(0xFF232323)
    val SurfaceContainerHighDark = Color(0xFF2D2D2D)
    val SurfaceContainerHighestDark = Color(0xFF373737)
    val SurfaceBrightDark = Color(0xFF373737)
    val SurfaceDimDark = Color(0xFF141414)

    // --- Fixed roles: identical in both themes by definition ---
    val PrimaryFixed = Color(0xFFDBE1FF)
    val PrimaryFixedDim = Color(0xFFB4C5FF)
    val OnPrimaryFixed = Color(0xFF00174B)
    val OnPrimaryFixedVariant = Color(0xFF003EA8)
    val SecondaryFixed = Color(0xFFA2EEFF)
    val SecondaryFixedDim = Color(0xFF2FD9F4)
    val OnSecondaryFixed = Color(0xFF001F25)
    val OnSecondaryFixedVariant = Color(0xFF004E5A)
    val TertiaryFixed = Color(0xFFFFDBCD)
    val TertiaryFixedDim = Color(0xFFFFB596)
    val OnTertiaryFixed = Color(0xFF360F00)
    val OnTertiaryFixedVariant = Color(0xFF7D2D00)
}

/**
 * Edge definition for elevated containers.
 *
 * On graphite, a card one tier lighter than its background is not always separable on its own, so
 * cards carry a hairline stroke of white at 5% opacity. Light mode needs no such trick - there the
 * tonal step plus `outlineVariant` already reads - so the light value is fully transparent and call
 * sites can draw the border unconditionally.
 */
internal object ContainerStroke {
    val Light = Color.Transparent
    val Dark = Color.White.copy(alpha = 0.05f)
}

/**
 * Status colors that carry meaning on their own and therefore cannot be derived from the brand
 * palette: Material only defines `error`, and success/warning/info are not interchangeable with it.
 *
 * Use them *only* for the state they name (see [com.griff.keeper.presentation.common
 * .MessageSeverity]), never as decoration, and never as the only carrier of the information - every
 * call site pairs them with an icon or with text.
 */
internal object SemanticColors {

    val SuccessLight = Color(0xFF047857)
    val SuccessDark = Color(0xFF34D399)

    val WarningLight = Color(0xFFB45309)
    val WarningDark = Color(0xFFF59E0B)

    val InfoLight = Color(0xFF1D4ED8)
    val InfoDark = Color(0xFF60A5FA)
}

/**
 * Qualitative palette for category charts.
 *
 * These are *data* colors, so they are intentionally hard-coded instead of taken from the color
 * scheme; the chart maps them by index, which keeps a category on the same color within one screen.
 * The first four entries are the design system's chart palette in its own order; the rest extend it
 * with hues of the same saturation, for the categories the spec's four slots do not cover.
 */
internal val ChartPalette: List<Color> = listOf(
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Red
    Color(0xFF8B5CF6), // Violet
    Color(0xFF22D3EE), // Cyan
    Color(0xFFEC4899), // Pink
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6), // Teal
    Color(0xFF64748B), // Slate
)

/**
 * The [ChartPalette] hues, shifted to stay readable as *text*.
 *
 * Provider monograms render initials in the color derived from the logo key. Chart bars are large
 * blocks and get away with vivid mid-tones, but the same mid-tones fail the 4.5:1 text contrast
 * requirement - amber on off-white and blue on graphite both do - so each hue has a darker
 * light-theme and a lighter dark-theme variant. Index `n` is the same hue as `ChartPalette[n]`.
 */
internal object MonogramPalette {

    val Light: List<Color> = listOf(
        Color(0xFF1D4ED8), // Blue
        Color(0xFF047857), // Emerald
        Color(0xFFB45309), // Amber
        Color(0xFFB91C1C), // Red
        Color(0xFF6D28D9), // Violet
        Color(0xFF0E7490), // Cyan
        Color(0xFFBE185D), // Pink
        Color(0xFFC2410C), // Orange
        Color(0xFF0F766E), // Teal
        Color(0xFF475569), // Slate
    )

    val Dark: List<Color> = listOf(
        Color(0xFF93C5FD), // Blue
        Color(0xFF6EE7B7), // Emerald
        Color(0xFFFCD34D), // Amber
        Color(0xFFFCA5A5), // Red
        Color(0xFFC4B5FD), // Violet
        Color(0xFF67E8F9), // Cyan
        Color(0xFFF9A8D4), // Pink
        Color(0xFFFDBA74), // Orange
        Color(0xFF5EEAD4), // Teal
        Color(0xFFCBD5E1), // Slate
    )
}

/**
 * Accents available to category tags.
 *
 * A tag is an identifier, not a status, so its color carries no severity - which is exactly why the
 * status colors in [SemanticColors] are not reused here. Every accent exists in a light and a dark
 * variant (see [TagPalette]); reusing one container color across both themes would either wash out
 * on off-white or glare on graphite.
 */
enum class TagAccent {
    BLUE,
    CYAN,
    EMERALD,
    OLIVE,
    AMBER,
    ORANGE,
    RED,
    PINK,
    VIOLET,
    INDIGO,
    TEAL,
    SLATE,
}

/** Container and content color of a tag, always used as a pair so contrast is never accidental. */
@Immutable
internal data class TagColors(
    val container: Color,
    val content: Color,
)

/**
 * Tag colors per theme.
 *
 * Light uses a very light tint of the hue with dark text on it; dark uses a desaturated, deep tint
 * with light text. Both directions are tuned to stay legible at `labelMedium` size on the app's own
 * surfaces rather than on pure white or pure black. [TagAccent.INDIGO] is the design system's
 * reference pair (`tag-bg` on `tag-text`) and sets the contrast target the other accents match.
 */
internal object TagPalette {

    val Light: Map<TagAccent, TagColors> = mapOf(
        TagAccent.BLUE to colors(0xFFDBEAFE, 0xFF1E40AF),
        TagAccent.CYAN to colors(0xFFCFFAFE, 0xFF155E75),
        TagAccent.EMERALD to colors(0xFFD1FAE5, 0xFF065F46),
        TagAccent.OLIVE to colors(0xFFE7F0CB, 0xFF4D6B10),
        TagAccent.AMBER to colors(0xFFFEF3C7, 0xFF92400E),
        TagAccent.ORANGE to colors(0xFFFFEDD5, 0xFF9A3412),
        TagAccent.RED to colors(0xFFFEE2E2, 0xFF991B1B),
        TagAccent.PINK to colors(0xFFFCE7F3, 0xFF9D174D),
        TagAccent.VIOLET to colors(0xFFEDE9FE, 0xFF5B21B6),
        TagAccent.INDIGO to colors(0xFFE0E7FF, 0xFF3730A3),
        TagAccent.TEAL to colors(0xFFCCFBF1, 0xFF115E59),
        TagAccent.SLATE to colors(0xFFE2E8F0, 0xFF334155),
    )

    val Dark: Map<TagAccent, TagColors> = mapOf(
        TagAccent.BLUE to colors(0xFF1E3A5F, 0xFFBFDBFE),
        TagAccent.CYAN to colors(0xFF164E63, 0xFFA5F3FC),
        TagAccent.EMERALD to colors(0xFF10402F, 0xFF6EE7B7),
        TagAccent.OLIVE to colors(0xFF33401A, 0xFFC8DE8A),
        TagAccent.AMBER to colors(0xFF453014, 0xFFFCD34D),
        TagAccent.ORANGE to colors(0xFF4A2711, 0xFFFDBA74),
        TagAccent.RED to colors(0xFF4C1D1D, 0xFFFCA5A5),
        TagAccent.PINK to colors(0xFF4A1733, 0xFFF9A8D4),
        TagAccent.VIOLET to colors(0xFF32215E, 0xFFC4B5FD),
        TagAccent.INDIGO to colors(0xFF272B5E, 0xFFA5B4FC),
        TagAccent.TEAL to colors(0xFF11433D, 0xFF5EEAD4),
        TagAccent.SLATE to colors(0xFF2C333F, 0xFFCBD5E1),
    )

    private fun colors(container: Long, content: Long) =
        TagColors(container = Color(container), content = Color(content))
}

/**
 * Colors of the two series on the combined statistics chart.
 *
 * Subscriptions take the brand accent (blue in light, cyan in dark) because they are the app's
 * primary subject; obligations get the chart palette's emerald, which stays clearly distinguishable
 * from the accent in both themes without competing with it.
 */
internal object SeriesColors {
    val ObligationLight = Color(0xFF047857)
    val ObligationDark = Color(0xFF34D399)
}
