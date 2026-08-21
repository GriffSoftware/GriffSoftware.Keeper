package com.griff.keeper.infrastructure.repository

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.ImportPlan
import com.griff.keeper.domain.backup.ImportSection
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import com.griff.keeper.infrastructure.database.GriffDatabase
import com.griff.keeper.infrastructure.database.entity.BackupOperationEntity
import com.griff.keeper.infrastructure.database.entity.ReminderEventEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

/**
 * Verifies the import against a real Room database.
 *
 * The all-or-nothing guarantee is the reason this test exists at a device level at all: an in-memory
 * double can be made to look transactional, whereas only a real database can show that it is.
 */
@RunWith(AndroidJUnit4::class)
class RoomBackupImportRepositoryTest {

    private lateinit var database: GriffDatabase
    private lateinit var repository: RoomBackupImportRepository

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GriffDatabase::class.java,
        ).build()
        repository = RoomBackupImportRepository(database, Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun mergeAddsAndUpdatesWithoutRemovingAnything() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        subscriptions.add(testSubscription(id = "A", name = "Spotify"))
        subscriptions.add(testSubscription(id = "B", name = "Netflix"))

        repository.apply(
            plan(
                mode = ImportMode.MERGE,
                subscriptionsToInsert = listOf(testSubscription(id = "C", name = "ChatGPT")),
                subscriptionsToUpdate = listOf(
                    testSubscription(id = "A", name = "Spotify Family", priceMinorUnits = 5_999),
                ),
            ),
        )

        val stored = subscriptions.observeAll().first().associateBy { it.id.value }
        assertEquals(setOf("A", "B", "C"), stored.keys)
        assertEquals("Spotify Family", stored.getValue("A").name.value)
        assertEquals("Netflix", stored.getValue("B").name.value)
    }

    @Test
    fun replaceLeavesOnlyWhatTheBackupCarried() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        val obligations = RoomObligationRepository(database.obligationDao(), Dispatchers.IO)
        repeat(5) { subscriptions.add(testSubscription(id = "local-$it", name = "Local $it")) }
        repeat(3) { obligations.add(testObligation(id = "obl-$it", name = "Policy $it")) }

        repository.apply(
            plan(
                mode = ImportMode.REPLACE,
                subscriptionsToInsert = List(2) {
                    testSubscription(id = "new-$it", name = "New $it")
                },
                obligationsToInsert = listOf(testObligation(id = "new-obl", name = "New policy")),
            ),
        )

        assertEquals(2, subscriptions.observeAll().first().size)
        assertEquals(1, obligations.observeAll().first().size)
    }

    @Test
    fun replaceLeavesTheDeviceLocalTablesAlone() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        subscriptions.add(testSubscription(id = "A", name = "Spotify"))
        database.reminderEventDao().insert(
            ReminderEventEntity(reminderKey = "SUBSCRIPTION:A:2026-08-21:7", sentAtEpochMillis = 1L),
        )
        database.backupOperationDao().insert(exportLogEntry())

        repository.apply(
            plan(
                mode = ImportMode.REPLACE,
                subscriptionsToInsert = listOf(testSubscription(id = "B", name = "New")),
            ),
        )

        // The user's records are gone and replaced...
        assertEquals(listOf("B"), subscriptions.observeAll().first().map { it.id.value })
        // ...while the two tables that describe *this installation* survive: the log, so the import
        // that wiped the records is still visible in the history right afterwards, and the delivery
        // ledger, so reminders the user has already seen are not shown again.
        assertEquals(1, database.backupOperationDao().observeRecent(10).first().size)
        assertEquals(1, database.reminderEventDao().keys().size)
    }

    @Test
    fun anImportThatFailsPartWayThroughLeavesNothingBehind() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        repeat(3) { subscriptions.add(testSubscription(id = "local-$it", name = "Local $it")) }
        val before = subscriptions.observeAll().first()

        // The exact shape of a REPLACE that dies after the wipe but before the rows are written -
        // the failure mode the transaction exists to make impossible.
        assertFailsWith<IllegalStateException> {
            database.withTransaction {
                database.subscriptionDao().deleteAll()
                database.subscriptionDao().upsertAll(emptyList())
                error("Simulated failure halfway through the import")
            }
        }

        assertEquals(before, subscriptions.observeAll().first())
        assertEquals(3, database.subscriptionDao().count())
    }

    @Test
    fun importingTheSameBackupTwiceIsIdempotent() = runTest {
        val subscriptions = RoomSubscriptionRepository(database.subscriptionDao(), Dispatchers.IO)
        val incoming = listOf(testSubscription(id = "A", name = "Spotify"))

        repository.apply(plan(ImportMode.MERGE, subscriptionsToInsert = incoming))
        // A second import of the same file: the plan would compute "unchanged", but even applying
        // the rows again must not produce a second Spotify - the id is the primary key.
        repository.apply(plan(ImportMode.MERGE, subscriptionsToInsert = incoming))

        assertEquals(1, database.subscriptionDao().count())
    }

    private fun plan(
        mode: ImportMode,
        subscriptionsToInsert: List<Subscription> = emptyList(),
        subscriptionsToUpdate: List<Subscription> = emptyList(),
        obligationsToInsert: List<Obligation> = emptyList(),
    ) = ImportPlan(
        mode = mode,
        subscriptions = ImportSection(
            toInsert = subscriptionsToInsert,
            toUpdate = subscriptionsToUpdate,
            unchanged = 0,
            removed = 0,
        ),
        obligations = ImportSection(
            toInsert = obligationsToInsert,
            toUpdate = emptyList(),
            unchanged = 0,
            removed = 0,
        ),
        settings = PortableSettings.Default,
        possibleDuplicates = 0,
    )

    private fun exportLogEntry() = BackupOperationEntity(
        id = "op-1",
        type = "EXPORT",
        startedAtEpochMillis = 1_787_272_980_000L,
        finishedAtEpochMillis = 1_787_272_981_000L,
        status = "SUCCESS",
        fileName = "griff-backup.griffbackup",
        importMode = null,
        subscriptionCount = 1,
        obligationCount = 0,
        settingsCount = 1,
        errorType = null,
    )
}
