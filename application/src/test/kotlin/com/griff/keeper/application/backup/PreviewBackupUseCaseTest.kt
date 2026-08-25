package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.testing.InMemoryBackupSource
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Opening a file, before anything is written.
 *
 * The point of this stage is that it changes nothing, so most of these tests assert an absence: no
 * records touched, no preference written, and in one case no history entry either.
 */
class PreviewBackupUseCaseTest {

    @Test
    fun `a preview reports what the file contains without writing anything`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "local", name = "Netflix")),
        )
        val payload = fixture.payload(
            subscriptions = List(12) { testSubscription(id = "s-$it", name = "Service $it") },
            obligations = List(6) { testObligation(id = "o-$it", name = "Policy $it") },
        )
        val source = InMemoryBackupSource(
            fixture.codec.register(payload, BackupUseCaseFixture.PASSWORD),
        )

        val result = fixture.previewBackup(source, BackupUseCaseFixture.PASSWORD.toCharArray())

        val preview = result.getOrThrow()
        assertEquals(12, preview.summary.subscriptionCount)
        assertEquals(6, preview.summary.obligationCount)
        assertEquals(BackupUseCaseFixture.APP_VERSION, preview.summary.appVersion)
        assertTrue(preview.hasLocalData)
        assertEquals(1, preview.localRecordCount)

        // Nothing has changed on the device.
        assertEquals(1, fixture.subscriptions.stored.size)
        assertTrue(fixture.settings.writes.isEmpty())
        assertEquals(0, fixture.importRepository.appliedPlans)
    }

    @Test
    fun `an empty device is reported as having nothing to lose`() = runTest {
        val fixture = BackupUseCaseFixture()
        val source = InMemoryBackupSource(
            fixture.codec.register(
                fixture.payload(subscriptions = listOf(testSubscription())),
                BackupUseCaseFixture.PASSWORD,
            ),
        )

        val preview = fixture.previewBackup(
            source,
            BackupUseCaseFixture.PASSWORD.toCharArray(),
        ).getOrThrow()

        assertFalse(preview.hasLocalData)
        assertEquals(0, preview.localRecordCount)
    }

    @Test
    fun `possible duplicates are reported but not acted on`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
        )
        val source = InMemoryBackupSource(
            fixture.codec.register(
                fixture.payload(subscriptions = listOf(testSubscription(id = "B", name = "Spotify"))),
                BackupUseCaseFixture.PASSWORD,
            ),
        )

        val preview = fixture.previewBackup(
            source,
            BackupUseCaseFixture.PASSWORD.toCharArray(),
        ).getOrThrow()

        assertEquals(1, preview.possibleDuplicates)
        assertEquals(1, fixture.subscriptions.stored.size)
    }

    @Test
    fun `a wrong password is refused and is not written to the history`() = runTest {
        val fixture = BackupUseCaseFixture()
        val source = InMemoryBackupSource(
            fixture.codec.register(
                fixture.payload(subscriptions = listOf(testSubscription())),
                BackupUseCaseFixture.PASSWORD,
            ),
        )

        val result = fixture.previewBackup(source, "OtherPassword".toCharArray())

        assertEquals(
            BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED,
            result.exceptionOrNull()?.backupErrorType,
        )
        // A typo the user fixes on the spot is not an event worth keeping: a log full of them stops
        // being a record of what happened to the data.
        assertTrue(fixture.history.recorded.isEmpty())
    }

    @Test
    fun `a file that is not a backup is refused and is recorded`() = runTest {
        val fixture = BackupUseCaseFixture()
        val source = InMemoryBackupSource("just some bytes".toByteArray())

        val result = fixture.previewBackup(source, BackupUseCaseFixture.PASSWORD.toCharArray())

        assertEquals(BackupErrorType.INVALID_FILE, result.exceptionOrNull()?.backupErrorType)
        val entry = fixture.history.recorded.single()
        assertEquals(BackupOperationStatus.FAILED, entry.status)
        assertEquals(BackupErrorType.INVALID_FILE, entry.errorType)
        assertEquals(source.displayName, entry.fileName)
    }

    @Test
    fun `a backup from a newer app is refused and is recorded`() = runTest {
        val fixture = BackupUseCaseFixture()
        fixture.codec.failOnInspect = BackupErrorType.UNSUPPORTED_VERSION
        val source = InMemoryBackupSource("anything".toByteArray())

        val result = fixture.previewBackup(source, BackupUseCaseFixture.PASSWORD.toCharArray())

        assertEquals(
            BackupErrorType.UNSUPPORTED_VERSION,
            result.exceptionOrNull()?.backupErrorType,
        )
        assertEquals(
            BackupErrorType.UNSUPPORTED_VERSION,
            fixture.history.recorded.single().errorType,
        )
    }

    @Test
    fun `an oversized file is refused before the password is asked for`() = runTest {
        val fixture = BackupUseCaseFixture()
        fixture.reader.failOnRead = BackupErrorType.FILE_TOO_LARGE
        val source = InMemoryBackupSource("anything".toByteArray())

        val result = fixture.validateBackupFile(source)

        assertEquals(BackupErrorType.FILE_TOO_LARGE, result.exceptionOrNull()?.backupErrorType)
        // The level-one check does not write to the history: the import proper has not started.
        assertTrue(fixture.history.recorded.isEmpty())
    }

    @Test
    fun `a currency mismatch is reported when the device already holds data`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription()),
            localAppCurrency = Currency.PLN,
        )
        val source = InMemoryBackupSource(
            fixture.codec.register(
                fixture.payload(
                    subscriptions = listOf(testSubscription()),
                    settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
                ),
                BackupUseCaseFixture.PASSWORD,
            ),
        )

        val preview = fixture.previewBackup(
            source,
            BackupUseCaseFixture.PASSWORD.toCharArray(),
        ).getOrThrow()

        assertTrue(preview.currencyMismatch)
        assertEquals(Currency.EUR, preview.summary.appCurrency)
    }

    @Test
    fun `a currency mismatch on an empty device is not reported`() = runTest {
        val fixture = BackupUseCaseFixture(localAppCurrency = Currency.PLN)
        val source = InMemoryBackupSource(
            fixture.codec.register(
                fixture.payload(
                    subscriptions = listOf(testSubscription()),
                    settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
                ),
                BackupUseCaseFixture.PASSWORD,
            ),
        )

        val preview = fixture.previewBackup(
            source,
            BackupUseCaseFixture.PASSWORD.toCharArray(),
        ).getOrThrow()

        // Nothing local to conflict with, so REPLACE-shaped adoption is unambiguous either way.
        assertFalse(preview.currencyMismatch)
    }

    @Test
    fun `the same currency on both sides is not reported as a mismatch`() = runTest {
        val fixture = BackupUseCaseFixture(
            localSubscriptions = listOf(testSubscription()),
            localAppCurrency = Currency.EUR,
        )
        val source = InMemoryBackupSource(
            fixture.codec.register(
                fixture.payload(
                    subscriptions = listOf(testSubscription()),
                    settings = PortableSettings.Default.copy(appCurrency = Currency.EUR),
                ),
                BackupUseCaseFixture.PASSWORD,
            ),
        )

        val preview = fixture.previewBackup(
            source,
            BackupUseCaseFixture.PASSWORD.toCharArray(),
        ).getOrThrow()

        assertFalse(preview.currencyMismatch)
    }

    @Test
    fun `the level one check accepts a real backup`() = runTest {
        val fixture = BackupUseCaseFixture()
        val source = InMemoryBackupSource(
            fixture.codec.register(fixture.payload(), BackupUseCaseFixture.PASSWORD),
        )

        assertTrue(fixture.validateBackupFile(source).isSuccess)
    }
}
