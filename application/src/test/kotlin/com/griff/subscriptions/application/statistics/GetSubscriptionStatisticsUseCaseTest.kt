package com.griff.subscriptions.application.statistics

import com.griff.subscriptions.domain.testing.FakeProviderCatalog
import com.griff.subscriptions.domain.testing.FakeSubscriptionRepository
import com.griff.subscriptions.domain.testing.FixedClockProvider
import com.griff.subscriptions.domain.testing.testSubscription
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GetSubscriptionStatisticsUseCaseTest {

    private val repository = FakeSubscriptionRepository(
        listOf(
            testSubscription(
                id = "1",
                providerId = "spotify",
                name = "Spotify",
                priceMinorUnits = 3499,
                nextBillingDate = LocalDate.of(2026, 9, 14),
            ),
            testSubscription(
                id = "2",
                providerId = "seohost",
                name = "SeoHost.pl",
                priceMinorUnits = 59_900,
                billingPeriod = BillingPeriod.YEARLY,
                nextBillingDate = LocalDate.of(2026, 11, 3),
            ),
        ),
    )

    private val useCase = GetSubscriptionStatisticsUseCase(
        repository = repository,
        catalog = FakeProviderCatalog(),
        clock = FixedClockProvider(),
    )

    @Test
    fun `resolves categories from the catalog`() = runTest {
        val statistics = useCase(MutableStateFlow(StatisticsPeriod.TWELVE_MONTHS)).first()

        assertEquals(
            setOf(ProviderCategory.HOSTING, ProviderCategory.MUSIC),
            statistics.categories.map { it.category }.toSet(),
        )
    }

    @Test
    fun `forecast follows the selected period`() = runTest {
        val periods = MutableStateFlow(StatisticsPeriod.MONTH)

        val monthly = useCase(periods).first()
        assertEquals(1, monthly.forecast.size)
        assertEquals(YearMonth.of(2026, 8), monthly.forecast.single().month)

        periods.value = StatisticsPeriod.TWELVE_MONTHS
        val rolling = useCase(periods).first()
        assertEquals(12, rolling.forecast.size)
        assertEquals(
            59_900,
            rolling.forecast.single { it.month == YearMonth.of(2026, 11) }.amount.minorUnits -
                3499,
        )
    }
}
