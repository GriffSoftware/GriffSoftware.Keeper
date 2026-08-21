package com.griff.keeper.infrastructure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.griff.keeper.infrastructure.database.entity.BackupOperationEntity
import kotlinx.coroutines.flow.Flow

/** Data access for the `backup_operations` table. Contains queries only, never business rules. */
@Dao
interface BackupOperationDao {

    /**
     * Newest first, capped by the caller.
     *
     * Ordered by the moment the operation ended rather than started: that is when its outcome became
     * true, and it is the time the list shows.
     */
    @Query(
        "SELECT * FROM backup_operations " +
            "ORDER BY finished_at_epoch_millis DESC, id DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<BackupOperationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: BackupOperationEntity)

    /** Keeps the log from growing without bound; the oldest entries fall off the end. */
    @Query(
        "DELETE FROM backup_operations WHERE id NOT IN (" +
            "SELECT id FROM backup_operations " +
            "ORDER BY finished_at_epoch_millis DESC, id DESC LIMIT :keep" +
            ")",
    )
    suspend fun trimTo(keep: Int)
}
