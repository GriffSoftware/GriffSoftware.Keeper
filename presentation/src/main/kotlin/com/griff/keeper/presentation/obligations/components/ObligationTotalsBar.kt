package com.griff.keeper.presentation.obligations.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.griff.keeper.domain.model.ExpensePeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationTotals
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.component.SummaryBar
import com.griff.keeper.presentation.common.format.MoneyFormatter
import com.griff.keeper.presentation.common.format.PeriodFormatter
import com.griff.keeper.presentation.theme.GriffThemePreview
import com.griff.keeper.presentation.theme.ThemePreviews

/**
 * What was actually paid in the selected period, plus what is still open.
 *
 * The headline figure counts only records whose payment date falls inside the period - a policy that
 * merely *expires* this year is not this year's expense. The outstanding amount sits beside it as a
 * separate number so a plan is never added to history.
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

    SummaryBar(
        title = if (isNarrowed) {
            stringResource(R.string.obligations_paid_filtered_suffix, periodTitle)
        } else {
            periodTitle
        },
        amount = MoneyFormatter.format(totals.paid),
        secondaryTitle = stringResource(R.string.obligations_outstanding_label)
            .takeIf { !totals.outstanding.isZero },
        secondaryAmount = MoneyFormatter.format(totals.outstanding)
            .takeIf { !totals.outstanding.isZero },
        modifier = modifier,
    )
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
        )
    }
}
