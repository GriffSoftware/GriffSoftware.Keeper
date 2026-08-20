package com.griff.subscriptions.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import com.griff.subscriptions.presentation.theme.Spacing

/**
 * Normalized monthly and yearly cost of the currently displayed subscriptions.
 *
 * Used as the scaffold's bottom bar so the summary stays visible no matter how long the list is;
 * the tonal surface and the divider keep it readable while rows scroll underneath.
 */
@Composable
internal fun SubscriptionTotalsBar(
    totals: SubscriptionTotals,
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = BarTonalElevation,
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
                ) {
                    Text(
                        text = stringResource(
                            if (isFiltered) {
                                R.string.home_totals_filtered_title
                            } else {
                                R.string.home_totals_title
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.amount_per_month,
                            MoneyFormatter.format(totals.monthly),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                Text(
                    text = stringResource(
                        R.string.amount_per_year,
                        MoneyFormatter.format(totals.yearly),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.Medium),
                )
            }
        }
    }
}

private val BarTonalElevation = 3.dp

@Preview(showBackground = true)
@Composable
private fun SubscriptionTotalsBarPreview() {
    GriffSubscriptionsTheme(dynamicColor = false) {
        SubscriptionTotalsBar(
            totals = SubscriptionTotals(
                monthly = Money.ofUnits(284, 87),
                yearly = Money.ofUnits(3_418, 44),
                subscriptionCount = 9,
            ),
            isFiltered = false,
        )
    }
}
