package com.griff.keeper.presentation.obligations.form

import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.domain.validation.ObligationField
import com.griff.keeper.presentation.common.UiMessage
import java.time.LocalDate

/** Whether the form creates a new obligation or edits an existing one. */
enum class ObligationFormMode { ADD, EDIT }

data class ObligationFormUiState(
    val mode: ObligationFormMode = ObligationFormMode.ADD,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val category: ObligationCategory? = null,
    val amount: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val paymentDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val notes: String = "",
    /** New records opt in; the global switch, not this flag, is what silences everything. */
    val remindersEnabled: Boolean = true,
    /** Field to error message resource; only fields the user should see an error for. */
    val fieldErrors: Map<ObligationField, Int> = emptyMap(),
    val isSaveEnabled: Boolean = false,
    val message: UiMessage? = null,
) {
    /**
     * A settled record needs the date it was settled on; an open one does not.
     *
     * This is the one field the payment status actually switches, rather than merely reorders.
     */
    val isPaymentDateVisible: Boolean get() = paymentStatus == PaymentStatus.PAID

    /**
     * Whether the cover end is the more important of the two remaining dates.
     *
     * The form shows both dates for every category - a tax can have an expiry and a policy a payment
     * deadline - but puts the one the category is usually about first. Duplicating the whole form per
     * category would be far more code and would break as soon as a record does not fit its box.
     */
    val isExpiryLed: Boolean get() = category?.expires == true

    val isEditable: Boolean get() = !isSaving && !isLoading
}
