package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupCodec
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFileReader
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.BackupSource
import com.griff.keeper.domain.backup.BackupSummary
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.LocalDataSnapshot
import com.griff.keeper.domain.backup.BackupMerger
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.repository.AppCurrencyRepository
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * A backup that has been opened and understood, but not yet applied.
 *
 * Carries the decrypted [payload] so that the import which follows the user's decision does not have
 * to ask for the password a second time. The password itself is *not* here and is never kept: it has
 * done its job by the time this exists.
 */
data class BackupPreview(
    val fileName: String?,
    val summary: BackupSummary,
    val payload: BackupPayload,
    /** Whether the device already holds portable records, which decides what the user is asked. */
    val hasLocalData: Boolean,
    val localRecordCount: Int,
    /** Records that share a name with a local one but not an id. Informational only. */
    val possibleDuplicates: Int,
    /**
     * Whether [ImportMode.MERGE] is refused because the device and the backup carry different app
     * currencies. Only meaningful when [hasLocalData] is true - an empty device has no currency of
     * its own to conflict with. [ImportMode.REPLACE] is never affected: it adopts the backup's
     * currency outright instead of reconciling it with anything local.
     */
    val currencyMismatch: Boolean,
)

/**
 * Opens a candidate backup: reads it, checks the format, decrypts it, validates every record.
 *
 * Nothing is written to the database here. That separation is the whole point - by the time the user
 * sees what a file contains, the app has already proved it can read all of it, so the decision they
 * are asked to make is a real one and not a promise it might fail to keep.
 *
 * A wrong password is deliberately *not* recorded in the history. The overwhelmingly common cause is
 * a typo the user corrects on the spot, and a log that fills up with those stops being a record of
 * what happened to the data. Every other failure - a file that is not a backup, one from a newer
 * app, one that does not survive validation - is recorded, because it says something about the file
 * rather than about the typing.
 */
class PreviewBackupUseCase @Inject constructor(
    private val reader: BackupFileReader,
    private val codec: BackupCodec,
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val appCurrency: AppCurrencyRepository,
    private val recorder: BackupOperationRecorder,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        source: BackupSource,
        password: CharArray,
    ): Result<BackupPreview> {
        val startedAt = clock.now()
        return runCatching {
            val bytes = reader.read(source)
            codec.inspect(bytes)
            val payload = codec.decode(bytes, password)
            val local = LocalDataSnapshot(
                subscriptions = subscriptions.observeAll().first(),
                obligations = obligations.observeAll().first(),
                appCurrency = appCurrency.current(),
            )

            BackupPreview(
                fileName = source.displayName,
                summary = payload.summary(),
                payload = payload,
                hasLocalData = !local.isEmpty,
                localRecordCount = local.recordCount,
                // Reported from the merge plan so the number the user sees is the number the merge
                // would actually produce, rather than a second, separately written estimate.
                possibleDuplicates = BackupMerger
                    .plan(ImportMode.MERGE, local, payload)
                    .possibleDuplicates,
                currencyMismatch = !local.isEmpty && local.appCurrency != payload.settings.appCurrency,
            )
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
            val errorType = throwable.backupErrorType
            if (errorType != BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED) {
                recorder.recordFailure(
                    type = BackupOperationType.IMPORT,
                    startedAt = startedAt,
                    fileName = source.displayName,
                    errorType = errorType,
                )
            }
        }
    }
}

/**
 * The level-one check: is this file worth asking for a password over?
 *
 * Runs before the password dialog so a photo, a ZIP or a half-downloaded file is refused with an
 * honest message instead of turning into "wrong password". Reads only the unencrypted envelope, so
 * it learns the format version and nothing about the user's data.
 */
class ValidateBackupFileUseCase @Inject constructor(
    private val reader: BackupFileReader,
    private val codec: BackupCodec,
) {
    suspend operator fun invoke(source: BackupSource): Result<Int> = runCatching {
        codec.inspect(reader.read(source))
    }
}
