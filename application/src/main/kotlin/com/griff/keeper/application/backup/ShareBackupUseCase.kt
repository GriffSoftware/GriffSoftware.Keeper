package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupCodec
import com.griff.keeper.domain.backup.BackupFileSharing
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.BackupSummary
import com.griff.keeper.domain.backup.SharedBackupFile
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** An encrypted backup staged for another app to pick up, plus what it contains. */
data class BackupShareResult(
    val file: SharedBackupFile,
    val summary: BackupSummary,
)

/**
 * Creates an encrypted backup and stages it where a mail client can read it.
 *
 * Griff does not send anything: it produces a file and hands a temporary, read-only handle to
 * whichever app the user picks in the system chooser. There is no SMTP client, no provider API and
 * no account here on purpose - the moment the app could send mail on its own, "your data never
 * leaves this device unless you move it" would stop being true.
 */
class ShareBackupUseCase @Inject constructor(
    private val collectPayload: CollectBackupPayloadUseCase,
    private val codec: BackupCodec,
    private val sharing: BackupFileSharing,
    private val recorder: BackupOperationRecorder,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        password: CharArray,
        fileName: String,
    ): Result<BackupShareResult> {
        val startedAt = clock.now()
        return runCatching {
            val payload = collectPayload()
            val image = codec.encode(payload, password)
            val restored = codec.decode(image, password)

            // Only one staged copy may exist at a time: a cache full of old backups is a pile of
            // encrypted files nobody asked to keep.
            sharing.clear()
            BackupShareResult(
                file = sharing.stage(fileName, image),
                summary = restored.summary(),
            )
        }
            .onSuccess { result ->
                recorder.recordSuccess(
                    type = BackupOperationType.EXPORT,
                    startedAt = startedAt,
                    fileName = result.file.fileName,
                    subscriptionCount = result.summary.subscriptionCount,
                    obligationCount = result.summary.obligationCount,
                    settingsCount = if (result.summary.hasSettings) 1 else 0,
                )
            }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                recorder.recordFailure(
                    type = BackupOperationType.EXPORT,
                    startedAt = startedAt,
                    fileName = fileName,
                    errorType = throwable.backupErrorType,
                )
            }
    }
}

/** Drops the staged copy once the chooser is done with it. */
class ClearSharedBackupsUseCase @Inject constructor(
    private val sharing: BackupFileSharing,
) {
    suspend operator fun invoke() = sharing.clear()
}
