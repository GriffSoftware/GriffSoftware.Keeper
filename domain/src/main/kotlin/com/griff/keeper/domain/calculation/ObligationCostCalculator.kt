package com.griff.keeper.domain.calculation

import com.griff.keeper.domain.model.ExpensePeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationTotals
import com.griff.keeper.domain.model.sum
import java.time.YearMonth

/**
 * Aggregates real obligation payments.
 *
 * Unlike subscriptions, obligations are never normalized: a yearly car insurance paid in March is a
 * March expense of its full amount, not a twelfth of it in every month. Every figure here is
 * therefore derived from [Obligation.paymentDate] and nothing else.
 */
object ObligationCostCalculator {

    /** Records actually settled inside [period], by payment date. */
    fun paidIn(obligations: Collection<Obligation>, period: ExpensePeriod): List<Obligation> =
        obligations.filter { obligation ->
            obligation.paymentDate?.let(period::contains) == true
        }

    /** Money that left the account inside [period]. */
    fun paidTotal(obligations: Collection<Obligation>, period: ExpensePeriod): Money =
        paidIn(obligations, period).map { it.amount }.sum()

    /**
     * Totals for the records the screen currently shows.
     *
     * [period] decides which payments count as "paid in this period"; unpaid records are summed as
     * they are, because an open charge has no payment date to filter on.
     */
    fun totals(obligations: Collection<Obligation>, period: ExpensePeriod): ObligationTotals {
        val paid = paidIn(obligations, period)
        val outstanding = obligations.filterNot { it.isPaid }
        return ObligationTotals(
            paid = paid.map { it.amount }.sum(),
            outstanding = outstanding.map { it.amount }.sum(),
            paidCount = paid.size,
            outstandingCount = outstanding.size,
            largestPaid = paid.maxOfOrNull { it.amount } ?: Money.ZERO,
        )
    }

    /**
     * Payments per month inside [period], zero for months without one.
     *
     * The map covers every month of the period so a chart can be drawn without gaps.
     */
    fun paidPerMonth(
        obligations: Collection<Obligation>,
        period: ExpensePeriod,
    ): Map<YearMonth, Money> {
        val byMonth = paidIn(obligations, period).groupBy { YearMonth.from(it.paymentDate) }
        return period.months.associateWith { month ->
            byMonth[month]?.map { it.amount }?.sum() ?: Money.ZERO
        }
    }
}
