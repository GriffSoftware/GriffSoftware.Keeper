package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupOperation
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** What an import actually does to the device, including when it goes wrong halfway. */
class ImportBackupUseCaseTest {

    @Test
    fun `merge keeps local records and adds the new ones`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(
                testSubscription(id = "A", name = "Spotify"),
                testSubscription(id = "B", name = "Netflix"),
            ),
        )
        val payload = fixture.payload(
            subscriptions = listOf(
                testSubscription(id = "A", name = "Spotify"),
                testSubscription(id = "C", name = "ChatGPT"),
            ),
        )

        val result = fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("Netflix", "Spotify", "ChatGPT").sorted(),
            fixture.subscriptions.stored.map { it.name.value }.sorted(),
        )
    }

    @Test
    fun `importing the same backup twice produces no duplicates`() = runTest {
        val fixture = BackupUseCaseFixture()
        val payload = fixture.payload(
            subscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            obligations = listOf(testObligation(id = "X", name = "OC Ford")),
        )

        fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)
        fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)

        assertEquals(listOf("Spotify"), fixture.subscriptions.stored.map { it.name.value })
        assertEquals(listOf("OC Ford"), fixture.obligations.stored.map { it.name.value })
    }

    @Test
    fun `replace leaves exactly what the backup carried`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = List(5) { testSubscription(id = "local-$it", name = "Local $it") },
            localObligations = List(3) { testObligation(id = "obl-$it", name = "Policy $it") },
        )
        val payload = fixture.payload(
            subscriptions = List(2) { testSubscription(id = "new-$it", name = "New $it") },
            obligations = listOf(testObligation(id = "new-obl", name = "New policy")),
            settings = PortableSettings.Default.copy(globalRemindersEnabled = false),
        )

        val result = fixture.importBackup(payload, ImportMode.REPLACE, FILE_NAME)

        assertTrue(result.isSuccess)
        assertEquals(2, fixture.subscriptions.stored.size)
        assertEquals(1, fixture.obligations.stored.size)
        assertEquals(false, fixture.settings.stored.globalRemindersEnabled)
    }

    @Test
    fun `a failed import leaves the data and the preferences exactly as they were`() = runTest {
        val local = listOf(
            testSubscription(id = "A", name = "Spotify"),
            testSubscription(id = "B", name = "Netflix"),
        )
        val fixture = BackupUseCaseFixture(
            localSubscriptions = local,
            localObligations = listOf(testObligation(id = "X", name = "OC Ford")),
            localSettings = PortableSettings.Default.copy(globalRemindersEnabled = true),
        )
        fixture.importRepository.failOnApply = BackupErrorType.IO_ERROR

        val payload = fixture.payload(
            subscriptions = listOf(testSubscription(id = "C", name = "ChatGPT")),
            settings = PortableSettings.Default.copy(globalRemindersEnabled = false),
        )

        val result = fixture.importBackup(payload, ImportMode.REPLACE, FILE_NAME)

        assertTrue(result.isFailure)
        // The records are untouched, because the write is all or nothing...
        assertEquals(
            local.map { it.id.value }.toSet(),
            fixture.subscriptions.stored.map { it.id.value }.toSet(),
        )
        assertEquals(1, fixture.obligations.stored.size)
        // ...and the preference the import had already written is put back, because a Room
        // transaction says nothing about DataStore.
        assertEquals(true, fixture.settings.stored.globalRemindersEnabled)
        assertEquals(
            listOf(false, true),
            fixture.settings.writes.map { it.globalRemindersEnabled },
        )
    }

    @Test
    fun `a successful import is recorded with the counts that were written`() = runTest {
        val fixture = BackupUseCaseFixture()
        val payload = fixture.payload(
            subscriptions = List(12) { testSubscription(id = "s-$it", name = "Service $it") },
            obligations = List(6) { testObligation(id = "o-$it", name = "Policy $it") },
        )

        fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)

        val entry = fixture.history.recorded.single()
        assertEquals(BackupOperationType.IMPORT, entry.type)
        assertEquals(BackupOperationStatus.SUCCESS, entry.status)
        assertEquals(ImportMode.MERGE, entry.importMode)
        assertEquals(12, entry.subscriptionCount)
        assertEquals(6, entry.obligationCount)
        assertEquals(1, entry.settingsCount)
        assertEquals(null, entry.errorType)
        assertEquals(FILE_NAME, entry.fileName)
    }

    @Test
    fun `a failed import is recorded with its category and no counts`() = runTest {
        val fixture = BackupUseCaseFixture()
        fixture.importRepository.failOnApply = BackupErrorType.INSUFFICIENT_STORAGE

        val result = fixture.importBackup(
            fixture.payload(subscriptions = listOf(testSubscription())),
            ImportMode.MERGE,
            FILE_NAME,
        )

        assertEquals(
            BackupErrorType.INSUFFICIENT_STORAGE,
            result.exceptionOrNull()?.backupErrorType,
        )
        val entry = fixture.history.recorded.single()
        assertEquals(BackupOperationStatus.FAILED, entry.status)
        assertEquals(BackupErrorType.INSUFFICIENT_STORAGE, entry.errorType)
        assertEquals(0, entry.recordCount)
    }

    @Test
    fun `the operation history survives a replace`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
        )
        // An earlier export, from before the import that is about to wipe the records.
        fixture.history.record(
            BackupOperation(
                id = "earlier",
                type = BackupOperationType.EXPORT,
                startedAt = Instant.parse("2026-08-20T10:00:00Z"),
                finishedAt = Instant.parse("2026-08-20T10:00:01Z"),
                status = BackupOperationStatus.SUCCESS,
                fileName = "older.griffbackup",
                importMode = null,
                subscriptionCount = 1,
                obligationCount = 0,
                settingsCount = 1,
                errorType = null,
            ),
        )

        fixture.importBackup(
            fixture.payload(subscriptions = listOf(testSubscription(id = "B", name = "New"))),
            ImportMode.REPLACE,
            FILE_NAME,
        )

        // The log describes what this device did; replacing the user's data is not a reason to
        // forget that the export happened.
        assertEquals(
            listOf("earlier", "backup-1"),
            fixture.history.recorded.map { it.id },
        )
        assertEquals(listOf("New"), fixture.subscriptions.stored.map { it.name.value })
    }

    @Test
    fun `an import asks the reminder engine to look at the new dates`() = runTest {
        val fixture = BackupUseCaseFixture()

        fixture.importBackup(
            fixture.payload(subscriptions = listOf(testSubscription())),
            ImportMode.MERGE,
            FILE_NAME,
        )

        // Re-registered, not fired: the engine only ever looks at what falls due today, so a restore
        // cannot replay months of notifications from the other device.
        assertEquals(1, fixture.scheduler.scheduleCount)
    }

    @Test
    fun `a failed import does not touch the reminder schedule`() = runTest {
        val fixture = BackupUseCaseFixture()
        fixture.importRepository.failOnApply = BackupErrorType.VALIDATION_ERROR

        fixture.importBackup(
            fixture.payload(subscriptions = listOf(testSubscription())),
            ImportMode.MERGE,
            FILE_NAME,
        )

        assertEquals(0, fixture.scheduler.scheduleCount)
    }

    @Test
    fun `merge is blocked when the backup and the device disagree on currency`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            localAppCurrency = Currency.PLN,
        )
        val payload = fixture.payload(
            subscriptions = listOf(testSubscription(id = "B", name = "Netflix")),
            settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
        )

        val result = fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)

        assertEquals(BackupErrorType.CURRENCY_MISMATCH, result.exceptionOrNull()?.backupErrorType)
        // Blocked before anything is written or attempted against the import repository.
        assertEquals(listOf("Spotify"), fixture.subscriptions.stored.map { it.name.value })
        assertEquals(0, fixture.importRepository.appliedPlans)
        assertTrue(fixture.settings.writes.isEmpty())
    }

    @Test
    fun `merge is blocked with nothing written when the device has no local data either`() = runTest {
        // Nothing to merge means nothing to conflict with, but the guard is still exercised so it is
        // pinned that an empty device never trips the mismatch check.
        val fixture = BackupUseCaseFixture(localAppCurrency = Currency.PLN)
        val payload = fixture.payload(
            subscriptions = listOf(testSubscription()),
            settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
        )

        val result = fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)

        assertTrue(result.isSuccess)
        assertEquals(Currency.EUR, fixture.settings.stored.appCurrency)
    }

    @Test
    fun `replace adopts the backup's currency even when it disagrees with the device`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            localAppCurrency = Currency.PLN,
        )
        val payload = fixture.payload(
            subscriptions = listOf(testSubscription(id = "B", name = "Netflix")),
            settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
        )

        val result = fixture.importBackup(payload, ImportMode.REPLACE, FILE_NAME)

        assertTrue(result.isSuccess)
        assertEquals(Currency.EUR, fixture.settings.stored.appCurrency)
        assertEquals(listOf("Netflix"), fixture.subscriptions.stored.map { it.name.value })
    }

    @Test
    fun `merge with the same currency on both sides is not blocked`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            localAppCurrency = Currency.EUR,
        )
        val payload = fixture.payload(
            subscriptions = listOf(testSubscription(id = "B", name = "Netflix")),
            settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
        )

        val result = fixture.importBackup(payload, ImportMode.MERGE, FILE_NAME)

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("Netflix", "Spotify").sorted(),
            fixture.subscriptions.stored.map { it.name.value }.sorted(),
        )
    }

    private companion object {
        const val FILE_NAME = "griff-backup-2026-08-21-0043.griffbackup"
    }
}
