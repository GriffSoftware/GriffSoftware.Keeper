package com.griff.subscriptions.presentation.home

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.common.UiMessage

/** One row of the subscription list. */
data class SubscriptionListItem(
    val id: String,
    val name: String,
    val logoKey: String,
    val billingPeriod: BillingPeriod,
    val price: Money,
)

/** Immutable state rendered by the home screen. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<SubscriptionListItem> = emptyList(),
    val totals: SubscriptionTotals = SubscriptionTotals.Empty,
    val totalSubscriptionCount: Int = 0,
    val message: UiMessage? = null,
) {
    val isFiltered: Boolean get() = query.isNotBlank()

    /** No subscriptions at all - as opposed to a search that returned nothing. */
    val isEmpty: Boolean get() = !isLoading && totalSubscriptionCount == 0

    val hasNoResults: Boolean get() = !isLoading && totalSubscriptionCount > 0 && items.isEmpty()
}
