package com.griff.keeper.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance

/**
 * Brand brushes of the navy gradient - the redesign's one recurring signature, reused for hero
 * cards, primary actions, the FAB, the drawer header and the subscription series of the statistics
 * chart.
 *
 * Theme is read from [MaterialTheme]'s own `colorScheme.surface` luminance rather than
 * `isSystemInDarkTheme()`, so a theme forced by [GriffKeeperTheme]'s `darkTheme` parameter -
 * previews, screenshot tests - resolves the right gradient without this file depending on
 * `Theme.kt` or vice versa.
 */
internal object GriffGradients {

    /** Below this, `colorScheme.surface` reads as a dark theme. */
    private const val DarkSurfaceLuminanceCeiling = 0.5f

    @Composable
    @ReadOnlyComposable
    private fun isDark(): Boolean =
        MaterialTheme.colorScheme.surface.luminance() < DarkSurfaceLuminanceCeiling

    /** The signature navy sweep: hero cards, primary CTAs, the FAB, the splash background. */
    @Composable
    @ReadOnlyComposable
    fun accent(): Brush = Brush.linearGradient(if (isDark()) DarkAccentStops else LightAccentStops)

    /** [accent], running top-to-bottom - the drawer header and the subscription chart bars. */
    @Composable
    @ReadOnlyComposable
    fun accentVertical(): Brush =
        Brush.verticalGradient(if (isDark()) DarkAccentStops else LightAccentStops)

    /** [accent], running left-to-right - the subscription-series ranking bars. */
    @Composable
    @ReadOnlyComposable
    fun accentHorizontal(): Brush =
        Brush.horizontalGradient(if (isDark()) DarkAccentStops else LightAccentStops)

    /** Content color for anything painted on [accent] / [accentVertical]: white at both ends. */
    val OnAccent = Color.White

    /**
     * The soft light-catch near a hero card's top-right corner that keeps the gradient from
     * reading as a flat fill.
     *
     * [Brush.radialGradient]'s own default center/radius cover the *whole* element - exactly what
     * this must not do, since the highlight is meant to sit in one corner, not wash out the entire
     * gradient underneath it. A small [ShaderBrush] positions and sizes it as fractions of the
     * actual draw size instead, so it scales correctly whether it lands on a hero card or the
     * full-screen splash background.
     */
    @Composable
    @ReadOnlyComposable
    fun sheen(): Brush {
        val alpha = if (isDark()) 0.28f else 0.32f
        return object : ShaderBrush() {
            override fun createShader(size: Size): Shader = RadialGradientShader(
                center = Offset(size.width * SheenCenterXFraction, size.height * SheenCenterYFraction),
                radius = (size.minDimension * SheenRadiusFraction).coerceAtLeast(1f),
                colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
            )
        }
    }

    /** Translucent tile background for the small stat chips laid over a hero card. */
    @Composable
    @ReadOnlyComposable
    fun veil(): Color = Color.White.copy(alpha = if (isDark()) 0.14f else 0.16f)

    /**
     * The obligation "paid" gradient - a framing of the existing [SeriesColors] emerald, not a new
     * color. Statistics keeps green reserved for obligations so a chart bar never lets a paid fee
     * get mistaken for an estimated subscription cost.
     */
    @Composable
    @ReadOnlyComposable
    fun obligationBarVertical(): Brush =
        Brush.verticalGradient(if (isDark()) DarkObligationBarStops else LightObligationBarStops)

    /** [obligationBarVertical], running left-to-right - ranking bars in "Największe koszty". */
    @Composable
    @ReadOnlyComposable
    fun obligationBarHorizontal(): Brush =
        Brush.horizontalGradient(if (isDark()) DarkObligationBarStops else LightObligationBarStops)

    private val LightAccentStops = listOf(Color(0xFF0F1C28), Color(0xFF24384A), Color(0xFF4E7291))
    private val DarkAccentStops = listOf(Color(0xFF080F17), Color(0xFF132433), Color(0xFF2A4459))

    private val LightObligationBarStops = listOf(Color(0xFF34D399), Color(0xFF047857))
    private val DarkObligationBarStops = listOf(Color(0xFF6EE7B7), Color(0xFF0F766E))

    private const val SheenCenterXFraction = 0.88f
    private const val SheenCenterYFraction = 0.05f
    private const val SheenRadiusFraction = 0.7f
}
