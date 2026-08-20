package com.griff.subscriptions.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Fallback brand palette used whenever dynamic color is unavailable (Android 11 and older) or
 * disabled. Generated from a single indigo seed and kept in sync between light and dark schemes.
 */
internal object BrandColors {
    val PrimaryLight = Color(0xFF4C5BA8)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val PrimaryContainerLight = Color(0xFFDEE0FF)
    val OnPrimaryContainerLight = Color(0xFF00135C)
    val SecondaryLight = Color(0xFF5B5D72)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val SecondaryContainerLight = Color(0xFFE0E1F9)
    val OnSecondaryContainerLight = Color(0xFF181A2C)
    val TertiaryLight = Color(0xFF1F6B5B)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val TertiaryContainerLight = Color(0xFFA6F2DD)
    val OnTertiaryContainerLight = Color(0xFF00201A)
    val ErrorLight = Color(0xFFBA1A1A)
    val OnErrorLight = Color(0xFFFFFFFF)
    val ErrorContainerLight = Color(0xFFFFDAD6)
    val OnErrorContainerLight = Color(0xFF410002)
    val BackgroundLight = Color(0xFFFBF8FF)
    val OnBackgroundLight = Color(0xFF1B1B21)
    val SurfaceLight = Color(0xFFFBF8FF)
    val OnSurfaceLight = Color(0xFF1B1B21)
    val SurfaceVariantLight = Color(0xFFE3E1EC)
    val OnSurfaceVariantLight = Color(0xFF46464F)
    val OutlineLight = Color(0xFF777680)
    val OutlineVariantLight = Color(0xFFC7C5D0)
    val SurfaceContainerLight = Color(0xFFF0EDF6)
    val SurfaceContainerHighLight = Color(0xFFEAE7F0)

    val PrimaryDark = Color(0xFFB9C3FF)
    val OnPrimaryDark = Color(0xFF1B2C76)
    val PrimaryContainerDark = Color(0xFF33438E)
    val OnPrimaryContainerDark = Color(0xFFDEE0FF)
    val SecondaryDark = Color(0xFFC4C5DD)
    val OnSecondaryDark = Color(0xFF2D2F42)
    val SecondaryContainerDark = Color(0xFF434559)
    val OnSecondaryContainerDark = Color(0xFFE0E1F9)
    val TertiaryDark = Color(0xFF8AD6C2)
    val OnTertiaryDark = Color(0xFF00382E)
    val TertiaryContainerDark = Color(0xFF005143)
    val OnTertiaryContainerDark = Color(0xFFA6F2DD)
    val ErrorDark = Color(0xFFFFB4AB)
    val OnErrorDark = Color(0xFF690005)
    val ErrorContainerDark = Color(0xFF93000A)
    val OnErrorContainerDark = Color(0xFFFFDAD6)
    val BackgroundDark = Color(0xFF121318)
    val OnBackgroundDark = Color(0xFFE4E1E9)
    val SurfaceDark = Color(0xFF121318)
    val OnSurfaceDark = Color(0xFFE4E1E9)
    val SurfaceVariantDark = Color(0xFF46464F)
    val OnSurfaceVariantDark = Color(0xFFC7C5D0)
    val OutlineDark = Color(0xFF918F9A)
    val OutlineVariantDark = Color(0xFF46464F)
    val SurfaceContainerDark = Color(0xFF1F1F25)
    val SurfaceContainerHighDark = Color(0xFF292930)
}

/**
 * Qualitative palette used for category charts and provider monograms.
 *
 * Colors are picked to stay readable on both light and dark surfaces; the chart maps them by index
 * so a category always keeps the same color within one screen.
 */
internal val ChartPalette: List<Color> = listOf(
    Color(0xFF4C5BA8),
    Color(0xFF1F6B5B),
    Color(0xFFB3541E),
    Color(0xFF7A4EAB),
    Color(0xFF0F6C8C),
    Color(0xFF8C6D1F),
    Color(0xFFA23B58),
    Color(0xFF3F6B21),
    Color(0xFF5C5F72),
    Color(0xFF8A4B77),
)
