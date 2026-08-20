package com.griff.subscriptions.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import com.griff.subscriptions.presentation.theme.Spacing

/** Normalized monthly and yearly cost of the currently displayed subscriptions. */
@Composable
internal fun SubscriptionTotalsSection(
    totals: SubscriptionTotals,
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(
            text = stringResource(
                if (isFiltered) R.string.home_totals_filtered_title else R.string.home_totals_title,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.amount_per_month, MoneyFormatter.format(totals.monthly)),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.amount_per_year, MoneyFormatter.format(totals.yearly)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionTotalsSectionPreview() {
    GriffSubscriptionsTheme(dynamicColor = false) {
        SubscriptionTotalsSection(
            totals = SubscriptionTotals(
                monthly = Money.ofUnits(286, 40),
                yearly = Money.ofUnits(3_436, 80),
                subscriptionCount = 12,
            ),
            isFiltered = false,
        )
    }
}
