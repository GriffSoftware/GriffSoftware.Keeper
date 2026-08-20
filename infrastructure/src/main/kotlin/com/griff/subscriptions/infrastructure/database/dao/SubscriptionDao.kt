package com.griff.subscriptions.infrastructure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.griff.subscriptions.infrastructure.database.entity.SubscriptionEntity
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
}
