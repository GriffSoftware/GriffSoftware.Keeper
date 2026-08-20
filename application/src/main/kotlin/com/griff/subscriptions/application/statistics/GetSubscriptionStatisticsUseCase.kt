package com.griff.subscriptions.application.statistics

import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.repository.ProviderCatalog
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.statistics.ProviderCategoryResolver
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.domain.statistics.SubscriptionStatistics
import com.griff.subscriptions.domain.statistics.SubscriptionStatisticsCalculator
import com.griff.subscriptions.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Streams statistics for the selected [StatisticsPeriod]. */
class GetSubscriptionStatisticsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val catalog: ProviderCatalog,
    private val clock: ClockProvider,
) {
    private val calculator = SubscriptionStatisticsCalculator(
        ProviderCategoryResolver { providerId ->
            catalog.findById(providerId)?.category ?: ProviderCategory.OTHER
        },
    )

    operator fun invoke(periods: Flow<StatisticsPeriod>): Flow<SubscriptionStatistics> =
        combine(repository.observeAll(), periods) { subscriptions, period ->
            calculator.calculate(subscriptions, period, clock.today())
        }
}
