package com.griff.keeper.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale of the "Graphite Precision" design system.
 *
 * The system splits the radius by the *kind* of element rather than by its size: content containers
 * are framed generously at 16dp, while anything the user acts on directly - buttons, fields, chips
 * - stays at 8dp, which is what makes a control read as a control and not as a small card.
 *
 * The Material roles are mapped so that the component defaults already land on the right value:
 * text fields and chips take `extraSmall`/`small`, cards take `medium`, and dialogs take
 * `extraLarge`. `medium` and `large` are deliberately the same 16dp - the system has one container
 * radius, not two.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Shapes that are not a Material role and therefore have to be applied at the call site.
 *
 * Buttons are the notable case: Material derives their shape from a token that the [Shapes] scale
 * cannot override, so a button that should read as an 8dp control has to say so explicitly.
 */
internal object GriffShapes {

    /**
     * Buttons, input fields, chips: the design system's 8dp control radius.
     *
     * Text buttons are left at the Material default on purpose - they have no container to shape,
     * so the only place a radius shows is the ripple.
     */
    val Interactive = RoundedCornerShape(8.dp)

    /** Content containers: cards and card-like blocks. */
    val Container = RoundedCornerShape(16.dp)

    /**
     * Selection indicators - drawer rows, active markers, meter bars.
     *
     * A pill deliberately breaks the geometric grid: it is how a transient "you are here" mark
     * stays distinguishable from the containers it sits among.
     */
    val Pill = RoundedCornerShape(percent = 50)

    /**
     * The scale's smallest step: drawer selection, and the inner check area of a checkbox.
     *
     * Chart marks - bars, legend swatches - stay below even this: at 10dp across, a 4dp radius
     * reads as a circle rather than as a square with soft corners.
     */
    val Marker = RoundedCornerShape(4.dp)
}
