package com.griff.keeper.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandColors.PrimaryLight,
    onPrimary = BrandColors.OnPrimaryLight,
    primaryContainer = BrandColors.PrimaryContainerLight,
    onPrimaryContainer = BrandColors.OnPrimaryContainerLight,
    inversePrimary = BrandColors.InversePrimaryLight,
    secondary = BrandColors.SecondaryLight,
    onSecondary = BrandColors.OnSecondaryLight,
    secondaryContainer = BrandColors.SecondaryContainerLight,
    onSecondaryContainer = BrandColors.OnSecondaryContainerLight,
    tertiary = BrandColors.TertiaryLight,
    onTertiary = BrandColors.OnTertiaryLight,
    tertiaryContainer = BrandColors.TertiaryContainerLight,
    onTertiaryContainer = BrandColors.OnTertiaryContainerLight,
    error = BrandColors.ErrorLight,
    onError = BrandColors.OnErrorLight,
    errorContainer = BrandColors.ErrorContainerLight,
    onErrorContainer = BrandColors.OnErrorContainerLight,
    background = BrandColors.BackgroundLight,
    onBackground = BrandColors.OnBackgroundLight,
    surface = BrandColors.SurfaceLight,
    onSurface = BrandColors.OnSurfaceLight,
    surfaceVariant = BrandColors.SurfaceVariantLight,
    onSurfaceVariant = BrandColors.OnSurfaceVariantLight,
    surfaceTint = BrandColors.PrimaryLight,
    inverseSurface = BrandColors.InverseSurfaceLight,
    inverseOnSurface = BrandColors.InverseOnSurfaceLight,
    outline = BrandColors.OutlineLight,
    outlineVariant = BrandColors.OutlineVariantLight,
    scrim = BrandColors.ScrimLight,
    surfaceBright = BrandColors.SurfaceBrightLight,
    surfaceDim = BrandColors.SurfaceDimLight,
    surfaceContainerLowest = BrandColors.SurfaceContainerLowestLight,
    surfaceContainerLow = BrandColors.SurfaceContainerLowLight,
    surfaceContainer = BrandColors.SurfaceContainerLight,
    surfaceContainerHigh = BrandColors.SurfaceContainerHighLight,
    surfaceContainerHighest = BrandColors.SurfaceContainerHighestLight,
    primaryFixed = BrandColors.PrimaryFixed,
    primaryFixedDim = BrandColors.PrimaryFixedDim,
    onPrimaryFixed = BrandColors.OnPrimaryFixed,
    onPrimaryFixedVariant = BrandColors.OnPrimaryFixedVariant,
    secondaryFixed = BrandColors.SecondaryFixed,
    secondaryFixedDim = BrandColors.SecondaryFixedDim,
    onSecondaryFixed = BrandColors.OnSecondaryFixed,
    onSecondaryFixedVariant = BrandColors.OnSecondaryFixedVariant,
    tertiaryFixed = BrandColors.TertiaryFixed,
    tertiaryFixedDim = BrandColors.TertiaryFixedDim,
    onTertiaryFixed = BrandColors.OnTertiaryFixed,
    onTertiaryFixedVariant = BrandColors.OnTertiaryFixedVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandColors.PrimaryDark,
    onPrimary = BrandColors.OnPrimaryDark,
    primaryContainer = BrandColors.PrimaryContainerDark,
    onPrimaryContainer = BrandColors.OnPrimaryContainerDark,
    inversePrimary = BrandColors.InversePrimaryDark,
    secondary = BrandColors.SecondaryDark,
    onSecondary = BrandColors.OnSecondaryDark,
    secondaryContainer = BrandColors.SecondaryContainerDark,
    onSecondaryContainer = BrandColors.OnSecondaryContainerDark,
    tertiary = BrandColors.TertiaryDark,
    onTertiary = BrandColors.OnTertiaryDark,
    tertiaryContainer = BrandColors.TertiaryContainerDark,
    onTertiaryContainer = BrandColors.OnTertiaryContainerDark,
    error = BrandColors.ErrorDark,
    onError = BrandColors.OnErrorDark,
    errorContainer = BrandColors.ErrorContainerDark,
    onErrorContainer = BrandColors.OnErrorContainerDark,
    background = BrandColors.BackgroundDark,
    onBackground = BrandColors.OnBackgroundDark,
    surface = BrandColors.SurfaceDark,
    onSurface = BrandColors.OnSurfaceDark,
    surfaceVariant = BrandColors.SurfaceVariantDark,
    onSurfaceVariant = BrandColors.OnSurfaceVariantDark,
    surfaceTint = BrandColors.PrimaryDark,
    inverseSurface = BrandColors.InverseSurfaceDark,
    inverseOnSurface = BrandColors.InverseOnSurfaceDark,
    outline = BrandColors.OutlineDark,
    outlineVariant = BrandColors.OutlineVariantDark,
    scrim = BrandColors.ScrimDark,
    surfaceBright = BrandColors.SurfaceBrightDark,
    surfaceDim = BrandColors.SurfaceDimDark,
    surfaceContainerLowest = BrandColors.SurfaceContainerLowestDark,
    surfaceContainerLow = BrandColors.SurfaceContainerLowDark,
    surfaceContainer = BrandColors.SurfaceContainerDark,
    surfaceContainerHigh = BrandColors.SurfaceContainerHighDark,
    surfaceContainerHighest = BrandColors.SurfaceContainerHighestDark,
    primaryFixed = BrandColors.PrimaryFixed,
    primaryFixedDim = BrandColors.PrimaryFixedDim,
    onPrimaryFixed = BrandColors.OnPrimaryFixed,
    onPrimaryFixedVariant = BrandColors.OnPrimaryFixedVariant,
    secondaryFixed = BrandColors.SecondaryFixed,
    secondaryFixedDim = BrandColors.SecondaryFixedDim,
    onSecondaryFixed = BrandColors.OnSecondaryFixed,
    onSecondaryFixedVariant = BrandColors.OnSecondaryFixedVariant,
    tertiaryFixed = BrandColors.TertiaryFixed,
    tertiaryFixedDim = BrandColors.TertiaryFixedDim,
    onTertiaryFixed = BrandColors.OnTertiaryFixed,
    onTertiaryFixedVariant = BrandColors.OnTertiaryFixedVariant,
)

