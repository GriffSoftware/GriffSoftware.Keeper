package com.griff.keeper.infrastructure.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.griff.keeper.infrastructure.database.dao.ObligationDao
import com.griff.keeper.infrastructure.database.dao.ReminderEventDao
import com.griff.keeper.infrastructure.database.dao.SubscriptionDao
import com.griff.keeper.infrastructure.database.entity.ObligationEntity
import com.griff.keeper.infrastructure.database.entity.ReminderEventEntity
import com.griff.keeper.infrastructure.database.entity.SubscriptionEntity

/**
 * Local database of the app.
 *
 * The schema is exported to `infrastructure/schemas` so that every version can be migrated with
 * verified [androidx.room.migration.Migration] implementations instead of destructive fallbacks.
 *
 * [NAME] keeps its original value even though the database now holds more than subscriptions:
 * changing the file name would leave the user's existing database behind and look exactly like data
 * loss.
 */
@Database(
    entities = [SubscriptionEntity::class, ObligationEntity::class, ReminderEventEntity::class],
    version = GriffDatabase.VERSION,
    exportSchema = true,
)
abstract class GriffDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun obligationDao(): ObligationDao

    abstract fun reminderEventDao(): ReminderEventDao

    companion object {
        const val VERSION = 3
        const val NAME = "subscriptions.db"
    }
}
