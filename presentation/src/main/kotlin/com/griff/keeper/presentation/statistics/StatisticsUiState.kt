package com.griff.keeper.presentation.statistics

import androidx.annotation.StringRes
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.ExpensePeriod
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationTotals
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.domain.statistics.ExpenseSource
import com.griff.keeper.domain.statistics.StatisticsPeriod
import com.griff.keeper.domain.statistics.StatisticsScope
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.common.component.TagStyle
import java.time.LocalDate
import java.time.YearMonth

/** One bar of the forecast chart. */
data class ForecastBar(
    val month: YearMonth,
    val amount: Money,
)

/**
 * One month of the combined chart.
 *
 * The two series are kept apart all the way into the UI model, because one is an estimate and the
 * other a settled payment; a single `total` field would lose that.
 */
data class ExpenseBar(
    val month: YearMonth,
    val subscriptions: Money,
    val obligations: Money,
)

/**
 * A row of a "cost per category" breakdown.
 *
 * The label is a resource reference and the accent comes from the shared tag palette, so a category
 * has the same color in its badge, its filter chip and its bar.
 */
data class SpendingShare(
    val style: TagStyle,
    val amount: Money,
    val share: Float,
)

/** A subscription in the "largest costs" ranking. */
data class RankedSubscription(
    val id: String,
    val name: String,
    val logoKey: String,
    val billingPeriod: BillingPeriod,
    val monthlyEquivalent: Money,
)

/** A row of the combined "largest costs" list, labelled with where the amount comes from. */
data class RankedExpenseItem(
    val id: String,
    val name: String,
    val amount: Money,
    val source: ExpenseSource,
    @param:StringRes val sourceLabelRes: Int,
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

/** Subscription-only figures, unchanged in meaning from before the obligations feature. */
data class SubscriptionStatisticsUi(
    val totals: SubscriptionTotals = SubscriptionTotals.Empty,
    val forecast: List<ForecastBar> = emptyList(),
    val upcomingCharges: List<UpcomingCharge> = emptyList(),
    val unscheduledMonthlyCost: Money = Money.ZERO,
    val subscriptionsWithoutBillingDate: Int = 0,
    val categories: List<SpendingShare> = emptyList(),
    val topSubscriptions: List<RankedSubscription> = emptyList(),
) {
    val hasForecast: Boolean get() = forecast.any { !it.amount.isZero }
}

/** Obligation-only figures: what was really paid inside the selected window. */
data class ObligationStatisticsUi(
    val totals: ObligationTotals = ObligationTotals.Empty,
    val payments: List<ExpenseBar> = emptyList(),
    val tags: List<SpendingShare> = emptyList(),
    val topObligations: List<RankedExpenseItem> = emptyList(),
) {
    val hasPayments: Boolean get() = totals.paidCount > 0
}

/** Figures that only make sense when both sources are shown together. */
data class CombinedStatisticsUi(
    val estimatedSubscriptions: Money = Money.ZERO,
    val paidObligations: Money = Money.ZERO,
    val total: Money = Money.ZERO,
    val months: List<ExpenseBar> = emptyList(),
    val topExpenses: List<RankedExpenseItem> = emptyList(),
) {
    val hasChartData: Boolean
        get() = months.any { !it.subscriptions.isZero || !it.obligations.isZero }
}

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val scope: StatisticsScope = StatisticsScope.ALL,
    val period: StatisticsPeriod = StatisticsPeriod.YEAR,
    val window: ExpensePeriod? = null,
    val subscriptions: SubscriptionStatisticsUi? = null,
    val obligations: ObligationStatisticsUi? = null,
    val combined: CombinedStatisticsUi? = null,
    val isEmpty: Boolean = false,
    val message: UiMessage? = null,
)
