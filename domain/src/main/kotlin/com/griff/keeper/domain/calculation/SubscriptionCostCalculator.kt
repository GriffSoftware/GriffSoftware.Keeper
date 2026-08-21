package com.griff.keeper.domain.calculation

import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.model.SubscriptionTotals
import com.griff.keeper.domain.model.sum

/**
 * Normalizes and aggregates subscription costs.
 *
 * Monthly and yearly amounts are never mixed: every subscription is first converted to the
 * requested period ([Subscription.monthlyEquivalent] / [Subscription.yearlyEquivalent]) and only
 * then summed up. The yearly total is the sum of exact yearly amounts rather than
 * `monthlyTotal * 12`, so yearly subscriptions are not distorted by monthly rounding.
 */
object SubscriptionCostCalculator {

    fun monthlyTotal(subscriptions: Collection<Subscription>): Money =
        subscriptions.map { it.monthlyEquivalent }.sum()

    fun yearlyTotal(subscriptions: Collection<Subscription>): Money =
        subscriptions.map { it.yearlyEquivalent }.sum()

    fun totals(subscriptions: Collection<Subscription>): SubscriptionTotals =
        SubscriptionTotals(
            monthly = monthlyTotal(subscriptions),
            yearly = yearlyTotal(subscriptions),
            subscriptionCount = subscriptions.size,
        )
}
