package com.griff.keeper.application.subscription

import com.griff.keeper.domain.calculation.SubscriptionCostCalculator
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionTotals
import javax.inject.Inject

/** Normalizes and sums the cost of the given subscriptions. */
class CalculateSubscriptionTotalsUseCase @Inject constructor() {
    operator fun invoke(subscriptions: List<Subscription>): SubscriptionTotals =
        SubscriptionCostCalculator.totals(subscriptions)
}
