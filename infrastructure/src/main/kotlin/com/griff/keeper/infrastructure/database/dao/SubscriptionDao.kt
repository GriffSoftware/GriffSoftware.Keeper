package com.griff.keeper.infrastructure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.griff.keeper.infrastructure.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

/** Data access for the `subscriptions` table. Contains queries only, never business rules. */
@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun observeById(id: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun findById(id: String): SubscriptionEntity?

    @Insert
    suspend fun insert(subscription: SubscriptionEntity)

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Bulk write used by a backup import.
     *
     * Upsert rather than insert-or-update: an import is planned against a snapshot, and between the
     * plan and the write the row could in principle have appeared or vanished. Upsert makes the
     * write describe the intended end state instead of depending on what was there a moment ago.
     */
    @Upsert
    suspend fun upsertAll(subscriptions: List<SubscriptionEntity>)

    /** Wipes the table for a REPLACE import. Never called outside a transaction. */
    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun count(): Int
}
