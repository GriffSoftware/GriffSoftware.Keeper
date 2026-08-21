package com.griff.subscriptions.domain.reminder

import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.testing.testObligation
import com.griff.subscriptions.domain.testing.testSubscription
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Which date a record is actually about, which is the one rule in the feature that differs per
 * category rather than per module.
 */
class ReminderCandidatesTest {

    private val defaults = ReminderDefaults.Standard

    @Test
    fun `a paid policy still reminds about the day its cover ends`() {
        val obligation = testObligation(
            category = ObligationCategory.VEHICLE_INSURANCE,
            payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
            validUntil = LocalDate.of(2027, 3, 11),
        )

        val candidate = ReminderCandidates.of(obligation, defaults)

        assertEquals(ReminderKind.INSURANCE_EXPIRY, candidate?.kind)
        assertEquals(LocalDate.of(2027, 3, 11), candidate?.targetDate)
        assertEquals(listOf(30, 7, 1), candidate?.schedule?.daysBefore)
    }

    @Test
    fun `a settled charge stops reminding about its deadline`() {
        val obligation = testObligation(
            category = ObligationCategory.LAND_TAX,
            payment = PaymentState.Paid(LocalDate.of(2026, 9, 10)),
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        )

        assertNull(ReminderCandidates.of(obligation, defaults))
    }

    @Test
    fun `an open charge reminds about its deadline, not about a cover period`() {
        val obligation = testObligation(
            category = ObligationCategory.LAND_TAX,
            payment = PaymentState.Unpaid,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        )

        val candidate = ReminderCandidates.of(obligation, defaults)

        assertEquals(ReminderKind.PAYMENT_DUE, candidate?.kind)
        assertEquals(LocalDate.of(2026, 9, 15), candidate?.targetDate)
        assertEquals(listOf(7, 1), candidate?.schedule?.daysBefore)
    }

    @Test
    fun `a subscription without a renewal date cannot be scheduled`() {
        val subscription = testSubscription(nextBillingDate = null)

        assertNull(
            ReminderCandidates.of(
                subscription = subscription,
                logoKey = "netflix",
                category = ProviderCategory.VIDEO,
                defaults = defaults,
            ),
        )
    }

    @Test
    fun `a subscription reminds about its next charge`() {
        val subscription = testSubscription(nextBillingDate = LocalDate.of(2026, 8, 28))

        val candidate = ReminderCandidates.of(
            subscription = subscription,
            logoKey = "netflix",
            category = ProviderCategory.VIDEO,
            defaults = defaults,
        )

        assertEquals(ReminderKind.SUBSCRIPTION_RENEWAL, candidate?.kind)
        assertEquals(LocalDate.of(2026, 8, 28), candidate?.targetDate)
        assertEquals(listOf(7, 1), candidate?.schedule?.daysBefore)
    }

    @Test
    fun `a record whose own switch is off is still a candidate, and is filtered later`() {
        val obligation = testObligation(remindersEnabled = false)

        assertEquals(false, ReminderCandidates.of(obligation, defaults)?.remindersEnabled)
    }
}
