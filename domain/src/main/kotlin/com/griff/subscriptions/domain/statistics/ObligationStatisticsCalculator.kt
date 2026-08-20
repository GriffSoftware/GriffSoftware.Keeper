package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.calculation.ObligationCostCalculator
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.sum

/**
 * Builds [ObligationStatistics] for a calendar window.
 *
 * Pure by design: the window is passed in, so every scenario is reproducible in tests. Amounts are
 * never spread over months - a yearly policy paid in March counts once, in March.
 */
object ObligationStatisticsCalculator {

    fun calculate(
        obligations: List<Obligation>,
        period: ExpensePeriod,
    ): ObligationStatistics {
        if (obligations.isEmpty()) return ObligationStatistics.empty(period)

        val paid = ObligationCostCalculator.paidIn(obligations, period)
        val totals = ObligationCostCalculator.totals(obligations, period)

        return ObligationStatistics(
            period = period,
            totals = totals,
            monthlyPaid = ObligationCostCalculator.paidPerMonth(obligations, period)
                .map { (month, amount) ->
                    MonthlyExpense(
                        month = month,
                        estimatedSubscriptions = Money.ZERO,
                        paidObligations = amount,
                    )
                },
            tags = tagSpending(paid, totals.paid),
            topObligations = paid.sortedWith(
                compareByDescending<Obligation> { it.amount }.thenBy { it.name.value },
            ),
        )
    }

    private fun tagSpending(paid: List<Obligation>, paidTotal: Money): List<TagSpending> =
        paid.groupBy { it.tag }
            .map { (tag, items) ->
                val amount = items.map { it.amount }.sum()
                TagSpending(
                    tag = tag,
                    paid = amount,
                    count = items.size,
                    share = amount.shareOf(paidTotal),
                )
            }
            .sortedWith(compareByDescending<TagSpending> { it.paid }.thenBy { it.tag.name })
}
