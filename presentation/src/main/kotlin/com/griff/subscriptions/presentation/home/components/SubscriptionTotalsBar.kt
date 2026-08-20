package com.griff.subscriptions.presentation.home.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.component.SummaryBar
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.ThemePreviews

/**
 * Normalized monthly and yearly cost of the currently displayed subscriptions.
 *
 * The monthly figure is what the screen exists for and therefore leads; the yearly one is the same
 * cost seen from further away, so it sits next to it in a quieter style.
 */
@Composable
internal fun SubscriptionTotalsBar(
    totals: SubscriptionTotals,
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    SummaryBar(
        title = stringResource(
            if (isFiltered) R.string.home_totals_filtered_title else R.string.home_totals_title,
        ),
        amount = stringResource(
            R.string.amount_per_month,
            MoneyFormatter.format(totals.monthly),
        ),
        secondaryAmount = stringResource(
            R.string.amount_per_year,
            MoneyFormatter.format(totals.yearly),
        ),
        modifier = modifier,
    )
}

@ThemePreviews
@Composable
private fun SubscriptionTotalsBarPreview() {
    GriffThemePreview {
        SubscriptionTotalsBar(
            totals = SubscriptionTotals(
                monthly = Money.ofUnits(284, 87),
                yearly = Money.ofUnits(3_418, 44),
                subscriptionCount = 9,
            ),
            isFiltered = false,
            modifier = Modifier.padding(),
        )
    }
}
