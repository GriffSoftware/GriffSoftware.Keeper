package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.calculation.SubscriptionCostCalculator
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ProviderCategoryResolver
import com.griff.subscriptions.domain.model.Subscription
import java.time.LocalDate

/**
 * Combines the two domains into what the statistics screen shows.
 *
 * The calculator aggregates; it does not merge. Subscriptions keep producing a normalized,
 * predictable cost and obligations keep producing settled payments, and the combined figures carry
 * that distinction in their names. Nothing here turns an estimate into history:
 *
 * - a subscription's contribution to a month is its normalized monthly cost, clearly an estimate,
 *   because a stored renewal date says nothing about what was charged in a month that has passed;
 * - an obligation's contribution is the payment that really happened in that month.
 */
class FinanceStatisticsCalculator(
    private val categoryResolver: ProviderCategoryResolver,
) {
    private val subscriptionCalculator = SubscriptionStatisticsCalculator(categoryResolver)

    fun calculate(
        subscriptions: List<Subscription>,
        obligations: List<Obligation>,
        scope: StatisticsScope,
        period: StatisticsPeriod,
        today: LocalDate,
    ): FinanceStatistics {
        val window = period.window(today)
        val includeSubscriptions = scope != StatisticsScope.OBLIGATIONS
        val includeObligations = scope != StatisticsScope.SUBSCRIPTIONS

        val subscriptionStatistics = if (includeSubscriptions) {
            subscriptionCalculator.calculate(subscriptions, period, today)
        } else {
            null
        }
        val obligationStatistics = if (includeObligations) {
            ObligationStatisticsCalculator.calculate(obligations, window)
        } else {
            null
        }

        val monthlySubscriptionEstimate = if (includeSubscriptions) {
            SubscriptionCostCalculator.monthlyTotal(subscriptions)
        } else {
            Money.ZERO
        }
        val estimatedSubscriptionCost = when {
            !includeSubscriptions -> Money.ZERO
            window is ExpensePeriod.Year -> SubscriptionCostCalculator.yearlyTotal(subscriptions)
            else -> monthlySubscriptionEstimate * window.months.size
        }

        return FinanceStatistics(
            scope = scope,
            period = period,
            window = window,
            subscriptions = subscriptionStatistics,
            obligations = obligationStatistics,
            estimatedSubscriptionCost = estimatedSubscriptionCost,
            paidObligationCost = obligationStatistics?.totals?.paid ?: Money.ZERO,
            monthlyExpenses = window.months.map { month ->
                MonthlyExpense(
                    month = month,
                    estimatedSubscriptions = monthlySubscriptionEstimate,
                    paidObligations = obligationStatistics
                        ?.monthlyPaid
                        ?.firstOrNull { it.month == month }
                        ?.paidObligations
                        ?: Money.ZERO,
                )
            },
            topExpenses = topExpenses(subscriptionStatistics, obligationStatistics),
        )
    }

    /**
     * The largest costs, comparable across sources.
     *
     * Subscriptions are ranked by their monthly cost and obligations by the amount actually paid in
     * the window; each row keeps its [ExpenseSource] so the UI can say which is which instead of
     * implying the two numbers mean the same thing.
     */
    private fun topExpenses(
        subscriptions: SubscriptionStatistics?,
        obligations: ObligationStatistics?,
    ): List<RankedExpense> {
        val fromSubscriptions = subscriptions?.topSubscriptions.orEmpty().map { subscription ->
            RankedExpense(
                id = subscription.id.value,
                name = subscription.name.value,
                amount = subscription.monthlyEquivalent,
                source = ExpenseSource.SUBSCRIPTION,
            )
        }
        val fromObligations = obligations?.topObligations.orEmpty().map { obligation ->
            RankedExpense(
                id = obligation.id.value,
                name = obligation.name.value,
                amount = obligation.amount,
                source = ExpenseSource.OBLIGATION,
            )
        }
        return (fromSubscriptions + fromObligations)
            .sortedWith(compareByDescending<RankedExpense> { it.amount }.thenBy { it.name })
    }
}

/**
 * The calendar window a statistics period covers.
 *
 * Note that this is *not* the same window the subscription forecast uses: a forecast can only look
 * forward from today, while actual expenses are counted over the whole calendar month or year the
 * user selected.
 */
fun StatisticsPeriod.window(today: LocalDate): ExpensePeriod = when (this) {
    StatisticsPeriod.MONTH -> ExpensePeriod.currentMonth(today)
    StatisticsPeriod.YEAR -> ExpensePeriod.currentYear(today)
    StatisticsPeriod.TWELVE_MONTHS -> ExpensePeriod.trailingYear(today)
}
