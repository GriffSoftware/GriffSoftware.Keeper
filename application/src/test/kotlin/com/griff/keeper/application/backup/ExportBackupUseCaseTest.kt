package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.testing.InMemoryBackupSink
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** What ends up in the file, in the history, and what happens when nothing does. */
class ExportBackupUseCaseTest {

    @Test
    fun `an export writes the file and records what it contained`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = List(12) { testSubscription(id = "s-$it", name = "Service $it") },
            localObligations = List(6) { testObligation(id = "o-$it", name = "Policy $it") },
        )
        val sink = InMemoryBackupSink()

        val result = fixture.exportBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
            sink,
        )

        assertTrue(result.isSuccess)
        assertEquals(12, result.getOrNull()?.summary?.subscriptionCount)
        assertEquals(6, result.getOrNull()?.summary?.obligationCount)
        assertTrue(sink.bytes.isNotEmpty())

        val entry = fixture.history.recorded.single()
        assertEquals(BackupOperationType.EXPORT, entry.type)
        assertEquals(BackupOperationStatus.SUCCESS, entry.status)
        assertEquals(12, entry.subscriptionCount)
        assertEquals(6, entry.obligationCount)
        assertNull(entry.importMode)
        assertNull(entry.errorType)
    }

    @Test
    fun `the exported payload carries the portable preferences`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription()),
            localSettings = PortableSettings.Default.copy(globalRemindersEnabled = false),
        )

        fixture.exportBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
            InMemoryBackupSink(),
        )

        val exported = fixture.codec.decode(
            requireNotNull(fixture.writer.written),
            BackupUseCaseFixture.PASSWORD.toCharArray(),
        )
        assertEquals(false, exported.settings.globalRemindersEnabled)
        assertEquals(BackupUseCaseFixture.APP_VERSION, exported.appVersion)
    }

    @Test
    fun `nothing is written when the destination cannot be opened`() = runTest {
        val fixture = BackupUseCaseFixture(localSubscriptions = listOf(testSubscription()))
        fixture.writer.failOnWrite = BackupErrorType.IO_ERROR

        val result = fixture.exportBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
            InMemoryBackupSink(),
        )

        assertTrue(result.isFailure)
        assertNull(fixture.writer.written)
        val entry = fixture.history.recorded.single()
        assertEquals(BackupOperationStatus.FAILED, entry.status)
        assertEquals(BackupErrorType.IO_ERROR, entry.errorType)
    }

    @Test
    fun `a full disk is reported as a storage problem`() = runTest {
        val fixture = BackupUseCaseFixture(localSubscriptions = listOf(testSubscription()))
        fixture.writer.failOnWrite = BackupErrorType.INSUFFICIENT_STORAGE

        fixture.exportBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
            InMemoryBackupSink(),
        )

        assertEquals(
            BackupErrorType.INSUFFICIENT_STORAGE,
            fixture.history.recorded.single().errorType,
        )
    }

    @Test
    fun `an export of an empty device still produces a restorable file`() = runTest {
        val fixture = BackupUseCaseFixture()
        val sink = InMemoryBackupSink()

        val result = fixture.exportBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
            sink,
        )

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.summary?.recordCount)
        assertTrue(result.getOrNull()?.summary?.hasSettings == true)
    }

    @Test
    fun `sharing stages one file and clears whatever was there before`() = runTest {
        val fixture = BackupUseCaseFixture(localSubscriptions = listOf(testSubscription()))

        val first = fixture.shareBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
        )
        val second = fixture.shareBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
        )

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        // Cleared before each staging, so a cache of old encrypted backups cannot build up.
        assertEquals(2, fixture.sharing.clearCount)
        assertEquals(FILE_NAME, fixture.sharing.staged?.fileName)
        assertEquals(2, fixture.history.recorded.size)
    }

    @Test
    fun `a share that cannot be staged is recorded as a failed export`() = runTest {
        val fixture = BackupUseCaseFixture(localSubscriptions = listOf(testSubscription()))
        fixture.sharing.failOnStage = BackupErrorType.INSUFFICIENT_STORAGE

        val result = fixture.shareBackup(
            BackupUseCaseFixture.PASSWORD.toCharArray(),
            FILE_NAME,
        )

        assertTrue(result.isFailure)
        val entry = fixture.history.recorded.single()
        assertEquals(BackupOperationType.EXPORT, entry.type)
        assertEquals(BackupOperationStatus.FAILED, entry.status)
        assertEquals(BackupErrorType.INSUFFICIENT_STORAGE, entry.errorType)
    }

    private companion object {
        const val FILE_NAME = "griff-backup-2026-08-21-0043.griffbackup"
    }
}
