package com.griff.keeper.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.griff.keeper.presentation.R

/**
 * Google Fonts provider, used as a downloadable font source.
 *
 * Inter is fetched by the Play Services font provider instead of being bundled: the four weights
 * the type scale needs would add roughly a megabyte to the APK for a face that most devices already
 * cache. The trade-off is that the first render on a device without it falls back to the platform
 * font - which is why every style below states its weight explicitly rather than relying on a
 * weight only Inter ships.
 */
private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val Inter = GoogleFont("Inter")

/**
 * The single family the app uses.
 *
 * Inter is a neutral, high x-height grotesque, which is what keeps dense numeric rows - amounts,
 * dates, periods - legible at `bodyMedium` size. Only the weights the scale actually uses are
 * declared, so no request is made for a face nothing renders.
 */
internal val InterFontFamily = FontFamily(
    Font(googleFont = Inter, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = Inter, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = Inter, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Inter, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
)

/**
 * Type scale of the "Graphite Precision" design system, mapped onto the Material 3 roles.
 *
 * Headlines and displays use tighter tracking and heavier weights so they anchor a screen without
 * needing size alone; body text keeps default tracking, which is what readability at 14sp in a
 * data-heavy list depends on. Labels are `Medium` with slightly open tracking, because they appear
 * as all-caps-adjacent short strings in buttons, chips and tags where tight tracking reads as a
 * blur.
 *
 * `labelLarge` is the one deliberate departure from the spec's `label-md`: it is the style Material
 * puts on button text, and 12sp there falls below what is comfortably tappable-and-readable on a
 * phone. Chips and tags, which the spec names explicitly, use `labelMedium` at the specified 12sp.
 */
internal val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.02).em,
    ),
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.01).em,
    ),
    // The spec's `headline-lg-mobile`: the size a phone screen headline actually uses.
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    ),
    // The spec's `label-md`: buttons in the design system, chips and tags here.
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.em,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)
