package com.griff.keeper.domain.model

/** Aggregated cost of a set of subscriptions, normalized to comparable periods. */
data class SubscriptionTotals(
    val monthly: Money,
    val yearly: Money,
    val subscriptionCount: Int,
) {
    companion object {
        val Empty = SubscriptionTotals(
            monthly = Money.ZERO,
            yearly = Money.ZERO,
            subscriptionCount = 0,
        )
    }
}
