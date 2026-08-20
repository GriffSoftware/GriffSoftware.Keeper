package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.ProviderCategoryResolver
import com.griff.subscriptions.domain.testing.testSubscription
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionStatisticsCalculatorTest {

    private val today = LocalDate.of(2026, 8, 20)

    private val categories = mapOf(
        "spotify" to ProviderCategory.MUSIC,
        "netflix" to ProviderCategory.VIDEO,
        "google_workspace" to ProviderCategory.CLOUD,
    )

    private val calculator = SubscriptionStatisticsCalculator(
        ProviderCategoryResolver { categories[it.value] ?: ProviderCategory.OTHER },
    )

    @Test
    fun `twelve month forecast places monthly charges in every month`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(
                testSubscription(
                    id = "1",
                    providerId = "spotify",
                    priceMinorUnits = 3499,
                    billingPeriod = BillingPeriod.MONTHLY,
                    nextBillingDate = LocalDate.of(2026, 9, 14),
                ),
            ),
            period = StatisticsPeriod.TWELVE_MONTHS,
            today = today,
        )

        assertEquals(12, statistics.forecast.size)
        assertEquals(YearMonth.of(2026, 8), statistics.forecast.first().month)
        // August charge already happened (next date is in September).
        assertEquals(0, statistics.forecast.first().amount.minorUnits)
        assertEquals(3499, statistics.forecast[1].amount.minorUnits)
        assertEquals(3499, statistics.forecast.last().amount.minorUnits)
    }

    @Test
    fun `yearly subscription lands only in its renewal month`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(
                testSubscription(
                    id = "1",
                    providerId = "netflix",
                    priceMinorUnits = 59_900,
                    billingPeriod = BillingPeriod.YEARLY,
                    nextBillingDate = LocalDate.of(2026, 11, 3),
                ),
            ),
            period = StatisticsPeriod.TWELVE_MONTHS,
            today = today,
        )

        val november = statistics.forecast.single { it.month == YearMonth.of(2026, 11) }
        assertEquals(59_900, november.amount.minorUnits)
        assertEquals(59_900, statistics.forecast.sumOf { it.amount.minorUnits })
    }

    @Test
    fun `year period stops at december`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(testSubscription(nextBillingDate = LocalDate.of(2026, 9, 1))),
            period = StatisticsPeriod.YEAR,
            today = today,
        )

        assertEquals(5, statistics.forecast.size)
        assertEquals(YearMonth.of(2026, 12), statistics.forecast.last().month)
    }

    @Test
    fun `month period lists upcoming charges of the current month only`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(
                testSubscription(id = "1", priceMinorUnits = 3499, nextBillingDate = LocalDate.of(2026, 8, 25)),
                testSubscription(id = "2", priceMinorUnits = 2999, nextBillingDate = LocalDate.of(2026, 9, 2)),
            ),
            period = StatisticsPeriod.MONTH,
            today = today,
        )

        assertEquals(1, statistics.forecast.size)
        assertEquals(3499, statistics.forecast.single().amount.minorUnits)
        assertEquals(1, statistics.upcomingCharges.size)
        assertEquals(LocalDate.of(2026, 8, 25), statistics.upcomingCharges.single().date)
    }

    @Test
    fun `subscriptions without a billing date are reported separately`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(
                testSubscription(id = "1", priceMinorUnits = 3499, nextBillingDate = null),
                testSubscription(
                    id = "2",
                    priceMinorUnits = 59_900,
                    billingPeriod = BillingPeriod.YEARLY,
                    nextBillingDate = null,
                ),
            ),
            period = StatisticsPeriod.TWELVE_MONTHS,
            today = today,
        )

        assertFalse(statistics.hasForecastData)
        assertEquals(2, statistics.subscriptionsWithoutBillingDate)
        // 34,99 + 49,92
        assertEquals(8491, statistics.unscheduledMonthlyCost.minorUnits)
        assertEquals(8491, statistics.totals.monthly.minorUnits)
    }

    @Test
    fun `category breakdown is sorted by monthly cost and sums up to the total`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(
                testSubscription(id = "1", providerId = "spotify", priceMinorUnits = 3499),
                testSubscription(id = "2", providerId = "netflix", priceMinorUnits = 6700),
                testSubscription(id = "3", providerId = "unknown_service", priceMinorUnits = 1000),
            ),
            period = StatisticsPeriod.MONTH,
            today = today,
        )

        assertEquals(
            listOf(ProviderCategory.VIDEO, ProviderCategory.MUSIC, ProviderCategory.OTHER),
            statistics.categories.map { it.category },
        )
        assertEquals(
            statistics.totals.monthly.minorUnits,
            statistics.categories.sumOf { it.monthly.minorUnits },
        )
        assertTrue(statistics.categories.first().share > 0.5f)
    }

    @Test
    fun `top subscriptions are ranked by monthly equivalent`() {
        val statistics = calculator.calculate(
            subscriptions = listOf(
                testSubscription(id = "1", name = "Spotify", priceMinorUnits = 3499),
                testSubscription(
                    id = "2",
                    name = "Google Workspace",
                    priceMinorUnits = 103_200,
                    billingPeriod = BillingPeriod.YEARLY,
                ),
            ),
            period = StatisticsPeriod.MONTH,
            today = today,
        )

        assertEquals(
            listOf("Google Workspace", "Spotify"),
            statistics.topSubscriptions.map { it.name.value },
        )
    }

    @Test
    fun `no subscriptions yields empty statistics`() {
        val statistics = calculator.calculate(emptyList(), StatisticsPeriod.MONTH, today)

        assertEquals(SubscriptionStatistics.empty(StatisticsPeriod.MONTH), statistics)
    }
}
