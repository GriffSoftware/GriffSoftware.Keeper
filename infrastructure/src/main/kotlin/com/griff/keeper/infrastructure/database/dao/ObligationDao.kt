package com.griff.keeper.infrastructure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.griff.keeper.infrastructure.database.entity.ObligationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the `obligations` table. Contains queries only, never business rules.
 *
 * Rows come out ordered by name; the useful ordering for the screen is by deadline, which depends on
 * the payment state and therefore belongs to the domain rather than to SQL.
 */
@Dao
interface ObligationDao {

    @Query("SELECT * FROM obligations ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations WHERE id = :id")
    fun observeById(id: String): Flow<ObligationEntity?>

    @Query("SELECT * FROM obligations WHERE id = :id")
    suspend fun findById(id: String): ObligationEntity?

    @Insert
    suspend fun insert(obligation: ObligationEntity)

    @Update
    suspend fun update(obligation: ObligationEntity)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Bulk write used by a backup import, see [SubscriptionDao.upsertAll]. */
    @Upsert
    suspend fun upsertAll(obligations: List<ObligationEntity>)

    /** Wipes the table for a REPLACE import. Never called outside a transaction. */
    @Query("DELETE FROM obligations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM obligations")
    suspend fun count(): Int
}
