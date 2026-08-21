package com.griff.keeper.infrastructure.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Verifies that adding reminders keeps the user's data.
 *
 * A class of its own, with its own database file: [MigrationTestHelper] reuses one file per class,
 * so a test that leaves the database at version 3 cannot share it with one that has to create a
 * version 1 database from scratch.
 */
@RunWith(AndroidJUnit4::class)
class GriffDatabaseReminderMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getDatabasePath("reminder-migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = GriffDatabase::class,
    )

    /**
     * The reminder columns arrive with a default, which is the whole point of the migration: a user
     * who updates the app keeps every record *and* starts getting reminders for them, without having
     * to open and re-save anything.
     */
    @Test
    fun migratingToVersion3KeepsRecordsAndOptsThemIntoReminders() {
        helper.createDatabase(version = 2).use { connection ->
            connection.execSQL(
                """
                INSERT INTO subscriptions (
                    id, provider_id, name, category, price_minor_units, currency_code,
                    billing_period, management_url, next_billing_date_epoch_day,
                    created_at_epoch_millis, updated_at_epoch_millis
                ) VALUES (
                    'id-1', 'netflix', 'Netflix', NULL, 6700, 'PLN', 'MONTHLY',
                    NULL, 20693, 1767225600000, 1767225600000
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO obligations (
                    id, name, category, amount_minor_units, currency_code, payment_status,
                    payment_date_epoch_day, due_date_epoch_day, valid_until_epoch_day, notes,
                    created_at_epoch_millis, updated_at_epoch_millis
                ) VALUES (
                    'o-1', 'OC Ford', 'VEHICLE_INSURANCE', 124000, 'PLN', 'PAID',
                    20524, NULL, 20889, NULL, 1767225600000, 1767225600000
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(version = 3, migrations = DatabaseMigrations.toList())
            .use { connection ->
                connection.prepare("SELECT name, reminders_enabled FROM subscriptions")
                    .use { statement ->
                        assertTrue(statement.step(), "the migrated database lost its subscription")
                        assertEquals("Netflix", statement.getText(0))
                        assertEquals(1L, statement.getLong(1))
                    }

                connection.prepare("SELECT name, reminders_enabled FROM obligations")
                    .use { statement ->
                        assertTrue(statement.step(), "the migrated database lost its obligation")
                        assertEquals("OC Ford", statement.getText(0))
                        assertEquals(1L, statement.getLong(1))
                    }

                // The deduplication ledger starts empty, which is what stops an update from
                // replaying reminders the user has already had - or never needed.
                connection.prepare("SELECT COUNT(*) FROM reminder_events").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(0L, statement.getLong(0))
                }

                connection.execSQL(
                    """
                    INSERT INTO reminder_events (reminder_key, sent_at_epoch_millis)
                    VALUES ('OBLIGATION:o-1:2027-03-11:30', 1767225600000)
                    """.trimIndent(),
                )
                // The key is the primary key, so the database itself refuses a duplicate.
                connection.prepare(
                    "SELECT COUNT(*) FROM reminder_events WHERE reminder_key = ?",
                ).use { statement ->
                    statement.bindText(1, "OBLIGATION:o-1:2027-03-11:30")
                    assertTrue(statement.step())
                    assertEquals(1L, statement.getLong(0))
                }
            }
    }
}
