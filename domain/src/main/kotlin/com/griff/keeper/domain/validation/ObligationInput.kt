package com.griff.keeper.domain.validation

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.ObligationName
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.domain.model.PaymentStatus
import java.time.LocalDate

/** Raw obligation form values as typed by the user. */
data class ObligationInput(
    val name: String,
    val category: ObligationCategory?,
    val amount: String,
    val paymentStatus: PaymentStatus,
    val paymentDate: LocalDate?,
    val dueDate: LocalDate?,
    val validUntil: LocalDate?,
    val notes: String,
    val remindersEnabled: Boolean,
)

/**
 * Form values that passed domain validation.
 *
 * Use cases only accept this type, so unvalidated input cannot reach the repository.
 */
@ConsistentCopyVisibility
data class ValidatedObligationInput internal constructor(
    val name: ObligationName,
    val category: ObligationCategory,
    val amount: Money,
    val currency: Currency,
    val payment: PaymentState,
    val dueDate: LocalDate?,
    val validUntil: LocalDate?,
    val notes: String?,
    val remindersEnabled: Boolean,
)

enum class ObligationField {
    NAME,
    CATEGORY,
    AMOUNT,
    PAYMENT_DATE,
    NOTES,
}

sealed interface ObligationInputError {
    val field: ObligationField

    data object NameMissing : ObligationInputError {
        override val field = ObligationField.NAME
    }

    data object NameTooLong : ObligationInputError {
        override val field = ObligationField.NAME
    }

    data object CategoryMissing : ObligationInputError {
        override val field = ObligationField.CATEGORY
    }

    data class Amount(val error: PriceError) : ObligationInputError {
        override val field = ObligationField.AMOUNT
    }

    /** A record marked as paid without a date could never be booked to a year. */
    data object PaymentDateMissing : ObligationInputError {
        override val field = ObligationField.PAYMENT_DATE
    }

    data object NotesTooLong : ObligationInputError {
        override val field = ObligationField.NOTES
    }
}

sealed interface ObligationInputValidation {
    data class Valid(val input: ValidatedObligationInput) : ObligationInputValidation
    data class Invalid(val errors: List<ObligationInputError>) : ObligationInputValidation
}
