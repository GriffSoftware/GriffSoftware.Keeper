package com.griff.keeper.presentation.common.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Segmented button colors for an active choice.
 *
 * Material paints the active segment with the neutral secondary container; a period or a billing
 * period is an active filter, which is one of the states the brand accent is reserved for. The
 * shared helper keeps both selectors identical.
 */
@Composable
internal fun accentSegmentedButtonColors(): SegmentedButtonColors =
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        activeBorderColor = MaterialTheme.colorScheme.primary,
    )

/**
 * The redesign's search-field treatment: filled with a tonal container instead of a bare outline,
 * with the outline kept faint so the field still reads as an input. For [SearchField] and the
 * provider search box, which use a `placeholder` rather than a floating `label` - the outlined
 * variant's border-cutout artifact around a floated label never applies to them.
 */
@Composable
internal fun griffTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

/**
 * The redesign's field treatment for fields with a floating `label` (a date, a price, a name).
 *
 * Deliberately the filled [androidx.compose.material3.TextField], not the outlined variant: an
 * outlined field draws a gap in its border behind a floated label and fills that gap with a
 * background patch, which reads as a stray white rectangle once the field's own fill is a tinted
 * container rather than the page background. The filled variant has no border to cut, so the label
 * just sits inside the tint with nothing to seam against; the indicator line stays invisible until
 * focus, so the field still reads as "filled", not "underlined".
 */
@Composable
internal fun griffFilledTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)
