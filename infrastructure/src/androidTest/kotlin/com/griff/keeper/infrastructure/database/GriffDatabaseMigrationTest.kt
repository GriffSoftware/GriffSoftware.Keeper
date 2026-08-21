package com.griff.keeper.infrastructure.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Verifies that updating the app keeps the user's data.
 *
 * The obligations feature raises the schema version, so the migration is exercised against a real
 * version 1 database with a row in it: an update must never look like data loss.
 */
@RunWith(AndroidJUnit4::class)
class GriffDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getDatabasePath("migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = GriffDatabase::class,
    )

    /**
     * One scenario end to end: an existing database is migrated, keeps its rows, and can then store
     * the new kind of record. Split into several tests it would need several database files, since
     * the helper reuses one per class.
     */
    @Test
    fun migratingToVersion2KeepsSubscriptionsAndAcceptsObligations() {
        helper.createDatabase(version = 1).use { connection ->
            connection.execSQL(
                """
                INSERT INTO subscriptions (
                    id, provider_id, name, price_minor_units, currency_code, billing_period,
                    management_url, next_billing_date_epoch_day, created_at_epoch_millis,
                    updated_at_epoch_millis
                ) VALUES (
                    'id-1', 'spotify', 'Spotify', 3499, 'PLN', 'MONTHLY',
                    'https://spotify.com/account', 20730, 1767225600000, 1767225600000
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(version = 2, migrations = DatabaseMigrations.toList())
            .use { connection ->
                connection.prepare("SELECT name, price_minor_units, category FROM subscriptions")
                    .use { statement ->
                        assertTrue(statement.step(), "the migrated database lost its only row")
                        assertEquals("Spotify", statement.getText(0))
                        assertEquals(3499L, statement.getLong(1))
                        // A catalog entry takes its category from the catalog, so the new column
                        // stays empty for rows that existed before the feature.
                        assertTrue(statement.isNull(2))
                        assertTrue(!statement.step(), "the migration duplicated a row")
                    }

                connection.prepare("SELECT COUNT(*) FROM obligations").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(0L, statement.getLong(0))
                }

                connection.execSQL(
                    """
                    INSERT INTO obligations (
                        id, name, category, amount_minor_units, currency_code, payment_status,
                        payment_date_epoch_day, due_date_epoch_day, valid_until_epoch_day, notes,
                        created_at_epoch_millis, updated_at_epoch_millis
                    ) VALUES (
                        'o-1', 'OC Ford', 'VEHICLE_INSURANCE', 124000, 'PLN', 'PAID',
                        20524, NULL, 20889, 'Polisa PZU', 1767225600000, 1767225600000
                    )
                    """.trimIndent(),
                )

                connection.prepare("SELECT name, payment_status, notes FROM obligations")
                    .use { statement ->
                        assertTrue(statement.step())
                        assertEquals("OC Ford", statement.getText(0))
                        assertEquals("PAID", statement.getText(1))
                        assertEquals("Polisa PZU", statement.getText(2))
                    }

                // The subscription is still there after the new table was written to.
                connection.prepare("SELECT COUNT(*) FROM subscriptions").use { statement ->
                    assertTrue(statement.step())
                    assertEquals(1L, statement.getLong(0))
                }
            }
    }

    @Test
    fun theMigrationListCoversEveryVersionUpToTheCurrentOne() {
        val versions = DatabaseMigrations.map { it.startVersion to it.endVersion }

        assertEquals(
            (1 until GriffDatabase.VERSION).map { it to it + 1 },
            versions,
            "every version bump needs a migration; destructive fallbacks are not an option",
        )
        assertNull(versions.firstOrNull { it.first >= it.second })
    }
}
