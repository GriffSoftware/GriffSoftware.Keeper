package com.griff.subscriptions.presentation.details

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.application.reminder.ItemReminderState
import com.griff.subscriptions.presentation.common.UiMessage
import java.time.LocalDate

/** Everything the details screen shows about one subscription. */
data class SubscriptionDetails(
    val id: String,
    val name: String,
    val logoKey: String,
    val category: ProviderCategory,
    val price: Money,
    val billingPeriod: BillingPeriod,
    val monthlyEquivalent: Money,
    val yearlyEquivalent: Money,
    val nextBillingDate: LocalDate?,
    val managementUrl: String?,
)

data class SubscriptionDetailsUiState(
    val isLoading: Boolean = true,
    val details: SubscriptionDetails? = null,
    /** `null` until the reminder state has loaded, or when the record does not exist. */
    val reminders: ItemReminderState? = null,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val message: UiMessage? = null,
) {
    val isMissing: Boolean get() = !isLoading && details == null
}
