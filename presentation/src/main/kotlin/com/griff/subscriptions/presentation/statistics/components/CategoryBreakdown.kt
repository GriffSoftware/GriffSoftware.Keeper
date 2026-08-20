package com.griff.subscriptions.presentation.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.Labels
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.statistics.CategoryShare
import com.griff.subscriptions.presentation.theme.ChartPalette
import com.griff.subscriptions.presentation.theme.Spacing
import kotlin.math.roundToInt

/**
 * Cost share per category as horizontal bars.
 *
 * Horizontal bars beat a donut here: they stay readable with ten categories, carry the exact amount
 * next to the label and work with screen readers.
 */
@Composable
internal fun CategoryBreakdown(
    categories: List<CategoryShare>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        categories.forEach { category ->
            CategoryRow(category)
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryShare) {
    val label = stringResource(Labels.category(category.category))
    val amount = stringResource(
        R.string.amount_per_month,
        MoneyFormatter.format(category.monthly),
    )
    val percent = (category.share * 100).roundToInt()
    val color = ChartPalette[category.colorIndex % ChartPalette.size]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label, $amount, $percent%" },
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.statistics_share_percent, percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.Small),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(BarHeight / 2),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = category.share.coerceIn(MinBarFraction, 1f))
                    .height(BarHeight)
                    .background(color = color, shape = RoundedCornerShape(BarHeight / 2)),
            )
        }
    }
}

private val BarHeight = 10.dp
private const val MinBarFraction = 0.02f
