package com.griff.keeper.infrastructure.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * All migrations applied to [GriffDatabase], in order.
 *
 * Every version bump adds an entry here together with the exported schema of the new version;
 * destructive fallbacks are never used, because the user's subscriptions have to survive an update.
 */
internal val DatabaseMigrations: Array<Migration> = arrayOf(MigrateV1ToV2, MigrateV2ToV3)

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

/**
 * Adds the reminder switches and the deduplication ledger.
 *
 * Purely additive again: both new columns arrive with a default of `1`, so every record the user
 * already has keeps working and starts producing reminders without being re-saved. The ledger starts
 * empty, which is correct - nothing has been delivered yet, and the engine only ever fires reminders
 * that fall on the current day, so an empty ledger cannot cause a burst of historical notifications.
 */
private object MigrateV2ToV3 : Migration(2, 3) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `subscriptions` ADD COLUMN `reminders_enabled` INTEGER NOT NULL DEFAULT 1",
        )
        connection.execSQL(
            "ALTER TABLE `obligations` ADD COLUMN `reminders_enabled` INTEGER NOT NULL DEFAULT 1",
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_events` (
                `reminder_key` TEXT NOT NULL,
                `sent_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`reminder_key`)
            )
            """.trimIndent(),
        )
    }
}
