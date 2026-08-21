package com.griff.keeper.domain.reminder

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationCategory
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The arithmetic the whole feature rests on, checked without a single Android class.
 *
 * Every case here is a rule a user would notice: a policy warning a month ahead, a reminder that
 * does not repeat, and - most importantly - a record entered too late that stays quiet instead of
 * firing a month of history at once.
 */
class ReminderPlannerTest {

    private val today = LocalDate.of(2026, 8, 21)

    @Test
    fun `an insurance is announced 30, 7 and 1 day before it expires`() {
        val candidate = insurance(validUntil = LocalDate.of(2026, 9, 20))

        val fireDates = ReminderPlanner.occurrences(candidate).map { it.fireDate }

        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 9, 13),
                LocalDate.of(2026, 9, 19),
            ),
            fireDates,
        )
    }

    @Test
    fun `a payment is announced 7 and 1 day before its deadline`() {
        val candidate = payment(dueDate = LocalDate.of(2026, 9, 15))

        val fireDates = ReminderPlanner.occurrences(candidate).map { it.fireDate }

        assertEquals(
            listOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 14)),
            fireDates,
        )
    }

    @Test
    fun `a subscription is announced 7 and 1 day before it renews`() {
        val candidate = subscription(nextBillingDate = LocalDate.of(2026, 8, 28))

        val fireDates = ReminderPlanner.occurrences(candidate).map { it.fireDate }

        assertEquals(
            listOf(LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 27)),
            fireDates,
        )
    }

    @Test
    fun `the 30 day reminder of a policy expiring in 30 days is due today`() {
        val candidate = insurance(validUntil = today.plusDays(30))

        val due = ReminderPlanner.dueOn(candidate, today)

        assertEquals(listOf(30), due.map { it.daysBefore })
    }

    @Test
    fun `nothing is due on a day no rule falls on`() {
        val candidate = insurance(validUntil = today.plusDays(29))

        assertTrue(ReminderPlanner.dueOn(candidate, today).isEmpty())
    }

    @Test
    fun `a record entered after its own deadline never fires its missed reminders`() {
        val candidate = insurance(validUntil = today.minusDays(1))

        assertTrue(ReminderPlanner.dueOn(candidate, today).isEmpty())
        assertNull(ReminderPlanner.nextUpcoming(candidate, today))
    }

    @Test
    fun `the next reminder skips the one already delivered`() {
        val candidate = insurance(validUntil = LocalDate.of(2026, 9, 20))
        val alreadySent = ReminderOccurrence(
            sourceType = ReminderSourceType.OBLIGATION,
            sourceId = "oc-ford",
            kind = ReminderKind.INSURANCE_EXPIRY,
            targetDate = LocalDate.of(2026, 9, 20),
            daysBefore = 30,
        )

        val next = ReminderPlanner.nextUpcoming(candidate, today) { it == alreadySent.key }

        assertEquals(LocalDate.of(2026, 9, 13), next?.fireDate)
        assertEquals(7, next?.daysBefore)
    }

    @Test
    fun `the identity of a reminder changes with its target date`() {
        val august = ReminderOccurrence(
            sourceType = ReminderSourceType.SUBSCRIPTION,
            sourceId = "netflix",
            kind = ReminderKind.SUBSCRIPTION_RENEWAL,
            targetDate = LocalDate.of(2026, 8, 28),
            daysBefore = 7,
        )
        val september = august.copy(targetDate = LocalDate.of(2026, 9, 28))

        // A renewed subscription has to be able to speak again; sharing a key with the previous
        // cycle would silence it forever.
        assertTrue(august.key != september.key)
        assertEquals("SUBSCRIPTION:netflix:2026-08-28:7", august.key)
    }

    @Test
    fun `reminders landing on the same day still get different notification ids`() {
        val insurance = ReminderOccurrence(
            sourceType = ReminderSourceType.OBLIGATION,
            sourceId = "oc-ford",
            kind = ReminderKind.INSURANCE_EXPIRY,
            targetDate = LocalDate.of(2026, 9, 20),
            daysBefore = 30,
        )
        val renewal = ReminderOccurrence(
            sourceType = ReminderSourceType.SUBSCRIPTION,
            sourceId = "netflix",
            kind = ReminderKind.SUBSCRIPTION_RENEWAL,
            targetDate = LocalDate.of(2026, 8, 28),
            daysBefore = 7,
        )

        assertEquals(insurance.fireDate, renewal.fireDate)
        assertTrue(insurance.notificationId != renewal.notificationId)
    }

    @Test
    fun `a schedule cannot fire twice on the same day`() {
        val schedule = ReminderSchedule.of(7, 7, 1)

        assertEquals(listOf(7, 1), schedule.daysBefore)
    }

    private fun insurance(validUntil: LocalDate) = candidate(
        source = ReminderSource.Obligation("oc-ford", ObligationCategory.VEHICLE_INSURANCE),
        kind = ReminderKind.INSURANCE_EXPIRY,
        target = validUntil,
        schedule = ReminderDefaults.Standard.insurance,
    )

    private fun payment(dueDate: LocalDate) = candidate(
        source = ReminderSource.Obligation("land-tax", ObligationCategory.LAND_TAX),
        kind = ReminderKind.PAYMENT_DUE,
        target = dueDate,
        schedule = ReminderDefaults.Standard.payment,
    )

    private fun subscription(nextBillingDate: LocalDate) = candidate(
        source = ReminderSource.Obligation("netflix", ObligationCategory.OTHER),
        kind = ReminderKind.SUBSCRIPTION_RENEWAL,
        target = nextBillingDate,
        schedule = ReminderDefaults.Standard.subscription,
    )

    private fun candidate(
        source: ReminderSource,
        kind: ReminderKind,
        target: LocalDate,
        schedule: ReminderSchedule,
    ) = ReminderCandidate(
        source = source,
        title = "test",
        kind = kind,
        targetDate = target,
        amount = Money.ofUnits(100),
        currency = Currency.PLN,
        schedule = schedule,
        remindersEnabled = true,
    )
}
