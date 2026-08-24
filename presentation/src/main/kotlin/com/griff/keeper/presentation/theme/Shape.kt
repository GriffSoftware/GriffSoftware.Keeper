package com.griff.keeper.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale of the "Graphite Precision" design system, after the 2026 redesign.
 *
 * Every radius is half of what it was: the system still splits the radius by the *kind* of element
 * rather than by its size, but the whole scale sits tighter, so a card reads as a framed block of
 * data rather than as a rounded tile. Content containers are framed at 8dp, and anything the user
 * acts on directly - buttons, fields, chips - sits at 4dp.
 *
 * The Material roles are mapped so that the component defaults already land on the right value:
 * text fields and chips take `extraSmall`/`small`, cards take `medium`, and dialogs take
 * `extraLarge`. `medium` and `large` are deliberately the same 8dp - the system has one container
 * radius, not two.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/**
 * Shapes that are not a Material role and therefore have to be applied at the call site.
 *
 * Buttons are the notable case: Material derives their shape from a token that the [Shapes] scale
 * cannot override, so a button that should read as a 4dp control has to say so explicitly.
 */
internal object GriffShapes {

    /**
     * Buttons, input fields, chips: the design system's 4dp control radius.
     *
     * Text buttons are left at the Material default on purpose - they have no container to shape,
     * so the only place a radius shows is the ripple.
     */
    val Interactive = RoundedCornerShape(4.dp)

    /** Content containers: cards and card-like blocks. */
    val Container = RoundedCornerShape(8.dp)

    /** Hero cards: the one container allowed a step above [Container]. */
    val Hero = RoundedCornerShape(12.dp)

    /**
     * A hero card that runs to the top edge of the screen, so only the bottom corners are drawn -
     * the subscription details and About headers.
     */
    val HeroTopAttached = RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp)

    /**
     * Selection indicators - drawer rows, active markers, meter bars.
     *
     * A pill deliberately breaks the geometric grid: it is how a transient "you are here" mark
     * stays distinguishable from the containers it sits among. It is the one value the redesign
     * does not halve - a pill is defined by being fully round, not by a radius.
     */
    val Pill = RoundedCornerShape(percent = 50)

    /**
     * The scale's smallest step: drawer selection, and the inner check area of a checkbox.
     *
     * Chart marks - bars, legend swatches - stay below even this: at 10dp across, a 2dp radius
     * reads as a circle rather than as a square with soft corners.
     */
    val Marker = RoundedCornerShape(2.dp)
}
