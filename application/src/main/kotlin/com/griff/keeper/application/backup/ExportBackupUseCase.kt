package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupCodec
import com.griff.keeper.domain.backup.BackupFileWriter
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.BackupSink
import com.griff.keeper.domain.backup.BackupSummary
import com.griff.keeper.domain.backup.backupErrorType
import com.griff.keeper.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** What an export produced, for the success message and for the history entry. */
data class BackupExportResult(
    val fileName: String,
    val summary: BackupSummary,
)

/**
 * Creates an encrypted backup and writes it to the destination the user picked.
 *
 * The write is the last step and it writes a *finished* file. The complete encrypted image is built
 * in memory and then opened again - decrypted, authenticated and validated with the same code path
 * an import uses - before a single byte reaches the user's document. A backup that cannot be
 * restored is worse than no backup at all, so the app refuses to hand one over: if anything fails,
 * the destination is never touched and the operation is reported as not completed.
 *
 * Building the image in memory rather than in a temporary file is deliberate. The payload is a few
 * hundred bytes per record, and a plaintext staging file - however short-lived - would be exactly
 * the artefact this feature exists to avoid.
 *
 * [password] belongs to the caller: this use case only reads it and never stores it, and clearing it
 * is the caller's responsibility.
 */
class ExportBackupUseCase @Inject constructor(
    private val collectPayload: CollectBackupPayloadUseCase,
    private val codec: BackupCodec,
    private val writer: BackupFileWriter,
    private val recorder: BackupOperationRecorder,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        password: CharArray,
        fileName: String,
        sink: BackupSink,
    ): Result<BackupExportResult> {
        val startedAt = clock.now()
        return runCatching {
            val payload = collectPayload()
            val image = codec.encode(payload, password)

            // Verifies what was produced, not what was intended: a full round trip through the
            // import path is the only check that proves the file can actually be restored.
            val restored = codec.decode(image, password)

            writer.write(sink, image)
            BackupExportResult(fileName = fileName, summary = restored.summary())
        }
            .onSuccess { result ->
                recorder.recordSuccess(
                    type = BackupOperationType.EXPORT,
                    startedAt = startedAt,
                    fileName = result.fileName,
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
