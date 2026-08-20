package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.ObligationTotals
import java.time.YearMonth

/** Which part of the user's finances the statistics screen is looking at. */
enum class StatisticsScope {
    ALL,
    SUBSCRIPTIONS,
    OBLIGATIONS,
}

/**
 * Where an amount comes from.
 *
 * Kept next to every combined figure because the two sources are not the same kind of number: a
 * subscription cost is a normalized estimate, an obligation payment is a settled transaction.
 */
enum class ExpenseSource {
    SUBSCRIPTION,
    OBLIGATION,
}

/** Cost of one obligation tag inside the analysed period. */
data class TagSpending(
    val tag: ObligationTag,
    val paid: Money,
    val count: Int,
    /** Share of the paid total in the range `0f..1f`. */
    val share: Float,
)

/** A single row of the "largest costs" ranking, with the source it came from. */
data class RankedExpense(
    val id: String,
    val name: String,
    val amount: Money,
    val source: ExpenseSource,
)

/** Actual and estimated cost of one month, kept apart so a chart never mixes them up. */
data class MonthlyExpense(
    val month: YearMonth,
    /** Normalized subscription cost attributed to the month - an estimate, not a transaction. */
    val estimatedSubscriptions: Money,
    /** Obligation payments that really happened in the month. */
    val paidObligations: Money,
) {
    val total: Money get() = estimatedSubscriptions + paidObligations
}

/**
 * Statistics of real obligation payments.
 *
 * Everything is derived from payment dates inside [period]; [outstanding] is the only forward
 * looking number and is reported separately from what was actually paid.
 */
data class ObligationStatistics(
    val period: ExpensePeriod,
    val totals: ObligationTotals,
    val monthlyPaid: List<MonthlyExpense>,
    val tags: List<TagSpending>,
    val topObligations: List<Obligation>,
) {
    val hasPayments: Boolean get() = totals.paidCount > 0

    companion object {
        fun empty(period: ExpensePeriod) = ObligationStatistics(
            period = period,
            totals = ObligationTotals.Empty,
            monthlyPaid = emptyList(),
            tags = emptyList(),
            topObligations = emptyList(),
        )
    }
}

/**
 * Everything the statistics screen needs, for whichever [scope] is selected.
 *
 * [subscriptions] and [obligations] stay separate models on purpose: the two domains are different
 * concepts and merging them into one "expense" type would lose the distinction between an estimated
 * recurring cost and a payment that actually happened. Only the combined *presentation* figures
 * ([estimatedSubscriptionCost], [paidObligationCost], [combinedTotal]) live side by side, and each
 * of them keeps its meaning in its name.
 */
data class FinanceStatistics(
    val scope: StatisticsScope,
    val period: StatisticsPeriod,
    val window: ExpensePeriod,
    val subscriptions: SubscriptionStatistics?,
    val obligations: ObligationStatistics?,
    /** Normalized subscription cost for the whole [window]; an estimate. */
    val estimatedSubscriptionCost: Money,
    /** Obligation payments settled inside [window]; a fact. */
    val paidObligationCost: Money,
    val monthlyExpenses: List<MonthlyExpense>,
    val topExpenses: List<RankedExpense>,
) {
    /**
     * Estimate plus payments.
     *
     * Only meaningful with a label that says one half is estimated, which is why the UI never shows
     * it on its own.
     */
    val combinedTotal: Money get() = estimatedSubscriptionCost + paidObligationCost

    val isEmpty: Boolean
        get() = when (scope) {
            StatisticsScope.SUBSCRIPTIONS -> subscriptions?.totals?.subscriptionCount == 0
            StatisticsScope.OBLIGATIONS -> obligations?.totals?.count == 0
            StatisticsScope.ALL ->
                subscriptions?.totals?.subscriptionCount == 0 && obligations?.totals?.count == 0
        }
}
