package com.griff.subscriptions.presentation.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.format.DateFormatter
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.statistics.ForecastBar
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews
import java.time.YearMonth

/**
 * Bar chart of projected charges per month.
 *
 * Drawn with [Canvas] on purpose: a single, simple visualization does not justify the size and the
 * maintenance cost of a charting library, and the Canvas version follows the Material color scheme
 * for free.
 */
@Composable
internal fun ForecastChart(
    bars: List<ForecastBar>,
    modifier: Modifier = Modifier,
) {
    if (bars.isEmpty()) return

    val maxAmount = bars.maxOf { it.amount.minorUnits }.coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
    val description = bars.joinToString(separator = ", ") { bar ->
        "${DateFormatter.formatMonthAndYear(bar.month)}: ${MoneyFormatter.format(bar.amount)}"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = MoneyFormatter.format(bars.maxByOrNull { it.amount.minorUnits }!!.amount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .padding(vertical = Spacing.Small)
                .semantics { contentDescription = description },
        ) {
            drawBars(
                values = bars.map { it.amount.minorUnits },
                maxValue = maxAmount,
                barColor = barColor,
                emptyBarColor = emptyBarColor,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BarSpacing),
        ) {
            bars.forEach { bar ->
                Text(
                    text = DateFormatter.formatRomanMonth(bar.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = stringResource(R.string.statistics_forecast_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.Small),
        )
    }
}

private fun DrawScope.drawBars(
    values: List<Long>,
    maxValue: Long,
    barColor: androidx.compose.ui.graphics.Color,
    emptyBarColor: androidx.compose.ui.graphics.Color,
) {
    val spacing = BarSpacing.toPx()
    val barWidth = ((size.width - spacing * (values.size - 1)) / values.size).coerceAtLeast(1f)
    val cornerRadius = CornerRadius(BarCornerRadius.toPx(), BarCornerRadius.toPx())

    values.forEachIndexed { index, value ->
        val ratio = value.toFloat() / maxValue.toFloat()
        val barHeight = (size.height * ratio).coerceAtLeast(MinimumBarHeightPx)
        val left = index * (barWidth + spacing)
        drawRoundRect(
            color = if (value == 0L) emptyBarColor else barColor,
            topLeft = Offset(x = left, y = size.height - barHeight),
            size = Size(width = barWidth, height = barHeight),
            cornerRadius = cornerRadius,
        )
    }
}

private val ChartHeight = 148.dp
private val BarSpacing = 6.dp
private val BarCornerRadius = 4.dp
private const val MinimumBarHeightPx = 3f

@ThemePreviews
@Composable
private fun ForecastChartPreview() {
    GriffThemePreview {
        ForecastChart(
            bars = List(12) { index ->
                ForecastBar(
                    month = YearMonth.of(2026, 8).plusMonths(index.toLong()),
                    amount = Money.ofUnits(120L + index * 25),
                )
            },
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
