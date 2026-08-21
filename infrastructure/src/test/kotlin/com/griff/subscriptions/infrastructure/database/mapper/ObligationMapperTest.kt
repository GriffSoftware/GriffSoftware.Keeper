package com.griff.subscriptions.infrastructure.database.mapper

import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.testing.testObligation
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObligationMapperTest {

    private val obligation = testObligation(
        id = "o-1",
        name = "OC Ford",
        amountMinorUnits = 124_000,
        payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
        validUntil = LocalDate.of(2027, 3, 11),
        notes = "Polisa PZU nr ABC123",
    )

    @Test
    fun `maps an obligation to primitive columns`() {
        val entity = ObligationMapper.toEntity(obligation)

        assertEquals("o-1", entity.id)
        assertEquals("OC Ford", entity.name)
        assertEquals("VEHICLE_INSURANCE", entity.category)
        assertEquals(124_000, entity.amountMinorUnits)
        assertEquals("PLN", entity.currencyCode)
        assertEquals("PAID", entity.paymentStatus)
        assertEquals(LocalDate.of(2026, 3, 12).toEpochDay(), entity.paymentDateEpochDay)
        assertEquals(LocalDate.of(2027, 3, 11).toEpochDay(), entity.validUntilEpochDay)
        assertNull(entity.dueDateEpochDay)
        assertEquals("Polisa PZU nr ABC123", entity.notes)
        assertEquals(true, entity.remindersEnabled)
    }

    @Test
    fun `the reminder switch survives a round trip in both positions`() {
        val off = obligation.copy(remindersEnabled = false)

        assertEquals(false, ObligationMapper.toEntity(off).remindersEnabled)
        assertEquals(off, ObligationMapper.toDomain(ObligationMapper.toEntity(off)))
    }

    @Test
    fun `round trip keeps every value`() {
        assertEquals(obligation, ObligationMapper.toDomain(ObligationMapper.toEntity(obligation)))
    }

    @Test
    fun `an unpaid obligation stores a due date and no payment date`() {
        val unpaid = obligation.copy(
            category = ObligationCategory.PROPERTY_TAX,
            payment = PaymentState.Unpaid,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        )

        val entity = ObligationMapper.toEntity(unpaid)

        assertEquals("UNPAID", entity.paymentStatus)
        assertNull(entity.paymentDateEpochDay)
        assertEquals(LocalDate.of(2026, 9, 15).toEpochDay(), entity.dueDateEpochDay)
        assertEquals(unpaid, ObligationMapper.toDomain(entity))
    }

    @Test
    fun `a row claiming to be paid without a date reads back as unpaid`() {
        val inconsistent = ObligationMapper.toEntity(obligation).copy(paymentDateEpochDay = null)

        // Cannot happen through the mapper, but a single bad row must not make the list unopenable.
        assertEquals(PaymentState.Unpaid, ObligationMapper.toDomain(inconsistent).payment)
    }
}
