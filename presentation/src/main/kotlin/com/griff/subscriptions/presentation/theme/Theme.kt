package com.griff.subscriptions.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandColors.PrimaryLight,
    onPrimary = BrandColors.OnPrimaryLight,
    primaryContainer = BrandColors.PrimaryContainerLight,
    onPrimaryContainer = BrandColors.OnPrimaryContainerLight,
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
    outline = BrandColors.OutlineLight,
    outlineVariant = BrandColors.OutlineVariantLight,
    surfaceContainer = BrandColors.SurfaceContainerLight,
    surfaceContainerHigh = BrandColors.SurfaceContainerHighLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandColors.PrimaryDark,
    onPrimary = BrandColors.OnPrimaryDark,
    primaryContainer = BrandColors.PrimaryContainerDark,
    onPrimaryContainer = BrandColors.OnPrimaryContainerDark,
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
    outline = BrandColors.OutlineDark,
    outlineVariant = BrandColors.OutlineVariantDark,
    surfaceContainer = BrandColors.SurfaceContainerDark,
    surfaceContainerHigh = BrandColors.SurfaceContainerHighDark,
)

/**
 * Application theme.
 *
 * Dynamic color is used on Android 12+ because it makes the app feel native; the brand palette is a
 * complete fallback for older devices and for previews.
 */
@Composable
fun GriffSubscriptionsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
