package com.griff.subscriptions.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.statistics.GetSubscriptionStatisticsUseCase
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.domain.statistics.SubscriptionStatistics
import com.griff.subscriptions.presentation.R
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

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    getStatistics: GetSubscriptionStatisticsUseCase,
    private val getProvider: GetProviderUseCase,
) : ViewModel() {

    private val period = MutableStateFlow(StatisticsPeriod.TWELVE_MONTHS)

    val uiState: StateFlow<StatisticsUiState> = getStatistics(period.asStateFlow())
        .map { statistics -> statistics.toUiState() }
        .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(
                StatisticsUiState(
                    isLoading = false,
                    period = period.value,
                    message = UiMessage(R.string.error_load_failed),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StatisticsUiState(),
        )

    fun onPeriodChange(value: StatisticsPeriod) {
        period.value = value
    }

    private fun SubscriptionStatistics.toUiState() = StatisticsUiState(
        isLoading = false,
        period = period,
        totals = totals,
        forecast = forecast.map { ForecastBar(month = it.month, amount = it.amount) },
        upcomingCharges = upcomingCharges.take(UPCOMING_CHARGES_LIMIT).map { charge ->
            UpcomingCharge(
                subscriptionId = charge.subscription.id.value,
                name = charge.subscription.name.value,
                logoKey = charge.subscription.logoKey(),
                date = charge.date,
                amount = charge.amount,
            )
        },
        unscheduledMonthlyCost = unscheduledMonthlyCost,
        subscriptionsWithoutBillingDate = subscriptionsWithoutBillingDate,
        categories = categories.mapIndexed { index, spending ->
            CategoryShare(
                category = spending.category,
                monthly = spending.monthly,
                share = spending.share,
                colorIndex = index,
            )
        },
        topSubscriptions = topSubscriptions.take(TOP_SUBSCRIPTIONS_LIMIT).map { subscription ->
            RankedSubscription(
                id = subscription.id.value,
                name = subscription.name.value,
                logoKey = subscription.logoKey(),
                billingPeriod = subscription.billingPeriod,
                monthlyEquivalent = subscription.monthlyEquivalent,
            )
        },
    )

    private fun Subscription.logoKey(): String {
        val provider = getProvider(providerId)
        return if (provider.isOther) name.value else provider.logoKey
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TOP_SUBSCRIPTIONS_LIMIT = 5
        const val UPCOMING_CHARGES_LIMIT = 5
    }
}
