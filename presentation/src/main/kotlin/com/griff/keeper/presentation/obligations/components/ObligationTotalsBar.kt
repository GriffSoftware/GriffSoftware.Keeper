package com.griff.keeper.presentation.obligations.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.griff.keeper.domain.model.ExpensePeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationTotals
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.GriffHeroCard
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.common.format.PeriodFormatter
import com.griff.keeper.presentation.theme.GriffGradients
import com.griff.keeper.presentation.theme.GriffShapes
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.Spacing
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * What was actually paid in the selected period, plus what is still open.
 *
 * The headline figure counts only records whose payment date falls inside the period - a policy that
 * merely *expires* this year is not this year's expense. The proportion bar reuses the same two
 * figures rather than a category breakdown the view model does not compute: it reads as "how much of
 * this period's obligations are settled".
 */
@Composable
internal fun ObligationTotalsBar(
    period: ExpensePeriod,
    totals: ObligationTotals,
    isNarrowed: Boolean,
    modifier: Modifier = Modifier,
) {
    val periodTitle = when (period) {
        is ExpensePeriod.Year -> stringResource(R.string.obligations_paid_in_year, period.year)

        // "Zapłacono w sierpniu 2026" needs the locative case, which no date formatter produces.
        is ExpensePeriod.Month -> stringResource(
            R.string.obligations_paid_in_month,
            stringArrayResource(R.array.months_locative)[period.yearMonth.monthValue - 1],
            period.yearMonth.year,
        )

        is ExpensePeriod.Range ->
            stringResource(R.string.obligations_paid_in_range, PeriodFormatter.format(period))
    }

    val total = totals.paid + totals.outstanding
    val paidShare = totals.paid.shareOf(total)

    GriffHeroCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2)) {
                    Text(
                        text = if (isNarrowed) {
                            stringResource(R.string.obligations_paid_filtered_suffix, periodTitle)
                        } else {
                            periodTitle
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = GriffGradients.OnAccent.copy(alpha = 0.82f),
                    )
                    Text(
                        text = MoneyFormatter.format(totals.paid),
                        style = MaterialTheme.typography.displaySmall,
                        color = GriffGradients.OnAccent,
                    )
                }

                if (!totals.outstanding.isZero) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
                    ) {
                        Text(
                            text = stringResource(R.string.obligations_outstanding_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = GriffGradients.OnAccent.copy(alpha = 0.8f),
                        )
                        Text(
                            text = MoneyFormatter.format(totals.outstanding),
                            style = MaterialTheme.typography.titleMedium,
                            color = GriffGradients.OnAccent,
                        )
                    }
                }
            }

            if (!totals.outstanding.isZero) {
                Row(
                    modifier = Modifier
                        .padding(top = Spacing.Medium)
                        .fillMaxWidth()
                        .height(Spacing.ExtraSmall)
                        .clip(GriffShapes.Pill)
                        .background(GriffGradients.veil()),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(paidShare.coerceIn(0.02f, 1f))
                            .fillMaxWidth()
                            .background(GriffGradients.OnAccent.copy(alpha = 0.95f)),
                    )
                    if (paidShare < 1f) {
                        Box(
                            modifier = Modifier
                                .weight((1f - paidShare).coerceAtLeast(0.02f))
                                .fillMaxWidth()
                                .background(GriffGradients.OnAccent.copy(alpha = 0.5f)),
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ObligationTotalsBarPreview() {
    GriffThemePreview {
        ObligationTotalsBar(
            period = ExpensePeriod.Year(2026),
            totals = ObligationTotals(
                paid = Money.ofUnits(4_820),
                outstanding = Money.ofUnits(920),
                paidCount = 4,
                outstandingCount = 1,
                largestPaid = Money.ofUnits(1_420),
            ),
            isNarrowed = false,
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}
