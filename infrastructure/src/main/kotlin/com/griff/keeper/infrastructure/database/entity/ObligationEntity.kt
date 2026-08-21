package com.griff.keeper.infrastructure.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of an obligation.
 *
 * Like [SubscriptionEntity] the table stores primitives only: the amount as minor units, dates as
 * epoch days and enums as their stable names. The payment state is flattened into a status column
 * plus a nullable date, because a sealed hierarchy is not a column; the invariant "paid implies a
 * date" is restored when mapping back to the domain.
 *
 * The tag shown on lists is *not* stored: it is derived from [category], which keeps a single source
 * of truth and means a change to the grouping needs no migration.
 */
@Entity(tableName = ObligationEntity.TABLE_NAME)
data class ObligationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "amount_minor_units")
    val amountMinorUnits: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "PLN")
    val currencyCode: String,
    @ColumnInfo(name = "payment_status")
    val paymentStatus: String,
    @ColumnInfo(name = "payment_date_epoch_day")
    val paymentDateEpochDay: Long?,
    @ColumnInfo(name = "due_date_epoch_day")
    val dueDateEpochDay: Long?,
    @ColumnInfo(name = "valid_until_epoch_day")
    val validUntilEpochDay: Long?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    /** Defaults to `1` for rows that predate the feature, see [SubscriptionEntity]. */
    @ColumnInfo(name = "reminders_enabled", defaultValue = "1")
    val remindersEnabled: Boolean,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
) {
    companion object {
        const val TABLE_NAME = "obligations"
    }
}
