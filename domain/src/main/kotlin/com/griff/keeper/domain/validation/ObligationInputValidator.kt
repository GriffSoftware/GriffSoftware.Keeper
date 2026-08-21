package com.griff.keeper.domain.validation

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ObligationName
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.model.PaymentStatus

/** Turns raw obligation form values into a [ValidatedObligationInput] or a list of field errors. */
object ObligationInputValidator {

    /** Long enough for a policy number and a note about it, short enough to stay a note. */
    const val MAX_NOTES_LENGTH: Int = 280

    fun validate(input: ObligationInput): ObligationInputValidation {
        val errors = mutableListOf<ObligationInputError>()

        val trimmedName = input.name.trim()
        val name: ObligationName? = when {
            trimmedName.isEmpty() -> {
                errors += ObligationInputError.NameMissing
                null
            }

            trimmedName.length > ObligationName.MAX_LENGTH -> {
                errors += ObligationInputError.NameTooLong
                null
            }

            else -> ObligationName.of(trimmedName)
        }

        val category = input.category
        if (category == null) errors += ObligationInputError.CategoryMissing

        val amount = when (val result = PriceParser.parse(input.amount)) {
            is PriceParseResult.Success -> result.money
            is PriceParseResult.Failure -> {
                errors += ObligationInputError.Amount(result.error)
                null
            }
        }

        // A paid record without a date cannot be attributed to a year, so the date is mandatory
        // exactly when the user says the charge has been settled.
        val payment = PaymentState.ofOrNull(input.paymentStatus, input.paymentDate)
        if (payment == null) errors += ObligationInputError.PaymentDateMissing

        val trimmedNotes = input.notes.trim()
        if (trimmedNotes.length > MAX_NOTES_LENGTH) errors += ObligationInputError.NotesTooLong

        if (errors.isNotEmpty() || name == null || category == null || amount == null || payment == null) {
            return ObligationInputValidation.Invalid(errors)
        }

        return ObligationInputValidation.Valid(
            ValidatedObligationInput(
                name = name,
                category = category,
                amount = amount,
                currency = Currency.Default,
                payment = payment,
                // Both dates stay optional: a policy usually has an expiry, a tax a deadline, and
                // nothing stops a record from having either, both or neither.
                dueDate = input.dueDate,
                validUntil = input.validUntil,
                notes = trimmedNotes.ifEmpty { null },
                remindersEnabled = input.remindersEnabled,
            ),
        )
    }
}
