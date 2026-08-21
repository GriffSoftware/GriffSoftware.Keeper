package com.griff.keeper.domain.calculation

import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals

class SubscriptionCostCalculatorTest {

    @Test
    fun `monthly subscription keeps its price as monthly equivalent`() {
        val monthly = testSubscription(priceMinorUnits = 3499, billingPeriod = BillingPeriod.MONTHLY)

        assertEquals(3499, monthly.monthlyEquivalent.minorUnits)
        assertEquals(41_988, monthly.yearlyEquivalent.minorUnits)
    }

    @Test
    fun `yearly subscription is normalized to a month`() {
        val yearly = testSubscription(priceMinorUnits = 59_900, billingPeriod = BillingPeriod.YEARLY)

        assertEquals(4992, yearly.monthlyEquivalent.minorUnits)
        assertEquals(59_900, yearly.yearlyEquivalent.minorUnits)
    }

    @Test
    fun `totals never mix billing periods`() {
        val totals = SubscriptionCostCalculator.totals(
            listOf(
                testSubscription(id = "1", priceMinorUnits = 3499, billingPeriod = BillingPeriod.MONTHLY),
                testSubscription(id = "2", priceMinorUnits = 59_900, billingPeriod = BillingPeriod.YEARLY),
            ),
        )

        // 34,99 + (599,00 / 12 = 49,92) = 84,91
        assertEquals(8491, totals.monthly.minorUnits)
        // 34,99 * 12 = 419,88 plus the exact yearly price 599,00 = 1018,88
        assertEquals(101_888, totals.yearly.minorUnits)
        assertEquals(2, totals.subscriptionCount)
    }

    @Test
    fun `empty list has zero totals`() {
        val totals = SubscriptionCostCalculator.totals(emptyList())

        assertEquals(0, totals.monthly.minorUnits)
        assertEquals(0, totals.yearly.minorUnits)
        assertEquals(0, totals.subscriptionCount)
    }
}
