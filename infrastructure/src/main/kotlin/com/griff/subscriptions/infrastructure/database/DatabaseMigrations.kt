package com.griff.subscriptions.infrastructure.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * All migrations applied to [GriffDatabase], in order.
 *
 * Every version bump adds an entry here together with the exported schema of the new version;
 * destructive fallbacks are never used, because the user's subscriptions have to survive an update.
 */
internal val DatabaseMigrations: Array<Migration> = arrayOf(MigrateV1ToV2)

/**
 * Adds the obligations table and the subscription category column.
 *
 * Both changes are purely additive: the existing `subscriptions` rows are left untouched and the new
 * column stays `NULL` for them, which is exactly what a catalog entry means - "take my category from
 * the provider catalog".
 */
private object MigrateV1ToV2 : Migration(1, 2) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `category` TEXT")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `obligations` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `amount_minor_units` INTEGER NOT NULL,
                `currency_code` TEXT NOT NULL DEFAULT 'PLN',
                `payment_status` TEXT NOT NULL,
                `payment_date_epoch_day` INTEGER,
                `due_date_epoch_day` INTEGER,
                `valid_until_epoch_day` INTEGER,
                `notes` TEXT,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}
