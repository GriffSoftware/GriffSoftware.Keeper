package com.griff.keeper.infrastructure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.griff.keeper.infrastructure.database.entity.ReminderEventEntity
import kotlinx.coroutines.flow.Flow

/** Data access for the `reminder_events` table. Contains queries only, never business rules. */
@Dao
interface ReminderEventDao {

    @Query("SELECT reminder_key FROM reminder_events")
    fun observeKeys(): Flow<List<String>>

    @Query("SELECT reminder_key FROM reminder_events")
    suspend fun keys(): List<String>

    /**
     * Ignores a second insert of the same key.
     *
     * Two workers racing on the same day must not turn into two notifications or into a crash; the
     * first one wins and the second is a no-op.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: ReminderEventEntity)

    @Query("DELETE FROM reminder_events WHERE sent_at_epoch_millis < :thresholdEpochMillis")
    suspend fun deleteSentBefore(thresholdEpochMillis: Long)
}
