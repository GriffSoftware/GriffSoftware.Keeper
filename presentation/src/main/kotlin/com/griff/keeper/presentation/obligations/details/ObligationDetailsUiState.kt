package com.griff.keeper.presentation.obligations.details

import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.application.reminder.ItemReminderState
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.obligations.DeadlineStatus
import java.time.LocalDate

/** Everything the details screen shows about one obligation. */
data class ObligationDetails(
    val id: String,
    val name: String,
    val category: ObligationCategory,
    val amount: Money,
    val paymentStatus: PaymentStatus,
    val paymentDate: LocalDate?,
    val dueDate: LocalDate?,
    val validUntil: LocalDate?,
    val notes: String?,
    val deadline: DeadlineStatus?,
)

data class ObligationDetailsUiState(
    val isLoading: Boolean = true,
    val details: ObligationDetails? = null,
    /** `null` until the reminder state has loaded, or when the record does not exist. */
    val reminders: ItemReminderState? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val message: UiMessage? = null,
) {
    val isMissing: Boolean get() = !isLoading && details == null
}
