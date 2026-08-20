package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.calculation.SubscriptionCostCalculator
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionTotals
import javax.inject.Inject

/** Normalizes and sums the cost of the given subscriptions. */
class CalculateSubscriptionTotalsUseCase @Inject constructor() {
    operator fun invoke(subscriptions: List<Subscription>): SubscriptionTotals =
        SubscriptionCostCalculator.totals(subscriptions)
}
