package com.griff.subscriptions.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.statistics.GetFinanceStatisticsUseCase
import com.griff.subscriptions.application.statistics.StatisticsSelection
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.statistics.ExpenseSource
import com.griff.subscriptions.domain.statistics.FinanceStatistics
import com.griff.subscriptions.domain.statistics.ObligationStatistics
import com.griff.subscriptions.domain.statistics.RankedExpense
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.domain.statistics.StatisticsScope
import com.griff.subscriptions.domain.statistics.SubscriptionStatistics
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.common.MessageSeverity
import com.griff.subscriptions.presentation.common.Tags
import com.griff.subscriptions.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    getStatistics: GetFinanceStatisticsUseCase,
    private val getProvider: GetProviderUseCase,
    private val clock: ClockProvider,
) : ViewModel() {

    /** Scope and period in one value, so any combination of the two is a single recomputation. */
    private val selection = MutableStateFlow(StatisticsSelection())

    val uiState: StateFlow<StatisticsUiState> = getStatistics(selection.asStateFlow())
        .map { statistics -> statistics.toUiState() }
        .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(
                StatisticsUiState(
                    isLoading = false,
                    scope = selection.value.scope,
                    period = selection.value.period,
                    message = UiMessage(
                        R.string.error_load_failed,
                        severity = MessageSeverity.ERROR,
                    ),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StatisticsUiState(),
        )

    fun onScopeChange(value: StatisticsScope) = selection.update { it.copy(scope = value) }

    fun onPeriodChange(value: StatisticsPeriod) = selection.update { it.copy(period = value) }

    private fun FinanceStatistics.toUiState() = StatisticsUiState(
        isLoading = false,
        scope = scope,
        period = period,
        window = window,
        subscriptions = subscriptions?.toUi(),
        obligations = obligations?.toUi(),
        combined = if (scope == StatisticsScope.ALL) {
            CombinedStatisticsUi(
                estimatedSubscriptions = estimatedSubscriptionCost,
                paidObligations = paidObligationCost,
                total = combinedTotal,
                months = monthlyExpenses.map {
                    ExpenseBar(
                        month = it.month,
                        subscriptions = it.estimatedSubscriptions,
                        obligations = it.paidObligations,
                    )
                },
                topExpenses = topExpenses.take(TOP_EXPENSES_LIMIT).map { it.toUi() },
            )
        } else {
            null
        },
        isEmpty = isEmpty,
    )

    private fun SubscriptionStatistics.toUi(): SubscriptionStatisticsUi {
        val dueSoonUntil = clock.today().plusDays(DUE_SOON_DAYS)
        return SubscriptionStatisticsUi(
            totals = totals,
            forecast = forecast.map { ForecastBar(month = it.month, amount = it.amount) },
            upcomingCharges = upcomingCharges.take(UPCOMING_CHARGES_LIMIT).map { charge ->
                UpcomingCharge(
                    subscriptionId = charge.subscription.id.value,
                    name = charge.subscription.name.value,
                    logoKey = charge.subscription.logoKey(),
                    date = charge.date,
                    amount = charge.amount,
                    isDueSoon = !charge.date.isAfter(dueSoonUntil),
                )
            },
            unscheduledMonthlyCost = unscheduledMonthlyCost,
            subscriptionsWithoutBillingDate = subscriptionsWithoutBillingDate,
            categories = categories.map { spending ->
                SpendingShare(
                    style = Tags.of(spending.category),
                    amount = spending.monthly,
                    share = spending.share,
                )
            },
            topSubscriptions = topSubscriptions.take(TOP_EXPENSES_LIMIT).map { subscription ->
                RankedSubscription(
                    id = subscription.id.value,
                    name = subscription.name.value,
                    logoKey = subscription.logoKey(),
                    billingPeriod = subscription.billingPeriod,
                    monthlyEquivalent = subscription.monthlyEquivalent,
                )
            },
        )
    }

    private fun ObligationStatistics.toUi() = ObligationStatisticsUi(
        totals = totals,
        payments = monthlyPaid.map {
            ExpenseBar(
                month = it.month,
                subscriptions = Money.ZERO,
                obligations = it.paidObligations,
            )
        },
        tags = tags.map { spending ->
            SpendingShare(
                style = Tags.of(spending.tag),
                amount = spending.paid,
                share = spending.share,
            )
        },
        topObligations = topObligations.take(TOP_EXPENSES_LIMIT).map { obligation ->
            RankedExpenseItem(
                id = obligation.id.value,
                name = obligation.name.value,
                amount = obligation.amount,
                source = ExpenseSource.OBLIGATION,
                sourceLabelRes = R.string.statistics_source_obligation,
            )
        },
    )

    private fun RankedExpense.toUi() = RankedExpenseItem(
        id = id,
        name = name,
        amount = amount,
        source = source,
        sourceLabelRes = when (source) {
            ExpenseSource.SUBSCRIPTION -> R.string.statistics_source_subscription
            ExpenseSource.OBLIGATION -> R.string.statistics_source_obligation
        },
    )

    private fun Subscription.logoKey(): String {
        val provider = getProvider(providerId)
        return if (provider.isOther) name.value else provider.logoKey
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TOP_EXPENSES_LIMIT = 5
        const val UPCOMING_CHARGES_LIMIT = 5

        /** A charge within a week is close enough that the user may still want to react to it. */
        const val DUE_SOON_DAYS = 7L
    }
}
