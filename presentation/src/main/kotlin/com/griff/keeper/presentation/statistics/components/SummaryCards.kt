package com.griff.keeper.presentation.statistics.components

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
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationTotals
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * Monthly cost, yearly cost and the number of subscriptions.
 *
 * The tiles stay neutral - a white (light) or graphite (dark) card with a hairline outline; only the
 * monthly cost, the number this scope is about, is printed in the accent color, so the row has a
 * single focal point instead of three.
 */
@Composable
internal fun SubscriptionSummaryCards(
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

/**
 * What was paid in the window, how many records it covers and what is still open.
 *
 * The amount already paid leads and takes the accent: it is the only figure here that describes
 * money that has actually moved.
 */
@Composable
internal fun ObligationSummaryCards(
    totals: ObligationTotals,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        SummaryCard(
            label = stringResource(R.string.statistics_obligations_paid_label),
            value = MoneyFormatter.format(totals.paid),
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1.3f),
        )
        SummaryCard(
            label = stringResource(R.string.statistics_obligations_count_label),
            value = totals.paidCount.toString(),
            modifier = Modifier.weight(0.7f),
        )
        SummaryCard(
            label = stringResource(R.string.statistics_obligations_outstanding_label),
            value = MoneyFormatter.format(totals.outstanding),
            modifier = Modifier.weight(1.3f),
        )
    }
}

@Composable
internal fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null,
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
            // The note is what keeps an estimate from being read as a transaction.
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SummaryCardsPreview() {
    GriffThemePreview {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            SubscriptionSummaryCards(
                totals = SubscriptionTotals(
                    monthly = Money.ofUnits(286, 40),
                    yearly = Money.ofUnits(3_436, 80),
                    subscriptionCount = 12,
                ),
            )
            ObligationSummaryCards(
                totals = ObligationTotals(
                    paid = Money.ofUnits(4_820),
                    outstanding = Money.ofUnits(920),
                    paidCount = 6,
                    outstandingCount = 1,
                    largestPaid = Money.ofUnits(1_420),
                ),
            )
        }
    }
}
