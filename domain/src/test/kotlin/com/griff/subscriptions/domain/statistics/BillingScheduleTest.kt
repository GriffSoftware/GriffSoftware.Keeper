package com.griff.subscriptions.domain.statistics

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.testing.testSubscription
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillingScheduleTest {

    @Test
    fun `monthly subscription is charged in every month of the window`() {
        val dates = BillingSchedule.occurrences(
            subscription = testSubscription(
                billingPeriod = BillingPeriod.MONTHLY,
                nextBillingDate = LocalDate.of(2026, 9, 14),
            ),
            from = LocalDate.of(2026, 8, 20),
            toInclusive = LocalDate.of(2026, 12, 31),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 12, 14),
            ),
            dates,
        )
    }

    @Test
    fun `yearly subscription is charged once per year`() {
        val dates = BillingSchedule.occurrences(
            subscription = testSubscription(
                billingPeriod = BillingPeriod.YEARLY,
                nextBillingDate = LocalDate.of(2026, 11, 3),
            ),
            from = LocalDate.of(2026, 8, 20),
            toInclusive = LocalDate.of(2027, 7, 31),
        )

        assertEquals(listOf(LocalDate.of(2026, 11, 3)), dates)
    }

    @Test
    fun `stale billing dates are rolled forward`() {
        val stale = testSubscription(
            billingPeriod = BillingPeriod.MONTHLY,
            nextBillingDate = LocalDate.of(2026, 2, 5),
        )

        assertEquals(
            LocalDate.of(2026, 9, 5),
            BillingSchedule.nextOccurrenceOnOrAfter(stale, LocalDate.of(2026, 8, 20)),
        )
    }

    @Test
    fun `end of month dates are clamped without drifting`() {
        val dates = BillingSchedule.occurrences(
            subscription = testSubscription(
                billingPeriod = BillingPeriod.MONTHLY,
                nextBillingDate = LocalDate.of(2027, 1, 31),
            ),
            from = LocalDate.of(2027, 1, 1),
            toInclusive = LocalDate.of(2027, 3, 31),
        )

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31),
            ),
            dates,
        )
    }

    @Test
    fun `monthly charges keep the anchor day after a short month`() {
        val dates = BillingSchedule.occurrences(
            subscription = testSubscription(
                billingPeriod = BillingPeriod.MONTHLY,
                nextBillingDate = LocalDate.of(2026, 9, 29),
            ),
            from = LocalDate.of(2026, 8, 20),
            toInclusive = LocalDate.of(2027, 4, 30),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 10, 29),
                LocalDate.of(2026, 11, 29),
                LocalDate.of(2026, 12, 29),
                LocalDate.of(2027, 1, 29),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 29),
                LocalDate.of(2027, 4, 29),
            ),
            dates,
        )
    }

    @Test
    fun `subscription without a billing date has no occurrences`() {
        val withoutDate = testSubscription(nextBillingDate = null)

        assertTrue(BillingSchedule.occurrences(withoutDate, LocalDate.now(), LocalDate.now()).isEmpty())
        assertNull(BillingSchedule.nextOccurrenceOnOrAfter(withoutDate, LocalDate.now()))
    }
}
