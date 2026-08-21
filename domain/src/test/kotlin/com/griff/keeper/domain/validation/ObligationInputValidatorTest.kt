package com.griff.keeper.domain.validation

import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.model.PaymentStatus
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObligationInputValidatorTest {

    @Test
    fun `accepts a complete insurance form`() {
        val result = ObligationInputValidator.validate(
            input(
                name = "  OC Ford ",
                amount = "1240,50",
                paymentDate = LocalDate.of(2026, 3, 12),
                validUntil = LocalDate.of(2027, 3, 11),
                notes = "  Polisa PZU nr ABC123  ",
            ),
        )

        val valid = result as ObligationInputValidation.Valid
        assertEquals("OC Ford", valid.input.name.value)
        assertEquals(124_050, valid.input.amount.minorUnits)
        assertEquals(PaymentState.Paid(LocalDate.of(2026, 3, 12)), valid.input.payment)
        assertEquals(LocalDate.of(2027, 3, 11), valid.input.validUntil)
        assertEquals("Polisa PZU nr ABC123", valid.input.notes)
    }

    @Test
    fun `accepts a tax with a deadline and no expiry`() {
        val valid = ObligationInputValidator.validate(
            input(
                name = "Podatek od nieruchomości",
                category = ObligationCategory.PROPERTY_TAX,
                paymentStatus = PaymentStatus.UNPAID,
                paymentDate = null,
                dueDate = LocalDate.of(2026, 9, 15),
                validUntil = null,
            ),
        ) as ObligationInputValidation.Valid

        assertEquals(PaymentState.Unpaid, valid.input.payment)
        assertEquals(LocalDate.of(2026, 9, 15), valid.input.dueDate)
        assertNull(valid.input.validUntil)
    }

    @Test
    fun `an unpaid record does not require a payment date`() {
        val result = ObligationInputValidator.validate(
            input(paymentStatus = PaymentStatus.UNPAID, paymentDate = null),
        )

        assertTrue(result is ObligationInputValidation.Valid)
    }

    @Test
    fun `a record marked as paid needs the date it was paid on`() {
        val result = ObligationInputValidator.validate(
            input(paymentStatus = PaymentStatus.PAID, paymentDate = null),
        )

        val errors = (result as ObligationInputValidation.Invalid).errors
        // Without a date the amount could not be attributed to any year.
        assertTrue(ObligationInputError.PaymentDateMissing in errors)
    }

    @Test
    fun `notes and both optional dates may be left out`() {
        val valid = ObligationInputValidator.validate(
            input(dueDate = null, validUntil = null, notes = "   "),
        ) as ObligationInputValidation.Valid

        assertNull(valid.input.notes)
        assertNull(valid.input.dueDate)
        assertNull(valid.input.validUntil)
    }

    @Test
    fun `reports every invalid field at once`() {
        val result = ObligationInputValidator.validate(
            ObligationInput(
                name = "",
                category = null,
                amount = "12,345",
                paymentStatus = PaymentStatus.PAID,
                paymentDate = null,
                dueDate = null,
                validUntil = null,
                notes = "x".repeat(ObligationInputValidator.MAX_NOTES_LENGTH + 1),
                remindersEnabled = true,
            ),
        )

        val errors = (result as ObligationInputValidation.Invalid).errors
        assertTrue(ObligationInputError.NameMissing in errors)
        assertTrue(ObligationInputError.CategoryMissing in errors)
        assertTrue(ObligationInputError.Amount(PriceError.TOO_MANY_DECIMALS) in errors)
        assertTrue(ObligationInputError.PaymentDateMissing in errors)
        assertTrue(ObligationInputError.NotesTooLong in errors)
    }

    @Test
    fun `amounts are parsed the same way as subscription prices`() {
        assertEquals(
            124_000,
            (ObligationInputValidator.validate(input(amount = "1240")) as
                ObligationInputValidation.Valid).input.amount.minorUnits,
        )
        assertEquals(
            124_050,
            (ObligationInputValidator.validate(input(amount = "1240.50")) as
                ObligationInputValidation.Valid).input.amount.minorUnits,
        )
        assertEquals(
            124_050,
            (ObligationInputValidator.validate(input(amount = "1 240,50")) as
                ObligationInputValidation.Valid).input.amount.minorUnits,
        )
    }

    @Test
    fun `rejects a zero amount`() {
        val result = ObligationInputValidator.validate(input(amount = "0"))

        val errors = (result as ObligationInputValidation.Invalid).errors
        assertTrue(ObligationInputError.Amount(PriceError.ZERO) in errors)
    }

    @Test
    fun `rejects overly long names`() {
        val result = ObligationInputValidator.validate(input(name = "x".repeat(61)))

        val errors = (result as ObligationInputValidation.Invalid).errors
        assertTrue(ObligationInputError.NameTooLong in errors)
    }

    private fun input(
        name: String = "OC Ford",
        category: ObligationCategory? = ObligationCategory.VEHICLE_INSURANCE,
        amount: String = "1240,00",
        paymentStatus: PaymentStatus = PaymentStatus.PAID,
        paymentDate: LocalDate? = LocalDate.of(2026, 3, 12),
        dueDate: LocalDate? = null,
        validUntil: LocalDate? = LocalDate.of(2027, 3, 11),
        notes: String = "",
        remindersEnabled: Boolean = true,
    ) = ObligationInput(
        name = name,
        category = category,
        amount = amount,
        paymentStatus = paymentStatus,
        paymentDate = paymentDate,
        dueDate = dueDate,
        validUntil = validUntil,
        notes = notes,
        remindersEnabled = remindersEnabled,
    )
}
