package com.griff.subscriptions.presentation.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews

/**
 * Monthly cost, yearly cost and the number of subscriptions.
 *
 * The tiles stay neutral - a white (light) or graphite (dark) card with a hairline outline; only
 * the monthly cost, the number the screen is about, is printed in the accent color, so the row has
 * a single focal point instead of three.
 */
@Composable
internal fun SummaryCards(
    totals: SubscriptionTotals,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        SummaryCard(
            label = stringResource(R.string.statistics_monthly_label),
            value = MoneyFormatter.format(totals.monthly),
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1.2f),
        )
        SummaryCard(
            label = stringResource(R.string.statistics_yearly_label),
            value = MoneyFormatter.format(totals.yearly),
            modifier = Modifier.weight(1.2f),
        )
        SummaryCard(
            label = stringResource(R.string.statistics_count_label),
            value = totals.subscriptionCount.toString(),
            modifier = Modifier.weight(0.8f),
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SummaryCardsPreview() {
    GriffThemePreview {
        SummaryCards(
            totals = SubscriptionTotals(
                monthly = Money.ofUnits(286, 40),
                yearly = Money.ofUnits(3_436, 80),
                subscriptionCount = 12,
            ),
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
