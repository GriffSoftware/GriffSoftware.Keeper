package com.griff.keeper.application.backup

import com.griff.keeper.application.reminder.EnsureRemindersScheduledUseCase
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupImportRepository
import com.griff.keeper.domain.backup.BackupMerger
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.ImportPlan
import com.griff.keeper.domain.backup.LocalDataSnapshot
import com.griff.keeper.domain.backup.PortableSettingsRepository
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.repository.AppCurrencyRepository
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Applies an already validated payload to local storage.
 *
 * ### All or nothing, across two stores
 *
 * The records live in Room and the portable preferences live in DataStore, and a Room transaction
 * says nothing about DataStore. Pretending otherwise would leave the app one failed write away from
 * a state the user cannot explain: restored subscriptions with the previous reminder settings, or the
 * reverse.
 *
 * The order is therefore chosen so the pair can be undone:
 *
 * 1. the plan is computed in full, before anything is written;
 * 2. the current preferences are captured;
 * 3. the preferences are written - one atomic edit of one small file, the write least likely to
 *    fail and the only one that is cheap to reverse;
 * 4. the records are written inside a single Room transaction, which is the bulk of the work and
 *    where a constraint or a full disk would actually show up;
 * 5. if that transaction fails, the captured preferences are put back.
 *
 * What remains is a process death between steps 3 and 4, which no amount of ordering can rule out
 * without a write-ahead log of its own. It is a bounded, recoverable outcome - a preference is
 * inconsistent, no record is - and the user can simply import again.
 *
 * The local import/export history is never part of any of this: a REPLACE wipes the user's portable
 * data, not the device's record of what was done to it.
 */
class ImportBackupUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val appCurrency: AppCurrencyRepository,
    private val importRepository: BackupImportRepository,
    private val portableSettings: PortableSettingsRepository,
    private val recorder: BackupOperationRecorder,
    private val ensureRemindersScheduled: EnsureRemindersScheduledUseCase,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        payload: BackupPayload,
        mode: ImportMode,
        fileName: String?,
    ): Result<ImportPlan> {
        val startedAt = clock.now()
        return runCatching {
            val local = LocalDataSnapshot(
                subscriptions = subscriptions.observeAll().first(),
                obligations = obligations.observeAll().first(),
                appCurrency = appCurrency.current(),
            )

            // Merging amounts held in two different currencies would either mix them silently or
            // require a conversion the user never asked for here, so the import is refused before
            // anything is written. REPLACE has no such restriction: it adopts the backup's currency
            // outright instead of reconciling it with what is already on the device.
            if (mode == ImportMode.MERGE && !local.isEmpty && local.appCurrency != payload.settings.appCurrency) {
                throw BackupFailureException(BackupErrorType.CURRENCY_MISMATCH)
            }

            val plan = BackupMerger.plan(mode, local, payload)

            val previousSettings = portableSettings.current()
            portableSettings.apply(plan.settings)
            try {
                importRepository.apply(plan)
            } catch (throwable: Throwable) {
                if (throwable !is CancellationException) {
                    runCatching { portableSettings.apply(previousSettings) }
                }
                throw throwable
            }

            plan
        }
            .onSuccess { plan ->
                recorder.recordSuccess(
                    type = BackupOperationType.IMPORT,
                    startedAt = startedAt,
                    fileName = fileName,
                    importMode = mode,
                    subscriptionCount = plan.importedSubscriptionCount,
                    obligationCount = plan.importedObligationCount,
                    settingsCount = 1,
                )

                // Imported records bring dates the reminder engine has never seen. The check is
                // re-registered rather than fired: it only ever looks at what falls due today, so a
                // restore cannot replay months of notifications from the other device.
                ensureRemindersScheduled()
            }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                recorder.recordFailure(
                    type = BackupOperationType.IMPORT,
                    startedAt = startedAt,
                    fileName = fileName,
                    importMode = mode,
                    errorType = throwable.backupErrorType,
                )
            }
    }
}
