package com.griff.subscriptions.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A single subscription owned by the user.
 *
 * [providerId] always points at a catalog entry; services outside of the catalog use
 * [ProviderId.OTHER] together with a user supplied [name].
 */
data class Subscription(
    val id: SubscriptionId,
    val providerId: ProviderId,
    val name: SubscriptionName,
    val price: Money,
    val currency: Currency,
    val billingPeriod: BillingPeriod,
    val managementUrl: ManagementUrl?,
    val nextBillingDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** Cost normalized to a single month, rounded half up. */
    val monthlyEquivalent: Money = price.dividedBy(billingPeriod.monthsPerPeriod)

    /** Cost normalized to a full year. */
    val yearlyEquivalent: Money = price * billingPeriod.periodsPerYear
}
