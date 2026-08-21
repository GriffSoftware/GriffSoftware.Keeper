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
 * Verifies that adding the import/export log keeps everything that was already there.
 *
 * A class of its own with its own database file, for the same reason as the reminder migration test:
 * [MigrationTestHelper] reuses one file per class.
 */
@RunWith(AndroidJUnit4::class)
class GriffDatabaseBackupMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getDatabasePath("backup-migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = GriffDatabase::class,
    )

    @Test
    fun migratingToVersion4KeepsRecordsAndAddsAnEmptyLog() {
        helper.createDatabase(version = 3).use { connection ->
            connection.execSQL(
                """
                INSERT INTO subscriptions (
                    id, provider_id, name, category, price_minor_units, currency_code,
                    billing_period, management_url, next_billing_date_epoch_day,
                    reminders_enabled, created_at_epoch_millis, updated_at_epoch_millis
                ) VALUES (
                    'id-1', 'netflix', 'Netflix', NULL, 6700, 'PLN', 'MONTHLY',
                    NULL, 20693, 1, 1767225600000, 1767225600000
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO reminder_events (reminder_key, sent_at_epoch_millis)
                VALUES ('SUBSCRIPTION:id-1:2026-08-21:7', 1767225600000)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(version = 4, migrations = DatabaseMigrations.toList())
            .use { connection ->
                connection.prepare("SELECT name FROM subscriptions").use { statement ->
                    assertTrue(statement.step(), "the migrated database lost its subscription")
                    assertEquals("Netflix", statement.getText(0))
                }

                // The delivery ledger is untouched: it says what this device has already shown the
                // user, and an update is not a reason to say any of it again.
                connection.prepare("SELECT COUNT(*) FROM reminder_events").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(1L, statement.getLong(0))
                }

                // Nothing has been imported or exported on this installation yet, and no update
                // could bring history over from anywhere, so the log starts empty.
                connection.prepare("SELECT COUNT(*) FROM backup_operations").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(0L, statement.getLong(0))
                }

                connection.execSQL(
                    """
                    INSERT INTO backup_operations (
                        id, type, started_at_epoch_millis, finished_at_epoch_millis, status,
                        file_name, import_mode, subscription_count, obligation_count,
                        settings_count, error_type
                    ) VALUES (
                        'op-1', 'EXPORT', 1787272980000, 1787272981000, 'SUCCESS',
                        'griff-backup.griffbackup', NULL, 1, 0, 1, NULL
                    )
                    """.trimIndent(),
                )
                connection.prepare("SELECT status, error_type FROM backup_operations")
                    .use { statement ->
                        assertTrue(statement.step())
                        assertEquals("SUCCESS", statement.getText(0))
                        assertTrue(statement.isNull(1))
                    }
            }
    }
}
