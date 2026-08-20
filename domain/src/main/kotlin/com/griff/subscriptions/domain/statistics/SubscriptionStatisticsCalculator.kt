package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.calculation.SubscriptionCostCalculator
import com.griff.subscriptions.domain.model.ProviderCategoryResolver
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.categoryWith
import com.griff.subscriptions.domain.model.sum
import java.time.LocalDate
import java.time.YearMonth

/**
 * Builds [SubscriptionStatistics] for a given period.
 *
 * The calculator is pure: the current date is passed in, so every scenario is reproducible in
 * tests.
 */
class SubscriptionStatisticsCalculator(
    private val categoryResolver: ProviderCategoryResolver,
) {

    fun calculate(
        subscriptions: List<Subscription>,
        period: StatisticsPeriod,
        today: LocalDate,
    ): SubscriptionStatistics {
        if (subscriptions.isEmpty()) return SubscriptionStatistics.empty(period)

        val months = monthsInPeriod(period, YearMonth.from(today))
        val scheduled = subscriptions.filter { it.nextBillingDate != null }
        val unscheduled = subscriptions.filter { it.nextBillingDate == null }

        val chargesByMonth = months.associateWith { mutableListOf<ProjectedCharge>() }
        val windowStart = maxOf(today, months.first().firstDay())
        val windowEnd = months.last().lastDay()

        scheduled.forEach { subscription ->
            BillingSchedule.occurrences(subscription, windowStart, windowEnd).forEach { date ->
                chargesByMonth[YearMonth.from(date)]?.add(
                    ProjectedCharge(
                        subscription = subscription,
                        date = date,
                        amount = subscription.price,
                    ),
                )
            }
        }

        val forecast = months.map { month ->
            MonthlyCharges(
                month = month,
                amount = chargesByMonth.getValue(month).map { it.amount }.sum(),
            )
        }

        val upcomingCharges = chargesByMonth.values
            .flatten()
            .sortedWith(compareBy({ it.date }, { it.subscription.name.value }))

        return SubscriptionStatistics(
            period = period,
            totals = SubscriptionCostCalculator.totals(subscriptions),
            forecast = forecast,
            upcomingCharges = upcomingCharges,
            unscheduledMonthlyCost = unscheduled.map { it.monthlyEquivalent }.sum(),
            subscriptionsWithoutBillingDate = unscheduled.size,
            categories = categories(subscriptions),
            topSubscriptions = subscriptions.sortedWith(
                compareByDescending<Subscription> { it.monthlyEquivalent }
                    .thenBy { it.name.value },
            ),
        )
    }

    private fun categories(subscriptions: List<Subscription>): List<CategorySpending> {
        val monthlyTotal = SubscriptionCostCalculator.monthlyTotal(subscriptions)
        return subscriptions
            .groupBy { it.categoryWith(categoryResolver) }
            .map { (category, items) ->
                val monthly = items.map { it.monthlyEquivalent }.sum()
                CategorySpending(
                    category = category,
                    monthly = monthly,
                    yearly = items.map { it.yearlyEquivalent }.sum(),
                    subscriptionCount = items.size,
                    share = monthly.shareOf(monthlyTotal),
                )
            }
            .sortedWith(compareByDescending<CategorySpending> { it.monthly }.thenBy { it.category.name })
    }

    private fun monthsInPeriod(period: StatisticsPeriod, currentMonth: YearMonth): List<YearMonth> =
        when (period) {
            StatisticsPeriod.MONTH -> listOf(currentMonth)
            StatisticsPeriod.YEAR -> {
                val december = YearMonth.of(currentMonth.year, 12)
                monthsBetween(currentMonth, december)
            }

            StatisticsPeriod.TWELVE_MONTHS -> monthsBetween(
                from = currentMonth,
                toInclusive = currentMonth.plusMonths(MONTHS_IN_ROLLING_WINDOW - 1),
            )
        }

    private fun monthsBetween(from: YearMonth, toInclusive: YearMonth): List<YearMonth> {
        val result = mutableListOf<YearMonth>()
        var current = from
        while (!current.isAfter(toInclusive)) {
            result += current
            current = current.plusMonths(1)
        }
        return result
    }

    private companion object {
        const val MONTHS_IN_ROLLING_WINDOW = 12L
    }
}

