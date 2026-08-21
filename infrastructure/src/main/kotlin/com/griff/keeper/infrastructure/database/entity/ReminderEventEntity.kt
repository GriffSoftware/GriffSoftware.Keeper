package com.griff.keeper.infrastructure.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A reminder that has already been shown to the user.
 *
 * The table exists for one reason: a worker can run several times a day, and the user must not be
 * told the same thing twice. The key is the primary key, so the database itself enforces that a
 * given reminder can only be recorded once, and no in-memory guard has to survive a process death.
 *
 * Nothing else is stored. This is a deduplication ledger, not a reminder history - the sent
 * timestamp is only kept so that old rows can be pruned.
 */
@Entity(tableName = ReminderEventEntity.TABLE_NAME)
data class ReminderEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_key")
    val reminderKey: String,
    @ColumnInfo(name = "sent_at_epoch_millis")
    val sentAtEpochMillis: Long,
) {
    companion object {
        const val TABLE_NAME = "reminder_events"
    }
}
