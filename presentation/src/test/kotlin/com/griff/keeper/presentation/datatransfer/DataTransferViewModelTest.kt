package com.griff.keeper.presentation.datatransfer

import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.application.appinfo.AppVersionProvider
import com.griff.keeper.application.appinfo.GetAppVersionUseCase
import com.griff.keeper.application.backup.BackupOperationRecorder
import com.griff.keeper.application.backup.ClearSharedBackupsUseCase
import com.griff.keeper.application.backup.CollectBackupPayloadUseCase
import com.griff.keeper.application.backup.ExportBackupUseCase
import com.griff.keeper.application.backup.ImportBackupUseCase
import com.griff.keeper.application.backup.IsNetworkAvailableUseCase
import com.griff.keeper.application.backup.ObserveBackupHistoryUseCase
import com.griff.keeper.application.backup.PreviewBackupUseCase
import com.griff.keeper.application.backup.ShareBackupUseCase
import com.griff.keeper.application.backup.ValidateBackupFileUseCase
import com.griff.keeper.application.reminder.EnsureRemindersScheduledUseCase
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.testing.FakeBackupCodec
import com.griff.keeper.domain.testing.FakeBackupFileReader
import com.griff.keeper.domain.testing.FakeBackupFileSharing
import com.griff.keeper.domain.testing.FakeBackupFileWriter
import com.griff.keeper.domain.testing.FakeBackupImportRepository
import com.griff.keeper.domain.testing.FakeBackupOperationRepository
import com.griff.keeper.domain.testing.FakeNetworkAvailability
import com.griff.keeper.domain.testing.FakeObligationRepository
import com.griff.keeper.domain.testing.FakePortableSettingsRepository
import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.FixedClockProvider
import com.griff.keeper.domain.testing.InMemoryBackupSink
import com.griff.keeper.domain.testing.InMemoryBackupSource
import com.griff.keeper.domain.testing.RecordingReminderScheduler
import com.griff.keeper.domain.testing.SequentialBackupOperationIdGenerator
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.util.MainDispatcherRule
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/**
 * The screen's decisions: what happens next, what is disabled while it happens, and what the user is
 * told afterwards.
 *
 * Wired against the real use cases with fake ports, so the flows under test are the ones that run in
 * the app rather than a re-implementation of them in the test.
 */
class DataTransferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // --- Export --------------------------------------------------------------------------

    @Test
    fun `exporting asks for a password before opening the picker`() = runTest {
        val fixture = fixture()

        fixture.viewModel.onExportRequested()

        assertEquals(DataTransferDialog.EXPORT_PASSWORD, fixture.viewModel.uiState.value.dialog)
    }

    @Test
    fun `confirming the password requests the system document picker`() = runTest {
        val fixture = fixture()
        val recorded = fixture.recordEvents(this)

        fixture.viewModel.onExportRequested()
        fixture.viewModel.onExportPasswordConfirmed(PASSWORD.toCharArray())
        advanceUntilIdle()

        val event = recorded.events.filterIsInstance<DataTransferEvent.CreateDocument>().single()
        assertTrue(event.suggestedFileName.endsWith(".${BackupFormat.FILE_EXTENSION}"))
        assertEquals(DataTransferDialog.NONE, fixture.viewModel.uiState.value.dialog)
        recorded.stop()
    }

    @Test
    fun `a successful export reports success and appears in the history`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))

        fixture.viewModel.onExportRequested()
        fixture.viewModel.onExportPasswordConfirmed(PASSWORD.toCharArray())
        fixture.viewModel.onExportDestinationChosen(InMemoryBackupSink(), FILE_NAME)
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(DataTransferStage.IDLE, state.stage)
        assertEquals(R.string.data_transfer_export_success, state.message?.textRes)
        assertEquals(MessageSeverity.SUCCESS, state.message?.severity)
        assertEquals(1, state.history.size)
        assertTrue(state.history.single().isSuccess)
    }

    @Test
    fun `cancelling the picker is not an error and writes no history`() = runTest {
        val fixture = fixture()

        fixture.viewModel.onExportRequested()
        fixture.viewModel.onExportPasswordConfirmed(PASSWORD.toCharArray())
        fixture.viewModel.onExportCancelled()
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(DataTransferStage.IDLE, state.stage)
        assertNull(state.message)
        assertTrue(fixture.history.recorded.isEmpty())
    }

    @Test
    fun `a picker result without a password does not silently export`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))

        // What a process death during the picker looks like: a destination arrives, the password is
        // gone. Guessing would be worse than saying it did not finish.
        fixture.viewModel.onExportDestinationChosen(InMemoryBackupSink(), FILE_NAME)
        advanceUntilIdle()

        assertEquals(
            R.string.data_transfer_error_export_failed,
            fixture.viewModel.uiState.value.message?.textRes,
        )
        assertNull(fixture.writer.written)
    }

    @Test
    fun `a failed export reports a failure and records one`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))
        fixture.writer.failOnWrite = BackupErrorType.INSUFFICIENT_STORAGE

        fixture.viewModel.onExportRequested()
        fixture.viewModel.onExportPasswordConfirmed(PASSWORD.toCharArray())
        fixture.viewModel.onExportDestinationChosen(InMemoryBackupSink(), FILE_NAME)
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(R.string.data_transfer_error_no_space_export, state.message?.textRes)
        assertEquals(MessageSeverity.ERROR, state.message?.severity)
        assertEquals(
            BackupOperationStatus.FAILED,
            fixture.history.recorded.single().status,
        )
    }

    // --- Sharing -------------------------------------------------------------------------

    @Test
    fun `sharing while online goes straight to the chooser`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))
        val recorded = fixture.recordEvents(this)

        fixture.viewModel.onShareRequested()
        fixture.viewModel.onSharePasswordConfirmed(PASSWORD.toCharArray(), "someone@example.com")
        advanceUntilIdle()

        val event = recorded.events.filterIsInstance<DataTransferEvent.ShareBackup>().single()
        assertEquals("someone@example.com", event.recipient)
        assertEquals(DataTransferDialog.NONE, fixture.viewModel.uiState.value.dialog)
        recorded.stop()
    }

    @Test
    fun `sharing while offline warns first and continues when the user insists`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))
        fixture.network.online = false
        val recorded = fixture.recordEvents(this)

        fixture.viewModel.onShareRequested()
        fixture.viewModel.onSharePasswordConfirmed(PASSWORD.toCharArray(), null)
        advanceUntilIdle()

        // The file exists either way; the warning is about the message leaving.
        assertEquals(
            DataTransferDialog.OFFLINE_SHARE_WARNING,
            fixture.viewModel.uiState.value.dialog,
        )
        assertTrue(recorded.events.none { it is DataTransferEvent.ShareBackup })
        assertNotNull(fixture.sharing.staged)

        fixture.viewModel.onOfflineShareConfirmed()
        advanceUntilIdle()

        assertEquals(1, recorded.events.count { it is DataTransferEvent.ShareBackup })
        recorded.stop()
    }

    @Test
    fun `cancelling the offline warning drops the staged file`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))
        fixture.network.online = false

        fixture.viewModel.onShareRequested()
        fixture.viewModel.onSharePasswordConfirmed(PASSWORD.toCharArray(), null)
        advanceUntilIdle()
        fixture.viewModel.onOfflineShareCancelled()
        advanceUntilIdle()

        assertNull(fixture.sharing.staged)
        assertEquals(DataTransferDialog.NONE, fixture.viewModel.uiState.value.dialog)
    }

    @Test
    fun `no application to share with is explained instead of crashing`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))

        fixture.viewModel.onShareRequested()
        fixture.viewModel.onSharePasswordConfirmed(PASSWORD.toCharArray(), null)
        advanceUntilIdle()
        fixture.viewModel.onShareTargetUnavailable()
        advanceUntilIdle()

        assertTrue(fixture.viewModel.uiState.value.shareUnavailable)
        // The staged copy is dropped: nothing is going to read it.
        assertNull(fixture.sharing.staged)
    }

    // --- Import --------------------------------------------------------------------------

    @Test
    fun `a picked file is checked before the password is asked for`() = runTest {
        val fixture = fixture()
        val source = InMemoryBackupSource(fixture.register(payload()))

        fixture.viewModel.onImportFileChosen(source)
        advanceUntilIdle()

        assertEquals(DataTransferDialog.IMPORT_PASSWORD, fixture.viewModel.uiState.value.dialog)
    }

    @Test
    fun `a file that is not a backup is refused without asking for a password`() = runTest {
        val fixture = fixture()

        fixture.viewModel.onImportFileChosen(InMemoryBackupSource("a photo".toByteArray()))
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(DataTransferDialog.NONE, state.dialog)
        assertEquals(R.string.data_transfer_error_invalid_file, state.message?.textRes)
    }

    @Test
    fun `a correct password opens the preview without changing any data`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription(id = "local")))
        val source = InMemoryBackupSource(
            fixture.register(
                payload(
                    subscriptions = List(12) { testSubscription(id = "s-$it", name = "S$it") },
                    obligations = List(6) { testObligation(id = "o-$it", name = "O$it") },
                ),
            ),
        )

        fixture.viewModel.onImportFileChosen(source)
        advanceUntilIdle()
        fixture.viewModel.onImportPasswordConfirmed(PASSWORD.toCharArray())
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(DataTransferDialog.IMPORT_PREVIEW, state.dialog)
        assertEquals(12, state.preview?.subscriptionCount)
        assertEquals(6, state.preview?.obligationCount)
        assertTrue(state.preview?.hasLocalData == true)
        assertEquals(1, fixture.subscriptions.stored.size)
    }

    @Test
    fun `a wrong password keeps the dialog open with the file still selected`() = runTest {
        val fixture = fixture()
        val source = InMemoryBackupSource(fixture.register(payload()))

        fixture.viewModel.onImportFileChosen(source)
        advanceUntilIdle()
        fixture.viewModel.onImportPasswordConfirmed("Wrong".toCharArray())
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(DataTransferDialog.IMPORT_PASSWORD, state.dialog)
        assertEquals(R.string.data_transfer_import_wrong_password, state.passwordError?.textRes)
        // No snackbar and no history entry: this is a typo being corrected in place.
        assertNull(state.message)
        assertTrue(fixture.history.recorded.isEmpty())

        // And the retry works, because the file was never let go of.
        fixture.viewModel.onImportPasswordConfirmed(PASSWORD.toCharArray())
        advanceUntilIdle()
        assertEquals(
            DataTransferDialog.IMPORT_PREVIEW,
            fixture.viewModel.uiState.value.dialog,
        )
    }

    @Test
    fun `replace needs a second confirmation and cancelling it loses nothing`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription(id = "local")))
        val source = InMemoryBackupSource(
            fixture.register(payload(subscriptions = listOf(testSubscription(id = "new")))),
        )

        fixture.viewModel.onImportFileChosen(source)
        advanceUntilIdle()
        fixture.viewModel.onImportPasswordConfirmed(PASSWORD.toCharArray())
        advanceUntilIdle()

        fixture.viewModel.onReplaceSelected()
        assertEquals(
            DataTransferDialog.REPLACE_CONFIRMATION,
            fixture.viewModel.uiState.value.dialog,
        )

        fixture.viewModel.onReplaceCancelled()
        advanceUntilIdle()

        // Back to the preview, not out of the flow, and nothing was written.
        assertEquals(DataTransferDialog.IMPORT_PREVIEW, fixture.viewModel.uiState.value.dialog)
        assertEquals(listOf("local"), fixture.subscriptions.stored.map { it.id.value })
        assertEquals(0, fixture.importRepository.appliedPlans)
    }

    @Test
    fun `confirming replace applies the import and says so`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription(id = "local")))
        val source = InMemoryBackupSource(
            fixture.register(payload(subscriptions = listOf(testSubscription(id = "new")))),
        )

        fixture.viewModel.onImportFileChosen(source)
        advanceUntilIdle()
        fixture.viewModel.onImportPasswordConfirmed(PASSWORD.toCharArray())
        advanceUntilIdle()
        fixture.viewModel.onReplaceSelected()
        fixture.viewModel.onReplaceConfirmed()
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(
            R.string.data_transfer_import_replace_success,
            state.message?.textRes,
        )
        assertEquals(listOf("new"), fixture.subscriptions.stored.map { it.id.value })
        assertNull(state.preview)
    }

    @Test
    fun `merging reports the merge message`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription(id = "local")))
        val source = InMemoryBackupSource(
            fixture.register(payload(subscriptions = listOf(testSubscription(id = "new")))),
        )

        fixture.viewModel.onImportFileChosen(source)
        advanceUntilIdle()
        fixture.viewModel.onImportPasswordConfirmed(PASSWORD.toCharArray())
        advanceUntilIdle()
        fixture.viewModel.onMergeSelected()
        advanceUntilIdle()

        assertEquals(
            R.string.data_transfer_import_merge_success,
            fixture.viewModel.uiState.value.message?.textRes,
        )
        assertEquals(
            setOf("local", "new"),
            fixture.subscriptions.stored.map { it.id.value }.toSet(),
        )
    }

    @Test
    fun `cancelling the import picker changes nothing`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription(id = "local")))

        fixture.viewModel.onImportCancelled()
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertEquals(DataTransferStage.IDLE, state.stage)
        assertNull(state.message)
        assertTrue(fixture.history.recorded.isEmpty())
    }

    // --- Nothing overlaps ---------------------------------------------------------------

    @Test
    fun `controls are disabled while an operation is running`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))

        fixture.viewModel.onExportRequested()
        fixture.viewModel.onExportPasswordConfirmed(PASSWORD.toCharArray())
        fixture.viewModel.onExportDestinationChosen(InMemoryBackupSink(), FILE_NAME)

        // Still in flight: the coroutine has not been allowed to run yet.
        val busy = fixture.viewModel.uiState.value
        assertEquals(DataTransferStage.EXPORTING, busy.stage)
        assertTrue(busy.isBusy)
        assertFalse(busy.areActionsEnabled)

        advanceUntilIdle()
        assertTrue(fixture.viewModel.uiState.value.areActionsEnabled)
    }

    @Test
    fun `a second export cannot start while the first is running`() = runTest {
        val fixture = fixture(localSubscriptions = listOf(testSubscription()))
        val recorded = fixture.recordEvents(this)

        fixture.viewModel.onExportRequested()
        fixture.viewModel.onExportPasswordConfirmed(PASSWORD.toCharArray())
        fixture.viewModel.onExportDestinationChosen(InMemoryBackupSink(), FILE_NAME)

        // A double tap while the export is in flight.
        fixture.viewModel.onExportRequested()
        fixture.viewModel.onShareRequested()
        fixture.viewModel.onImportRequested()
        advanceUntilIdle()

        assertEquals(DataTransferDialog.NONE, fixture.viewModel.uiState.value.dialog)
        assertTrue(recorded.events.none { it is DataTransferEvent.OpenDocument })
        // Exactly one export happened.
        assertEquals(1, fixture.history.recorded.size)
        recorded.stop()
    }

    @Test
    fun `an import cannot start while another is running`() = runTest {
        val fixture = fixture()
        val source = InMemoryBackupSource(fixture.register(payload()))

        fixture.viewModel.onImportFileChosen(source)
        // Still reading the file when a second file arrives.
        fixture.viewModel.onImportFileChosen(InMemoryBackupSource("other".toByteArray()))
        advanceUntilIdle()

        // The first one won and opened the password dialog; the second was ignored rather than
        // replacing the file mid-flight.
        assertEquals(DataTransferDialog.IMPORT_PASSWORD, fixture.viewModel.uiState.value.dialog)
    }

    // --- History -----------------------------------------------------------------------

    @Test
    fun `an empty history is reported as empty rather than as loading forever`() = runTest {
        val fixture = fixture()

        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        assertFalse(state.isHistoryLoading)
        assertTrue(state.isHistoryEmpty)
    }

    private fun payload(
        subscriptions: List<com.griff.keeper.domain.model.Subscription> = emptyList(),
        obligations: List<com.griff.keeper.domain.model.Obligation> = emptyList(),
    ) = BackupPayload(
        schemaVersion = BackupFormat.SCHEMA_VERSION,
        exportedAt = Instant.parse("2026-08-18T19:43:00Z"),
        appVersion = "1.3.0",
        subscriptions = subscriptions,
        obligations = obligations,
        settings = PortableSettings.Default,
    )

    private fun fixture(
        localSubscriptions: List<com.griff.keeper.domain.model.Subscription> = emptyList(),
        localObligations: List<com.griff.keeper.domain.model.Obligation> = emptyList(),
    ) = Fixture(localSubscriptions, localObligations)

    private class Fixture(
        localSubscriptions: List<com.griff.keeper.domain.model.Subscription>,
        localObligations: List<com.griff.keeper.domain.model.Obligation>,
    ) {
        val clock = FixedClockProvider(Instant.parse("2026-08-21T00:43:00Z"))
        val subscriptions = FakeSubscriptionRepository(localSubscriptions)
        val obligations = FakeObligationRepository(localObligations)
        val settings = FakePortableSettingsRepository()
        val history = FakeBackupOperationRepository()
        val codec = FakeBackupCodec()
        val reader = FakeBackupFileReader()
        val writer = FakeBackupFileWriter()
        val sharing = FakeBackupFileSharing()
        val network = FakeNetworkAvailability()
        val importRepository = FakeBackupImportRepository(subscriptions, obligations)

        private val recorder = BackupOperationRecorder(
            repository = history,
            idGenerator = SequentialBackupOperationIdGenerator(),
            clock = clock,
        )

        private val collectPayload = CollectBackupPayloadUseCase(
            subscriptions = subscriptions,
            obligations = obligations,
            settings = settings,
            getAppVersion = GetAppVersionUseCase(
                object : AppVersionProvider {
                    override fun version() = AppVersion(name = "1.3.0", code = 7L)
                },
            ),
            clock = clock,
        )

        val viewModel = DataTransferViewModel(
            observeHistory = ObserveBackupHistoryUseCase(history),
            exportBackup = ExportBackupUseCase(
                collectPayload = collectPayload,
                codec = codec,
                writer = writer,
                recorder = recorder,
                clock = clock,
            ),
            shareBackup = ShareBackupUseCase(
                collectPayload = collectPayload,
                codec = codec,
                sharing = sharing,
                recorder = recorder,
                clock = clock,
            ),
            clearSharedBackups = ClearSharedBackupsUseCase(sharing),
            validateBackupFile = ValidateBackupFileUseCase(reader = reader, codec = codec),
            previewBackup = PreviewBackupUseCase(
                reader = reader,
                codec = codec,
                subscriptions = subscriptions,
                obligations = obligations,
                recorder = recorder,
                clock = clock,
            ),
            importBackup = ImportBackupUseCase(
                subscriptions = subscriptions,
                obligations = obligations,
                importRepository = importRepository,
                portableSettings = settings,
                recorder = recorder,
                ensureRemindersScheduled = EnsureRemindersScheduledUseCase(
                    RecordingReminderScheduler(),
                ),
                clock = clock,
            ),
            isNetworkAvailable = IsNetworkAvailableUseCase(network),
            clock = clock,
        )

        fun register(payload: BackupPayload): ByteArray = codec.register(payload, PASSWORD)

        /**
         * Starts recording the one-off events.
         *
         * Undispatched, so the collector is subscribed by the time this returns: the events carry no
         * replay - which is what stops a rotation from opening the picker twice - so anything emitted
         * before a subscriber exists is simply gone.
         */
        fun recordEvents(scope: TestScope): RecordedEvents {
            val collected = mutableListOf<DataTransferEvent>()
            val job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                viewModel.events.collect(collected::add)
            }
            return RecordedEvents(job, collected)
        }
    }

    /** The events seen so far, plus the way to stop watching once a test is done. */
    private class RecordedEvents(
        private val job: Job,
        val events: List<DataTransferEvent>,
    ) {
        /** The collector never finishes on its own, and `runTest` waits for the ones that do not. */
        fun stop() = job.cancel()
    }

    private companion object {
        const val PASSWORD = "MyBackupPassword"
        const val FILE_NAME = "griff-backup-2026-08-21-0043.griffbackup"
    }
}
