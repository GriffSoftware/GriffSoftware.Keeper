package com.griff.subscriptions.infrastructure.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.griff.subscriptions.infrastructure.database.dao.SubscriptionDao
import com.griff.subscriptions.infrastructure.database.entity.SubscriptionEntity

/**
 * Local database of the app.
 *
 * The schema is exported to `infrastructure/schemas` so that future versions can be migrated with
 * verified [androidx.room.migration.Migration] implementations instead of destructive fallbacks.
 */
@Database(
    entities = [SubscriptionEntity::class],
    version = SubscriptionDatabase.VERSION,
    exportSchema = true,
)
abstract class SubscriptionDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val VERSION = 1
        const val NAME = "subscriptions.db"
    }
}