/**
 * The colors the app needs on top of [androidx.compose.material3.ColorScheme]: status colors that
 * Material does not define, and the palette used for provider monograms.
 *
 * Kept in a composition local rather than read from `isSystemInDarkTheme()` at the call site, so a
 * theme forced to light or dark (previews, screenshot tests) stays consistent.
 */
@Immutable
internal data class GriffColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val monogramPalette: List<Color>,
    val tagColors: Map<TagAccent, TagColors>,
    /** Series colors of the combined statistics chart, one per expense source. */
    val subscriptionSeries: Color,
    val obligationSeries: Color,
)

private val LightGriffColors = GriffColors(
    success = SemanticColors.SuccessLight,
    warning = SemanticColors.WarningLight,
    info = SemanticColors.InfoLight,
    monogramPalette = MonogramPalette.Light,
    tagColors = TagPalette.Light,
    subscriptionSeries = BrandColors.PrimaryLight,
    obligationSeries = SeriesColors.ObligationLight,
)

private val DarkGriffColors = GriffColors(
    success = SemanticColors.SuccessDark,
    warning = SemanticColors.WarningDark,
    info = SemanticColors.InfoDark,
    monogramPalette = MonogramPalette.Dark,
    tagColors = TagPalette.Dark,
    subscriptionSeries = BrandColors.PrimaryDark,
    obligationSeries = SeriesColors.ObligationDark,
)

private val LocalGriffColors = staticCompositionLocalOf { LightGriffColors }

/** Access point for the Griff specific colors, mirroring how [MaterialTheme] is used. */
internal object GriffTheme {

    val colors: GriffColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGriffColors.current
}

/**
 * Application theme.
 *
 * Dynamic color is off by default: the brand accent (blue in light, cyan in dark) is the point of
 * the palette, and letting the system wallpaper replace it would make the app look like any other
 * Material sample. The parameter stays so the choice can be exposed as a user setting later.
 */
@Composable
fun GriffKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalGriffColors provides if (darkTheme) DarkGriffColors else LightGriffColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
