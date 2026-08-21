package com.griff.keeper.presentation.common.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable

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
