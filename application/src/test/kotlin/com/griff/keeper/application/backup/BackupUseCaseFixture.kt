package com.griff.keeper.application.backup

import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.application.appinfo.AppVersionProvider
import com.griff.keeper.application.appinfo.GetAppVersionUseCase
import com.griff.keeper.application.reminder.EnsureRemindersScheduledUseCase
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.testing.FakeAppCurrencyRepository
import com.griff.keeper.domain.testing.FakeBackupCodec
import com.griff.keeper.domain.testing.FakeBackupFileReader
import com.griff.keeper.domain.testing.FakeBackupFileSharing
import com.griff.keeper.domain.testing.FakeBackupFileWriter
import com.griff.keeper.domain.testing.FakeBackupImportRepository
import com.griff.keeper.domain.testing.FakeBackupOperationRepository
import com.griff.keeper.domain.testing.FakeObligationRepository
import com.griff.keeper.domain.testing.FakePortableSettingsRepository
import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.FixedClockProvider
import com.griff.keeper.domain.testing.RecordingReminderScheduler
import com.griff.keeper.domain.testing.SequentialBackupOperationIdGenerator
import java.time.Instant

/**
 * One wiring of the backup use cases and their doubles, so each test says what it is about.
 *
 * Assembled by hand rather than through a DI container: the tests are about the decisions the use
 * cases make, and a container would only add a way for the wiring itself to be wrong.
 */
internal class BackupUseCaseFixture(
    localSubscriptions: List<Subscription> = emptyList(),
    localObligations: List<Obligation> = emptyList(),
    localSettings: PortableSettings = PortableSettings.Default,
    localAppCurrency: Currency = Currency.Default,
) {
    val clock = FixedClockProvider(Instant.parse("2026-08-21T00:43:00Z"))
    val subscriptions = FakeSubscriptionRepository(localSubscriptions)
    val obligations = FakeObligationRepository(localObligations)
    val settings = FakePortableSettingsRepository(localSettings)
    val appCurrency = FakeAppCurrencyRepository(localAppCurrency)
    val history = FakeBackupOperationRepository()
    val codec = FakeBackupCodec()
    val reader = FakeBackupFileReader()
    val writer = FakeBackupFileWriter()
    val sharing = FakeBackupFileSharing()
    val importRepository = FakeBackupImportRepository(subscriptions, obligations)
    val scheduler = RecordingReminderScheduler()

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
                override fun version() = AppVersion(name = APP_VERSION, code = 7L)
            },
        ),
        clock = clock,
    )

    val exportBackup = ExportBackupUseCase(
        collectPayload = collectPayload,
        codec = codec,
        writer = writer,
        recorder = recorder,
        clock = clock,
    )

    val shareBackup = ShareBackupUseCase(
        collectPayload = collectPayload,
        codec = codec,
        sharing = sharing,
        recorder = recorder,
        clock = clock,
    )

    val previewBackup = PreviewBackupUseCase(
        reader = reader,
        codec = codec,
        subscriptions = subscriptions,
        obligations = obligations,
        appCurrency = appCurrency,
        recorder = recorder,
        clock = clock,
    )

    val importBackup = ImportBackupUseCase(
        subscriptions = subscriptions,
        obligations = obligations,
        appCurrency = appCurrency,
        importRepository = importRepository,
        portableSettings = settings,
        recorder = recorder,
        ensureRemindersScheduled = EnsureRemindersScheduledUseCase(scheduler),
        clock = clock,
    )

    val validateBackupFile = ValidateBackupFileUseCase(reader = reader, codec = codec)

    val observeHistory = ObserveBackupHistoryUseCase(history)

    fun payload(
        subscriptions: List<Subscription> = emptyList(),
        obligations: List<Obligation> = emptyList(),
        settings: PortableSettings = PortableSettings.Default,
        exportedAt: Instant = Instant.parse("2026-08-18T19:43:00Z"),
    ) = BackupPayload(
        schemaVersion = BackupFormat.SCHEMA_VERSION,
        exportedAt = exportedAt,
        appVersion = APP_VERSION,
        subscriptions = subscriptions,
        obligations = obligations,
        settings = settings,
    )

    companion object {
        const val APP_VERSION = "1.3.0"
        const val PASSWORD = "MyBackupPassword"
    }
}
