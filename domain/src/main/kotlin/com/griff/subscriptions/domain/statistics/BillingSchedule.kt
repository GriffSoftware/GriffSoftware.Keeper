package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.model.Subscription
import java.time.LocalDate
import java.time.YearMonth

/**
 * Derives concrete charge dates from a subscription's billing period and next billing date.
 *
 * Occurrences are always computed as `anchor + n * period` so the day of the month never drifts:
 * a subscription billed on the 31st is charged on the 28th of February but on the 31st of March
 * again. A stored `nextBillingDate` may be in the past when the user has not opened the app for a
 * while, so occurrences are rolled forward instead of being trusted blindly.
 */
object BillingSchedule {

    fun occurrences(
        subscription: Subscription,
        from: LocalDate,
        toInclusive: LocalDate,
    ): List<LocalDate> {
        val anchor = subscription.nextBillingDate ?: return emptyList()
        val step = subscription.billingPeriod.monthsPerPeriod.toLong()

        val result = mutableListOf<LocalDate>()
        var index = periodsUntil(anchor, from, step)
        var current = anchor.plusMonths(index * step)
        while (!current.isAfter(toInclusive)) {
            if (!current.isBefore(from)) result += current
            index++
            current = anchor.plusMonths(index * step)
        }
        return result
    }

    fun nextOccurrenceOnOrAfter(subscription: Subscription, from: LocalDate): LocalDate? {
        val anchor = subscription.nextBillingDate ?: return null
        val step = subscription.billingPeriod.monthsPerPeriod.toLong()
        return anchor.plusMonths(periodsUntil(anchor, from, step) * step)
    }

    /** Number of whole periods that have to be added to [anchor] to reach [from] or later. */
    private fun periodsUntil(anchor: LocalDate, from: LocalDate, step: Long): Long {
        if (!anchor.isBefore(from)) return 0
        var periods = 0L
        while (anchor.plusMonths(periods * step).isBefore(from)) {
            periods++
        }
        return periods
    }
}

internal fun YearMonth.firstDay(): LocalDate = atDay(1)

internal fun YearMonth.lastDay(): LocalDate = atEndOfMonth()
