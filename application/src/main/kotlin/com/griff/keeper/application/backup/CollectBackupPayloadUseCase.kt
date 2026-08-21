package com.griff.keeper.application.backup

import com.griff.keeper.application.appinfo.GetAppVersionUseCase
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.PortableSettingsRepository
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Gathers the portable data of this device into a payload.
 *
 * The one place that decides what a backup contains. Everything it reads is data the user entered or
 * a preference they set; nothing device bound is even reachable from here, which is why "did we
 * accidentally export the notification permission" has a structural answer rather than a review
 * checklist.
 */
class CollectBackupPayloadUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val settings: PortableSettingsRepository,
    private val getAppVersion: GetAppVersionUseCase,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(): BackupPayload = BackupPayload(
        schemaVersion = BackupFormat.SCHEMA_VERSION,
        exportedAt = clock.now(),
        appVersion = getAppVersion().name,
        subscriptions = subscriptions.observeAll().first(),
        obligations = obligations.observeAll().first(),
        settings = settings.current(),
    )
}
