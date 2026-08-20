package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionTotals
import java.time.LocalDate
import java.time.YearMonth

/** Time window the statistics screen is looking at. */
enum class StatisticsPeriod {
    /** The current calendar month. */
    MONTH,

    /** From the current month until the end of the current calendar year. */
    YEAR,

    /** A rolling window of twelve months starting with the current one. */
    TWELVE_MONTHS,
}

/** Projected charges falling into a single month. */
data class MonthlyCharges(
    val month: YearMonth,
    val amount: Money,
)

/** A single charge expected on a concrete date. */
data class ProjectedCharge(
    val subscription: Subscription,
    val date: LocalDate,
    val amount: Money,
)

/** Normalized cost of all subscriptions of one category. */
data class CategorySpending(
    val category: ProviderCategory,
    val monthly: Money,
    val yearly: Money,
    val subscriptionCount: Int,
    /** Share of the monthly total in the range `0f..1f`. */
    val share: Float,
)

/**
 * Everything the statistics screen needs.
 *
 * The model deliberately separates the always-available normalized cost ([totals]) from the
 * forecast of real charges ([forecast]), which only covers subscriptions with a known
 * `nextBillingDate`. [subscriptionsWithoutBillingDate] tells the UI how much of the cost cannot be
 * placed on the calendar, so the app never pretends to know a charge date it was not given.
 */
data class SubscriptionStatistics(
    val period: StatisticsPeriod,
    val totals: SubscriptionTotals,
    val forecast: List<MonthlyCharges>,
    val upcomingCharges: List<ProjectedCharge>,
    val unscheduledMonthlyCost: Money,
    val subscriptionsWithoutBillingDate: Int,
    val categories: List<CategorySpending>,
    val topSubscriptions: List<Subscription>,
) {
    val hasForecastData: Boolean get() = forecast.any { !it.amount.isZero }

    companion object {
        fun empty(period: StatisticsPeriod) = SubscriptionStatistics(
            period = period,
            totals = SubscriptionTotals.Empty,
            forecast = emptyList(),
            upcomingCharges = emptyList(),
            unscheduledMonthlyCost = Money.ZERO,
            subscriptionsWithoutBillingDate = 0,
            categories = emptyList(),
            topSubscriptions = emptyList(),
        )
    }
}

/** Resolves the category of a subscription's provider. */
fun interface ProviderCategoryResolver {
    fun categoryOf(providerId: ProviderId): ProviderCategory
}
