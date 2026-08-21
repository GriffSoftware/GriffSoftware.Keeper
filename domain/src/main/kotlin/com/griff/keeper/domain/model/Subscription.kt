package com.griff.keeper.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A single subscription owned by the user.
 *
 * [providerId] always points at a catalog entry; services outside of the catalog use
 * [ProviderId.OTHER] together with a user supplied [name]. Such a custom entry has no catalog
 * category, so it carries its own [categoryOverride]; catalog entries leave it `null` and take the
 * category from the catalog, which keeps a single source of truth for known services.
 */
data class Subscription(
    val id: SubscriptionId,
    val providerId: ProviderId,
    val name: SubscriptionName,
    val categoryOverride: ProviderCategory?,
    val price: Money,
    val currency: Currency,
    val billingPeriod: BillingPeriod,
    val managementUrl: ManagementUrl?,
    val nextBillingDate: LocalDate?,
    /**
     * Whether this subscription may produce reminders.
     *
     * Independent of the app-wide switch: turning reminders off globally must not rewrite the
     * records, so that turning them back on restores exactly what the user had chosen per service.
     */
    val remindersEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** Cost normalized to a single month, rounded half up. */
    val monthlyEquivalent: Money = price.dividedBy(billingPeriod.monthsPerPeriod)

    /** Cost normalized to a full year. */
    val yearlyEquivalent: Money = price * billingPeriod.periodsPerYear
}
