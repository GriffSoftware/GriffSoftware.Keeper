package com.griff.subscriptions.application.statistics

import com.griff.subscriptions.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.statistics.FinanceStatistics
import com.griff.subscriptions.domain.statistics.FinanceStatisticsCalculator
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.domain.statistics.StatisticsScope
import com.griff.subscriptions.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** What the statistics screen is currently looking at. */
data class StatisticsSelection(
    val scope: StatisticsScope = StatisticsScope.ALL,
    val period: StatisticsPeriod = StatisticsPeriod.YEAR,
)

/**
 * Streams statistics for the selected scope and period.
 *
 * Both repositories are observed regardless of the scope, so switching between "all", "subscriptions"
 * and "obligations" is instant and never re-queries; the calculator decides which halves of the
 * result are populated.
 */
class GetFinanceStatisticsUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val obligationRepository: ObligationRepository,
    getSubscriptionCategory: GetSubscriptionCategoryUseCase,
    private val clock: ClockProvider,
) {
    private val calculator = FinanceStatisticsCalculator(getSubscriptionCategory.resolver)

    operator fun invoke(selections: Flow<StatisticsSelection>): Flow<FinanceStatistics> =
        combine(
            subscriptionRepository.observeAll(),
            obligationRepository.observeAll(),
            selections,
        ) { subscriptions, obligations, selection ->
            calculator.calculate(
                subscriptions = subscriptions,
                obligations = obligations,
                scope = selection.scope,
                period = selection.period,
                today = clock.today(),
            )
        }
}
