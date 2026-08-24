package com.griff.keeper.presentation.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.griff.keeper.domain.model.Money
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.statistics.ExpenseBar
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.YearMonth

/** Which series a chart draws. */
enum class ExpenseSeries {
    SUBSCRIPTIONS,
    OBLIGATIONS,
}

/**
 * Monthly bar chart of one or two expense series.
 *
 * Drawn with [Canvas] on purpose: two simple visualizations do not justify the size and the
 * maintenance cost of a charting library, and the Canvas version follows the color scheme for free.
 *
 * With both series selected the bars are grouped rather than stacked: a stack would invite reading
 * the combined height as one number, and the two halves are not the same kind of number - one is a
 * normalized estimate, the other a settled payment. The legend says which is which.
 */
@Composable
internal fun MonthlyExpenseChart(
    bars: List<ExpenseBar>,
    series: List<ExpenseSeries>,
    modifier: Modifier = Modifier,
) {
    if (bars.isEmpty() || series.isEmpty()) return

    val subscriptionColor = GriffTheme.colors.subscriptionSeries
    val obligationColor = GriffTheme.colors.obligationSeries
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val accentBrush = GriffGradients.accentVertical()
    val obligationBrush = GriffGradients.obligationBarVertical()

    val values = bars.map { bar ->
        series.map { entry ->
            when (entry) {
                ExpenseSeries.SUBSCRIPTIONS -> bar.subscriptions
                ExpenseSeries.OBLIGATIONS -> bar.obligations
            }
        }
    }
    val maxValue = values.flatten().maxOf { it.minorUnits }.coerceAtLeast(1)
    val colors = series.map {
        when (it) {
            ExpenseSeries.SUBSCRIPTIONS -> subscriptionColor
            ExpenseSeries.OBLIGATIONS -> obligationColor
        }
    }
    val brushes = series.map {
        when (it) {
            ExpenseSeries.SUBSCRIPTIONS -> accentBrush
            ExpenseSeries.OBLIGATIONS -> obligationBrush
        }
    }

    val description = bars.joinToString(separator = ", ") { bar ->
        val amounts = series.joinToString(separator = " / ") { entry ->
            when (entry) {
                ExpenseSeries.SUBSCRIPTIONS -> MoneyFormatter.format(bar.subscriptions)
                ExpenseSeries.OBLIGATIONS -> MoneyFormatter.format(bar.obligations)
            }
        }
        "${DateFormatter.formatMonthAndYear(bar.month)}: $amounts"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = MoneyFormatter.format(Money.ofMinorUnits(maxValue)),
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
            drawGroupedBars(
                groups = values.map { group -> group.map { it.minorUnits } },
                maxValue = maxValue,
                brushes = brushes,
                emptyColor = emptyColor,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GroupSpacing),
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

        if (series.size > 1) {
            ChartLegend(
                series = series,
                colors = colors,
                modifier = Modifier.padding(top = Spacing.Medium),
            )
        }
    }
}

@Composable
private fun ChartLegend(
    series: List<ExpenseSeries>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        series.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Box(
                    modifier = Modifier
                        .size(LegendSwatchSize)
                        .clip(RoundedCornerShape(LegendSwatchCorner))
                        .background(colors[index]),
                )
                Text(
                    text = stringResource(
                        when (entry) {
                            ExpenseSeries.SUBSCRIPTIONS ->
                                R.string.statistics_combined_subscriptions_label

                            ExpenseSeries.OBLIGATIONS ->
                                R.string.statistics_combined_obligations_label
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun DrawScope.drawGroupedBars(
    groups: List<List<Long>>,
    maxValue: Long,
    brushes: List<Brush>,
    emptyColor: Color,
) {
    val seriesCount = brushes.size
    val groupSpacing = GroupSpacing.toPx()
    val barSpacing = if (seriesCount > 1) BarSpacing.toPx() else 0f
    val groupWidth =
        ((size.width - groupSpacing * (groups.size - 1)) / groups.size).coerceAtLeast(1f)
    val barWidth =
        ((groupWidth - barSpacing * (seriesCount - 1)) / seriesCount).coerceAtLeast(1f)
    val cornerRadius = CornerRadius(BarCornerRadius.toPx(), BarCornerRadius.toPx())
    val emptyBrush = SolidColor(emptyColor)

    groups.forEachIndexed { groupIndex, group ->
        val groupLeft = groupIndex * (groupWidth + groupSpacing)
        group.forEachIndexed { seriesIndex, value ->
            val ratio = value.toFloat() / maxValue.toFloat()
            val barHeight = (size.height * ratio).coerceAtLeast(MinimumBarHeightPx)
            drawRoundRect(
                brush = if (value == 0L) emptyBrush else brushes[seriesIndex],
                topLeft = Offset(
                    x = groupLeft + seriesIndex * (barWidth + barSpacing),
                    y = size.height - barHeight,
                ),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = cornerRadius,
            )
        }
    }
}

private val ChartHeight = 148.dp
private val GroupSpacing = 6.dp
private val BarSpacing = 2.dp
private val BarCornerRadius = 3.dp
private val LegendSwatchSize = 10.dp
private val LegendSwatchCorner = 2.dp
private const val MinimumBarHeightPx = 3f

@ThemePreviews
@Composable
private fun MonthlyExpenseChartPreview() {
    GriffThemePreview {
        Column(modifier = Modifier.padding(Spacing.Large)) {
            MonthlyExpenseChart(
                bars = List(12) { index ->
                    ExpenseBar(
                        month = YearMonth.of(2026, 1).plusMonths(index.toLong()),
                        subscriptions = Money.ofUnits(286, 40),
                        obligations = if (index % 4 == 2) {
                            Money.ofUnits(1_240L - index * 40)
                        } else {
                            Money.ZERO
                        },
                    )
                },
                series = listOf(ExpenseSeries.SUBSCRIPTIONS, ExpenseSeries.OBLIGATIONS),
            )
        }
    }
}
