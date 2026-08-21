package com.griff.subscriptions.infrastructure.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of a subscription.
 *
 * The table intentionally stores primitives only: amounts as minor units, dates as epoch values and
 * enums as their stable names. Mapping to the domain model happens in
 * [com.griff.subscriptions.infrastructure.database.mapper.SubscriptionMapper], which keeps the
 * schema free to evolve independently from the domain.
 */
@Entity(tableName = SubscriptionEntity.TABLE_NAME)
data class SubscriptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    /**
     * Category chosen by the user, `null` for catalog entries.
     *
     * Nullable on purpose: a known service takes its category from the provider catalog, so storing
     * a copy would create a second source of truth that a catalog update could not fix.
     */
    @ColumnInfo(name = "category")
    val category: String?,
    @ColumnInfo(name = "price_minor_units")
    val priceMinorUnits: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "PLN")
    val currencyCode: String,
    @ColumnInfo(name = "billing_period")
    val billingPeriod: String,
    @ColumnInfo(name = "management_url")
    val managementUrl: String?,
    @ColumnInfo(name = "next_billing_date_epoch_day")
    val nextBillingDateEpochDay: Long?,
    /**
     * Existing rows default to `1`: a user who updates the app has never seen the switch, and the
     * only defensible reading of that is "yes, remind me" - the same default a new record gets.
     */
    @ColumnInfo(name = "reminders_enabled", defaultValue = "1")
    val remindersEnabled: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val TABLE_NAME = "subscriptions"
    }
}
