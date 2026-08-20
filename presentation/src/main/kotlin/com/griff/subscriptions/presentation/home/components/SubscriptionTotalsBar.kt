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
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.format.MoneyFormatter
import com.griff.subscriptions.presentation.theme.GriffThemePreview
import com.griff.subscriptions.presentation.theme.Spacing
import com.griff.subscriptions.presentation.theme.ThemePreviews

/**
 * Normalized monthly and yearly cost of the currently displayed subscriptions.
 *
 * Used as the scaffold's bottom bar so the summary stays visible no matter how long the list is; a
 * neutral container plus the divider keep it readable while rows scroll underneath. The monthly
 * figure is the one number the screen exists for, so it is the only place here that gets the accent.
 */
@Composable
internal fun SubscriptionTotalsBar(
    totals: SubscriptionTotals,
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
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
                        color = MaterialTheme.colorScheme.primary,
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
        )
    }
}
