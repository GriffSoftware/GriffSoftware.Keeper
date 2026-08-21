package com.griff.keeper.presentation.statistics.components

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
import com.griff.keeper.domain.model.Money
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.TagStyle
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.statistics.SpendingShare
import com.griff.keeper.presentation.theme.GriffTheme
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.TagAccent
import com.griff.keeper.presentation.theme.ThemePreviews
import kotlin.math.roundToInt

/** How the amount next to a breakdown row is worded. */
enum class BreakdownAmount {
    /** A normalized subscription cost, printed as "x / mies.". */
    PER_MONTH,

    /** An amount that was actually paid, printed as it is. */
    ABSOLUTE,
}

/**
 * Cost share per category as horizontal bars.
 *
 * Horizontal bars beat a donut here: they stay readable with ten categories, carry the exact amount
 * next to the label and work with screen readers. Each bar takes the color of its category's tag, so
 * the breakdown, the badges on the lists and the filter chips all agree.
 */
@Composable
internal fun SpendingBreakdown(
    entries: List<SpendingShare>,
    amountStyle: BreakdownAmount,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        entries.forEach { entry ->
            SpendingRow(entry = entry, amountStyle = amountStyle)
        }
    }
}

@Composable
private fun SpendingRow(entry: SpendingShare, amountStyle: BreakdownAmount) {
    val label = stringResource(entry.style.labelRes)
    val amount = when (amountStyle) {
        BreakdownAmount.PER_MONTH ->
            stringResource(R.string.amount_per_month, MoneyFormatter.format(entry.amount))

        BreakdownAmount.ABSOLUTE -> MoneyFormatter.format(entry.amount)
    }
    val percent = (entry.share * PERCENT).roundToInt()
    // Resolved before the semantics lambda, which cannot call a composable.
    val description = stringResource(R.string.statistics_share_description, label, amount, percent)
    val colors = GriffTheme.colors.tagColors.getValue(entry.style.accent)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
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
                    .fillMaxWidth(fraction = entry.share.coerceIn(MinBarFraction, 1f))
                    .height(BarHeight)
                    .background(color = colors.content, shape = RoundedCornerShape(BarHeight / 2)),
            )
        }
    }
}

private val BarHeight = 10.dp
private const val MinBarFraction = 0.02f
private const val PERCENT = 100

@ThemePreviews
@Composable
private fun SpendingBreakdownPreview() {
    GriffThemePreview {
        SpendingBreakdown(
            entries = listOf(
                SpendingShare(
                    TagStyle(R.string.category_video, TagAccent.RED),
                    Money.ofUnits(120),
                    0.42f,
                ),
                SpendingShare(
                    TagStyle(R.string.category_music, TagAccent.EMERALD),
                    Money.ofUnits(70),
                    0.24f,
                ),
                SpendingShare(
                    TagStyle(R.string.category_ai, TagAccent.CYAN),
                    Money.ofUnits(58),
                    0.20f,
                ),
            ),
            amountStyle = BreakdownAmount.PER_MONTH,
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
