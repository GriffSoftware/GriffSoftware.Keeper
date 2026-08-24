package com.griff.keeper.presentation.subscription.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.em
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.GriffHeroCard
import com.griff.keeper.presentation.common.component.HeroStatTile
import com.griff.keeper.presentation.common.format.DateFormatter
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.subscription.SubscriptionListItem
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews
import java.time.LocalDate

/**
 * Gradient hero card summarizing the currently displayed subscriptions.
 *
 * The normalized monthly total leads, the same figure seen yearly sits underneath in a quieter
 * style, and two tiles ground it: how many subscriptions the total is drawn from, and when the next
 * one renews. Moved from a pinned bottom bar to the top of the list in the redesign, so the number
 * the screen exists for is the first thing seen rather than something scrolled past to reach.
 */
@Composable
fun SubscriptionTotalsBar(
    totals: SubscriptionTotals,
    isFiltered: Boolean,
    totalSubscriptionCount: Int,
    items: List<SubscriptionListItem>,
    modifier: Modifier = Modifier,
) {
    val nextRenewal = items.mapNotNull { it.nextBillingDate }.minOrNull()

    GriffHeroCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2)) {
            Text(
                text = stringResource(
                    if (isFiltered) {
                        R.string.subscriptions_totals_filtered_title
                    } else {
                        R.string.subscriptions_totals_title
                    },
                ),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.06.em),
                color = GriffGradients.OnAccent.copy(alpha = 0.82f),
            )
            Text(
                text = MoneyFormatter.format(totals.monthly),
                style = MaterialTheme.typography.displaySmall,
                color = GriffGradients.OnAccent,
            )
            Text(
                text = stringResource(R.string.amount_per_year, MoneyFormatter.format(totals.yearly)),
                style = MaterialTheme.typography.bodyMedium,
                color = GriffGradients.OnAccent.copy(alpha = 0.85f),
            )

            Row(
                modifier = Modifier.padding(top = Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                HeroStatTile(
                    label = stringResource(R.string.subscriptions_totals_active_label),
                    value = totalSubscriptionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                if (nextRenewal != null) {
                    HeroStatTile(
                        label = stringResource(R.string.subscriptions_next_renewal_label),
                        value = DateFormatter.formatDayAndMonth(nextRenewal),
                        modifier = Modifier.weight(1.5f),
                    )
                }
            }
        }
    }
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
            totalSubscriptionCount = 9,
            items = listOf(
                SubscriptionListItem(
                    id = "1",
                    name = "Spotify",
                    logoKey = "spotify",
                    category = ProviderCategory.MUSIC,
                    billingPeriod = BillingPeriod.MONTHLY,
                    price = Money.ofUnits(34, 99),
                    nextBillingDate = LocalDate.now().plusDays(12),
                ),
            ),
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
