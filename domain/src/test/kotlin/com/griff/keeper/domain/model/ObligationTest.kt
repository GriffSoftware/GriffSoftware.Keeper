package com.griff.keeper.domain.model

import com.griff.keeper.domain.testing.testObligation
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObligationTest {

    private val today = LocalDate.of(2026, 8, 20)

    @Test
    fun `every category maps to exactly one tag`() {
        assertEquals(ObligationTag.VEHICLE, ObligationCategory.VEHICLE_INSURANCE.tag)
        assertEquals(ObligationTag.HOME, ObligationCategory.HOME_INSURANCE.tag)
        assertEquals(ObligationTag.LAND, ObligationCategory.LAND_INSURANCE.tag)
        assertEquals(ObligationTag.DRONE, ObligationCategory.DRONE_INSURANCE.tag)
        assertEquals(ObligationTag.TAX, ObligationCategory.PROPERTY_TAX.tag)
        assertEquals(ObligationTag.TAX, ObligationCategory.LAND_TAX.tag)
        assertEquals(ObligationTag.OTHER, ObligationCategory.OTHER.tag)
    }

    @Test
    fun `every tag knows the categories it stands for`() {
        assertEquals(
            setOf(ObligationCategory.PROPERTY_TAX, ObligationCategory.LAND_TAX),
            ObligationTag.TAX.categories,
        )
        // Nothing falls through the mapping in either direction.
        assertEquals(
            ObligationCategory.entries.toSet(),
            ObligationTag.entries.flatMap { it.categories }.toSet(),
        )
    }

    @Test
    fun `a settled policy is about the end of its cover`() {
        val policy = testObligation(
            payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
            validUntil = LocalDate.of(2027, 3, 11),
            dueDate = LocalDate.of(2026, 3, 1),
        )

        assertEquals(LocalDate.of(2027, 3, 11), policy.deadline)
        // Attribution always follows the payment, never the expiry.
        assertEquals(LocalDate.of(2026, 3, 12), policy.periodDate)
    }

    @Test
    fun `an open charge is about its payment deadline`() {
        val tax = testObligation(
            category = ObligationCategory.PROPERTY_TAX,
            payment = PaymentState.Unpaid,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        )

        assertEquals(LocalDate.of(2026, 9, 15), tax.deadline)
        assertEquals(LocalDate.of(2026, 9, 15), tax.periodDate)
        assertNull(tax.paymentDate)
    }

    @Test
    fun `a record without dates has no deadline at all`() {
        val undated = testObligation(
            payment = PaymentState.Unpaid,
            dueDate = null,
            validUntil = null,
        )

        assertNull(undated.deadline)
        assertNull(undated.periodDate)
        assertNull(undated.daysUntilDeadline(today))
    }

    @Test
    fun `days until the deadline go negative once it has passed`() {
        val expiring = testObligation(validUntil = LocalDate.of(2026, 9, 1))
        val expired = testObligation(validUntil = LocalDate.of(2026, 8, 1))

        assertEquals(12, expiring.daysUntilDeadline(today))
        assertEquals(-19, expired.daysUntilDeadline(today))
    }

    @Test
    fun `insurance categories expire, taxes do not`() {
        assertTrue(ObligationCategory.VEHICLE_INSURANCE.expires)
        assertTrue(ObligationCategory.DRONE_INSURANCE.expires)
        assertTrue(!ObligationCategory.PROPERTY_TAX.expires)
        assertTrue(!ObligationCategory.OTHER.expires)
    }

    @Test
    fun `a payment state cannot be paid without a date`() {
        assertNull(PaymentState.ofOrNull(PaymentStatus.PAID, paymentDate = null))
        assertEquals(
            PaymentState.Paid(today),
            PaymentState.ofOrNull(PaymentStatus.PAID, paymentDate = today),
        )
        // An unpaid record simply ignores a date that was typed before the status was switched.
        assertEquals(PaymentState.Unpaid, PaymentState.ofOrNull(PaymentStatus.UNPAID, today))
    }
}
