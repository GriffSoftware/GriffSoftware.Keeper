package com.griff.keeper.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Griff brand palette.
 *
 * Light is an almost white, "financial dashboard" surface set with a saturated blue accent; dark is
 * a neutral graphite set with a cyan accent. The two themes share the same neutrals (slate) and the
 * same structure, but deliberately *not* the same accent - dark is not a negative of light.
 *
 * Every Material 3 color role is spelled out, including the ones the app does not use directly
 * (`*Fixed*`, `inverse*`): whatever is left out falls back to the baseline Material purple, which
 * would leak into components such as the navigation drawer or a tonally elevated surface.
 */
internal object BrandColors {

    // --- Light: white + Griff Blue ---
    val PrimaryLight = Color(0xFF2563EB)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val PrimaryContainerLight = Color(0xFFDBEAFE)
    val OnPrimaryContainerLight = Color(0xFF172554)
    val InversePrimaryLight = Color(0xFF93C5FD)
    val SecondaryLight = Color(0xFF475569)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val SecondaryContainerLight = Color(0xFFE2E8F0)
    val OnSecondaryContainerLight = Color(0xFF1E293B)
    val TertiaryLight = Color(0xFF0891B2)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val TertiaryContainerLight = Color(0xFFCFFAFE)
    val OnTertiaryContainerLight = Color(0xFF164E63)
    val ErrorLight = Color(0xFFDC2626)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFFEE2E2)
    val OnErrorContainerLight = Color(0xFF7F1D1D)
    val BackgroundLight = Color(0xFFFAFBFD)
    val OnBackgroundLight = Color(0xFF111827)
    val SurfaceLight = Color(0xFFFFFFFF)
    val OnSurfaceLight = Color(0xFF111827)
    val SurfaceVariantLight = Color(0xFFF1F5F9)
    val OnSurfaceVariantLight = Color(0xFF475569)
    val InverseSurfaceLight = Color(0xFF1F2937)
    val InverseOnSurfaceLight = Color(0xFFF9FAFB)
    val OutlineLight = Color(0xFF94A3B8)
    val OutlineVariantLight = Color(0xFFE2E8F0)
    val ScrimLight = Color(0xFF000000)

    // Neutral surface ramp, from the brightest card to the dimmest backdrop.
    val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowLight = Color(0xFFF8FAFC)
    val SurfaceContainerLight = Color(0xFFF3F6FA)
    val SurfaceContainerHighLight = Color(0xFFE9EEF5)
    val SurfaceContainerHighestLight = Color(0xFFDFE6EF)
    val SurfaceBrightLight = Color(0xFFFFFFFF)
    val SurfaceDimLight = Color(0xFFDEE5EE)

    // --- Dark: graphite + Griff Cyan ---
    val PrimaryDark = Color(0xFF22D3EE)
    val OnPrimaryDark = Color(0xFF083344)
    val PrimaryContainerDark = Color(0xFF164E63)
    val OnPrimaryContainerDark = Color(0xFFCFFAFE)
    val InversePrimaryDark = Color(0xFF0E7490)
    val SecondaryDark = Color(0xFFCBD5E1)
    val OnSecondaryDark = Color(0xFF1E293B)
    val SecondaryContainerDark = Color(0xFF334155)
    val OnSecondaryContainerDark = Color(0xFFE2E8F0)
    val TertiaryDark = Color(0xFF67E8F9)
    val OnTertiaryDark = Color(0xFF083344)
    val TertiaryContainerDark = Color(0xFF155E75)
    val OnTertiaryContainerDark = Color(0xFFCFFAFE)
    val ErrorDark = Color(0xFFFCA5A5)
    val OnErrorDark = Color(0xFF7F1D1D)
    val ErrorContainerDark = Color(0xFF7F1D1D)
    val OnErrorContainerDark = Color(0xFFFEE2E2)
    val BackgroundDark = Color(0xFF0F1115)
    val OnBackgroundDark = Color(0xFFE5E7EB)
    val SurfaceDark = Color(0xFF14171C)
    val OnSurfaceDark = Color(0xFFF1F5F9)
    val SurfaceVariantDark = Color(0xFF252A32)
    val OnSurfaceVariantDark = Color(0xFFCBD5E1)
    val InverseSurfaceDark = Color(0xFFE5E7EB)
    val InverseOnSurfaceDark = Color(0xFF1F2937)
    val OutlineDark = Color(0xFF64748B)
    val OutlineVariantDark = Color(0xFF334155)
    val ScrimDark = Color(0xFF000000)

    val SurfaceContainerLowestDark = Color(0xFF0B0D11)
    val SurfaceContainerLowDark = Color(0xFF171B21)
    val SurfaceContainerDark = Color(0xFF1B1F26)
    val SurfaceContainerHighDark = Color(0xFF232831)
    val SurfaceContainerHighestDark = Color(0xFF2C323C)
    val SurfaceBrightDark = Color(0xFF343B45)
    val SurfaceDimDark = Color(0xFF0B0D10)

    // --- Fixed roles: identical in both themes by definition ---
    val PrimaryFixed = Color(0xFFDBEAFE)
    val PrimaryFixedDim = Color(0xFFBFDBFE)
    val OnPrimaryFixed = Color(0xFF172554)
    val OnPrimaryFixedVariant = Color(0xFF1D4ED8)
    val SecondaryFixed = Color(0xFFE2E8F0)
    val SecondaryFixedDim = Color(0xFFCBD5E1)
    val OnSecondaryFixed = Color(0xFF1E293B)
    val OnSecondaryFixedVariant = Color(0xFF334155)
    val TertiaryFixed = Color(0xFFCFFAFE)
    val TertiaryFixedDim = Color(0xFFA5F3FC)
    val OnTertiaryFixed = Color(0xFF083344)
    val OnTertiaryFixedVariant = Color(0xFF0E7490)
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

    val SuccessLight = Color(0xFF15803D)
    val SuccessDark = Color(0xFF4ADE80)

    val WarningLight = Color(0xFFD97706)
    val WarningDark = Color(0xFFFBBF24)

    val InfoLight = Color(0xFF0284C7)
    val InfoDark = Color(0xFF38BDF8)
}

