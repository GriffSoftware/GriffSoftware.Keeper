package com.griff.keeper.infrastructure.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room representation of one import/export operation.
 *
 * Device-local bookkeeping, and the only table in the database that is deliberately *not* part of a
 * backup: "this phone exported its data on the 21st" is not a fact about the user's subscriptions,
 * and carrying it to another device would be describing something that never happened there. It is
 * also the one table a REPLACE import leaves alone, so the import that wiped the records is still
 * visible in the history immediately afterwards.
 *
 * [errorType] holds a category name and nothing else - never a message, a path, a stack trace or any
 * fragment of the data that failed.
 */
@Entity(tableName = BackupOperationEntity.TABLE_NAME)
data class BackupOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "finished_at_epoch_millis")
    val finishedAtEpochMillis: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "file_name")
    val fileName: String?,
    /** Only set for imports; an export has no mode. */
    @ColumnInfo(name = "import_mode")
    val importMode: String?,
    @ColumnInfo(name = "subscription_count")
    val subscriptionCount: Int,
    @ColumnInfo(name = "obligation_count")
    val obligationCount: Int,
    @ColumnInfo(name = "settings_count")
    val settingsCount: Int,
    @ColumnInfo(name = "error_type")
    val errorType: String?,
) {
    companion object {
        const val TABLE_NAME = "backup_operations"
    }
}
