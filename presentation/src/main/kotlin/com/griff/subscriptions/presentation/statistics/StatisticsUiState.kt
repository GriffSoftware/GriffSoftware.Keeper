package com.griff.subscriptions.presentation.statistics

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.presentation.common.UiMessage
import java.time.LocalDate
import java.time.YearMonth

/** One bar of the forecast chart. */
data class ForecastBar(
    val month: YearMonth,
    val amount: Money,
)

/** Cost share of a single category. */
data class CategoryShare(
    val category: ProviderCategory,
    val monthly: Money,
    val share: Float,
    val colorIndex: Int,
)

/** A subscription in the "largest costs" ranking. */
data class RankedSubscription(
    val id: String,
    val name: String,
    val logoKey: String,
    val billingPeriod: BillingPeriod,
    val monthlyEquivalent: Money,
)

/** A single expected charge. [isDueSoon] drives the warning marker on the row. */
data class UpcomingCharge(
    val subscriptionId: String,
    val name: String,
    val logoKey: String,
    val date: LocalDate,
    val amount: Money,
    val isDueSoon: Boolean = false,
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val period: StatisticsPeriod = StatisticsPeriod.TWELVE_MONTHS,
    val totals: SubscriptionTotals = SubscriptionTotals.Empty,
    val forecast: List<ForecastBar> = emptyList(),
    val upcomingCharges: List<UpcomingCharge> = emptyList(),
    val unscheduledMonthlyCost: Money = Money.ZERO,
    val subscriptionsWithoutBillingDate: Int = 0,
    val categories: List<CategoryShare> = emptyList(),
    val topSubscriptions: List<RankedSubscription> = emptyList(),
    val message: UiMessage? = null,
) {
    val isEmpty: Boolean get() = !isLoading && totals.subscriptionCount == 0

    val hasForecast: Boolean get() = forecast.any { !it.amount.isZero }
}