/**
 * Qualitative palette for category charts.
 *
 * These are *data* colors, so they are intentionally hard-coded instead of taken from the color
 * scheme; the chart maps them by index, which keeps a category on the same color within one screen.
 * Blue leads the palette so the busiest category matches the light brand accent.
 */
internal val ChartPalette: List<Color> = listOf(
    Color(0xFF2563EB), // Blue
    Color(0xFF06B6D4), // Cyan
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6), // Teal
    Color(0xFF6366F1), // Indigo
    Color(0xFF64748B), // Slate
)

/**
 * The [ChartPalette] hues, shifted to stay readable as *text*.
 *
 * Provider monograms render initials in the color derived from the logo key. Chart bars are large
 * blocks and get away with vivid mid-tones, but the same mid-tones fail the 4.5:1 text contrast
 * requirement - amber on white and blue on graphite both do - so each hue has a darker light-theme
 * and a lighter dark-theme variant. Index `n` is the same hue as `ChartPalette[n]`.
 */
internal object MonogramPalette {

    val Light: List<Color> = listOf(
        Color(0xFF1D4ED8), // Blue
        Color(0xFF0E7490), // Cyan
        Color(0xFF047857), // Emerald
        Color(0xFFB45309), // Amber
        Color(0xFF6D28D9), // Violet
        Color(0xFFBE185D), // Pink
        Color(0xFFC2410C), // Orange
        Color(0xFF0F766E), // Teal
        Color(0xFF4338CA), // Indigo
        Color(0xFF475569), // Slate
    )

    val Dark: List<Color> = listOf(
        Color(0xFF93C5FD), // Blue
        Color(0xFF67E8F9), // Cyan
        Color(0xFF6EE7B7), // Emerald
        Color(0xFFFCD34D), // Amber
        Color(0xFFC4B5FD), // Violet
        Color(0xFFF9A8D4), // Pink
        Color(0xFFFDBA74), // Orange
        Color(0xFF5EEAD4), // Teal
        Color(0xFFA5B4FC), // Indigo
        Color(0xFFCBD5E1), // Slate
    )
}

/**
 * Accents available to category tags.
 *
 * A tag is an identifier, not a status, so its color carries no severity - which is exactly why the
 * status colors in [SemanticColors] are not reused here. Every accent exists in a light and a dark
 * variant (see [TagPalette]); reusing one container color across both themes would either wash out
 * on white or glare on graphite.
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
 * with light text. Both directions are tuned to stay legible at `labelSmall` size on the app's own
 * surfaces rather than on pure white or pure black.
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
 * primary subject; obligations get emerald, which stays clearly distinguishable from the accent in
 * both themes without competing with it.
 */
internal object SeriesColors {
    val ObligationLight = Color(0xFF059669)
    val ObligationDark = Color(0xFF34D399)
}
