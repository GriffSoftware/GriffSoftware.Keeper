package com.griff.keeper.application.statistics

import com.griff.keeper.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.ExpensePeriod
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.ObligationTag
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.statistics.ExpenseSource
import com.griff.keeper.domain.statistics.StatisticsPeriod
import com.griff.keeper.domain.statistics.StatisticsScope
import com.griff.keeper.domain.testing.FakeObligationRepository
import com.griff.keeper.domain.testing.FakeProviderCatalog
import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.FixedClockProvider
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** The clock is frozen at 2026-08-20, see [FixedClockProvider]. */
class GetFinanceStatisticsUseCaseTest {

    private val subscriptions = FakeSubscriptionRepository(
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

    private val obligations = FakeObligationRepository(
        listOf(
            testObligation(
                id = "o-1",
                name = "OC Ford",
                amountMinorUnits = 124_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
            ),
            testObligation(
                id = "o-2",
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                amountMinorUnits = 32_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 15)),
                validUntil = null,
            ),
            // Paid in a different year: it must not show up in 2026 despite expiring in 2026.
            testObligation(
                id = "o-3",
                name = "Ubezpieczenie drona",
                category = ObligationCategory.DRONE_INSURANCE,
                amountMinorUnits = 19_000,
                payment = PaymentState.Paid(LocalDate.of(2025, 8, 2)),
                validUntil = LocalDate.of(2026, 8, 1),
            ),
        ),
    )

    private val useCase = GetFinanceStatisticsUseCase(
        subscriptionRepository = subscriptions,
        obligationRepository = obligations,
        getSubscriptionCategory = GetSubscriptionCategoryUseCase(FakeProviderCatalog()),
        clock = FixedClockProvider(),
    )

    private fun selection(
        scope: StatisticsScope,
        period: StatisticsPeriod = StatisticsPeriod.YEAR,
    ) = MutableStateFlow(StatisticsSelection(scope, period))

    @Test
    fun `subscriptions only keeps the existing figures and skips obligations`() = runTest {
        val statistics = useCase(selection(StatisticsScope.SUBSCRIPTIONS)).first()

        val subscriptionStatistics = assertNotNull(statistics.subscriptions)
        assertNull(statistics.obligations)
        // 34,99 + (599,00 / 12 = 49,92)
        assertEquals(8491, subscriptionStatistics.totals.monthly.minorUnits)
        assertEquals(
            setOf(ProviderCategory.HOSTING, ProviderCategory.MUSIC),
            subscriptionStatistics.categories.map { it.category }.toSet(),
        )
    }

    @Test
    fun `the subscription forecast still follows the selected period`() = runTest {
        val monthly = useCase(
            selection(StatisticsScope.SUBSCRIPTIONS, StatisticsPeriod.MONTH),
        ).first()
        assertEquals(1, monthly.subscriptions?.forecast?.size)
        assertEquals(YearMonth.of(2026, 8), monthly.subscriptions?.forecast?.single()?.month)

        val rolling = useCase(
            selection(StatisticsScope.SUBSCRIPTIONS, StatisticsPeriod.TWELVE_MONTHS),
        ).first()
        assertEquals(12, rolling.subscriptions?.forecast?.size)
        assertEquals(
            59_900,
            rolling.subscriptions
                ?.forecast
                ?.single { it.month == YearMonth.of(2026, 11) }
                ?.amount
                ?.minorUnits
                ?.minus(3499),
        )
    }

    @Test
    fun `obligations only counts what was really paid inside the year`() = runTest {
        val statistics = useCase(selection(StatisticsScope.OBLIGATIONS)).first()

        val obligationStatistics = assertNotNull(statistics.obligations)
        assertNull(statistics.subscriptions)
        assertEquals(ExpensePeriod.Year(2026), statistics.window)
        // 1 240 + 320; the drone policy was paid in 2025 even though it expires in 2026.
        assertEquals(156_000, obligationStatistics.totals.paid.minorUnits)
        assertEquals(2, obligationStatistics.totals.paidCount)
        assertEquals(124_000, obligationStatistics.totals.largestPaid.minorUnits)
    }

    @Test
    fun `obligation payments land in the month they were paid in`() = runTest {
        val statistics = useCase(selection(StatisticsScope.OBLIGATIONS)).first()
        val monthly = statistics.obligations!!.monthlyPaid.associateBy { it.month }

        assertEquals(12, monthly.size)
        // A yearly policy is not spread over twelve months; it counts once, in March.
        assertEquals(156_000, monthly.getValue(YearMonth.of(2026, 3)).paidObligations.minorUnits)
        assertEquals(0, monthly.getValue(YearMonth.of(2026, 4)).paidObligations.minorUnits)
    }

    @Test
    fun `obligation tags sum up per tag, folding both taxes together`() = runTest {
        val statistics = useCase(selection(StatisticsScope.OBLIGATIONS)).first()
        val tags = statistics.obligations!!.tags.associate { it.tag to it.paid.minorUnits }

        assertEquals(124_000, tags.getValue(ObligationTag.VEHICLE))
        assertEquals(32_000, tags.getValue(ObligationTag.TAX))
    }

    @Test
    fun `all combines an estimate with real payments and keeps the two apart`() = runTest {
        val statistics = useCase(selection(StatisticsScope.ALL)).first()

        assertNotNull(statistics.subscriptions)
        assertNotNull(statistics.obligations)
        // Yearly estimate: 34,99 * 12 + 599,00
        assertEquals(101_888, statistics.estimatedSubscriptionCost.minorUnits)
        assertEquals(156_000, statistics.paidObligationCost.minorUnits)
        assertEquals(257_888, statistics.combinedTotal.minorUnits)
    }

    @Test
    fun `the combined chart keeps the two series separate per month`() = runTest {
        val statistics = useCase(selection(StatisticsScope.ALL)).first()
        val march = statistics.monthlyExpenses.single { it.month == YearMonth.of(2026, 3) }
        val april = statistics.monthlyExpenses.single { it.month == YearMonth.of(2026, 4) }

        assertEquals(12, statistics.monthlyExpenses.size)
        assertEquals(8491, march.estimatedSubscriptions.minorUnits)
        assertEquals(156_000, march.paidObligations.minorUnits)
        assertEquals(8491, april.estimatedSubscriptions.minorUnits)
        assertEquals(0, april.paidObligations.minorUnits)
    }

    @Test
    fun `the combined ranking labels every row with its source`() = runTest {
        val statistics = useCase(selection(StatisticsScope.ALL)).first()

        assertEquals("OC Ford", statistics.topExpenses.first().name)
        assertEquals(
            ExpenseSource.OBLIGATION,
            statistics.topExpenses.first().source,
        )
        assertEquals(
            setOf(
                ExpenseSource.SUBSCRIPTION,
                ExpenseSource.OBLIGATION,
            ),
            statistics.topExpenses.map { it.source }.toSet(),
        )
    }

    @Test
    fun `a month window counts only that month`() = runTest {
        val statistics = useCase(
            selection(StatisticsScope.ALL, StatisticsPeriod.MONTH),
        ).first()

        assertEquals(ExpensePeriod.Month(YearMonth.of(2026, 8)), statistics.window)
        // Nothing was paid in August 2026, and the estimate covers a single month.
        assertEquals(0, statistics.paidObligationCost.minorUnits)
        assertEquals(8491, statistics.estimatedSubscriptionCost.minorUnits)
    }

    @Test
    fun `a rolling twelve month window reaches back into the previous year`() = runTest {
        val statistics = useCase(
            selection(StatisticsScope.OBLIGATIONS, StatisticsPeriod.TWELVE_MONTHS),
        ).first()

        assertEquals(
            ExpensePeriod.Range(YearMonth.of(2025, 9), YearMonth.of(2026, 8)),
            statistics.window,
        )
        // The drone policy paid in August 2025 falls outside a window starting in September 2025.
        assertEquals(156_000, statistics.paidObligationCost.minorUnits)
    }
}
