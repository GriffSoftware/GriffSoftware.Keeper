package com.griff.subscriptions.presentation.form

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.validation.SubscriptionField
import com.griff.subscriptions.presentation.common.UiMessage
import java.time.LocalDate

/** Whether the form creates a new subscription or edits an existing one. */
enum class SubscriptionFormMode { ADD, EDIT }

/** A single entry of the provider picker. */
data class ProviderOption(
    val id: String,
    val displayName: String,
    val logoKey: String,
    val isOther: Boolean,
)

data class SubscriptionFormUiState(
    val mode: SubscriptionFormMode = SubscriptionFormMode.ADD,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val providerQuery: String = "",
    val providerOptions: List<ProviderOption> = emptyList(),
    val selectedProvider: ProviderOption? = null,
    val name: String = "",
    val category: ProviderCategory = ProviderCategory.OTHER,
    val price: String = "",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val nextBillingDate: LocalDate? = null,
    val managementUrl: String = "",
    /** Field to error message resource; only fields the user should see an error for. */
    val fieldErrors: Map<SubscriptionField, Int> = emptyMap(),
    val isSaveEnabled: Boolean = false,
    val message: UiMessage? = null,
) {
    /** Custom services need a user supplied name; catalog entries take the brand name. */
    val isNameFieldVisible: Boolean get() = selectedProvider?.isOther == true

    /**
     * Only a custom service asks for a category.
     *
     * A catalog entry already has one, and letting the user override it would create a second answer
     * to the same question.
     */
    val isCategoryFieldVisible: Boolean get() = selectedProvider?.isOther == true

    val isEditable: Boolean get() = !isSaving && !isLoading
}
