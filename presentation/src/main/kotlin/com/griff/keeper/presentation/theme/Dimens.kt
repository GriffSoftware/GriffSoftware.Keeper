package com.griff.keeper.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale, so paddings stay consistent across screens.
 *
 * Every value is a multiple of the design system's 8dp base, except [ExtraSmall], which is the half
 * step the system reserves for tight groupings - a label and the field it names.
 */
internal object Spacing {
    /** Tight grouping: a label and its value, an icon and its text. */
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp

    /** Gutter between columns, and the side margin on a phone. */
    val Large = 16.dp

    /** Separation between distinct sections of a screen. */
    val ExtraLarge = 24.dp

    /** Side margin on a wide screen. */
    val Huge = 32.dp
}

/** Hairline used for dividers and for the stroke that defines a container's edge. */
internal val HairlineWidth = 1.dp

/** Minimum touch target required by the Material accessibility guidelines. */
internal val MinTouchTarget = 48.dp
