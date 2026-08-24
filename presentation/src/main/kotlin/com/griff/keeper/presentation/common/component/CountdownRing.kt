package com.griff.keeper.presentation.common.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * A ring that reads at a glance instead of a sentence: the fraction of the billing/validity period
 * already elapsed, with the remaining day count centered inside. Replaces plain "za N dni" text on
 * the subscription details reminders card and in the reminders list.
 *
 * [progress] is the elapsed fraction (0 = just renewed/issued, 1 = due today), clamped defensively
 * since a stale local date could otherwise push it outside the arc's domain.
 */
@Composable
fun CountdownRing(
    daysRemaining: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val strokeWidth = size / 10

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            val diameter = this.size.minDimension - stroke.width
            val topLeft = Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            val sweep = progress.coerceIn(0f, 1f) * 360f

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = daysRemaining.toString(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.countdown_ring_days_unit),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CountdownRingPreview() {
    GriffThemePreview {
        CountdownRing(daysRemaining = 28, progress = 0.78f)
    }
}
