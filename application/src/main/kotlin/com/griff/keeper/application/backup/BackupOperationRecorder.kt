package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupOperation
import com.griff.keeper.domain.backup.BackupOperationRepository
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.id.BackupOperationIdGenerator
import com.griff.keeper.domain.time.ClockProvider
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Writes entries of the local import/export log.
 *
 * A collaborator rather than something every use case does inline, so that the log has exactly one
 * shape: an id, a window of time, a result and the counts - and never a message, a path or a cause.
 *
 * Failing to write the log is swallowed on purpose. The log exists to describe what happened to the
 * user's data; it must never be the reason an otherwise successful export or import is reported as
 * failed, nor turn a failure into a crash.
 */
class BackupOperationRecorder @Inject constructor(
    private val repository: BackupOperationRepository,
    private val idGenerator: BackupOperationIdGenerator,
    private val clock: ClockProvider,
) {

    suspend fun recordSuccess(
        type: BackupOperationType,
        startedAt: Instant,
        fileName: String?,
        importMode: ImportMode? = null,
        subscriptionCount: Int,
        obligationCount: Int,
        settingsCount: Int,
    ) = record(
        type = type,
        startedAt = startedAt,
        status = BackupOperationStatus.SUCCESS,
        fileName = fileName,
        importMode = importMode,
        subscriptionCount = subscriptionCount,
        obligationCount = obligationCount,
        settingsCount = settingsCount,
        errorType = null,
    )

    suspend fun recordFailure(
        type: BackupOperationType,
        startedAt: Instant,
        fileName: String?,
        importMode: ImportMode? = null,
        errorType: BackupErrorType,
    ) = record(
        type = type,
        startedAt = startedAt,
        status = BackupOperationStatus.FAILED,
        fileName = fileName,
        importMode = importMode,
        subscriptionCount = 0,
        obligationCount = 0,
        settingsCount = 0,
        errorType = errorType,
    )

    private suspend fun record(
        type: BackupOperationType,
        startedAt: Instant,
        status: BackupOperationStatus,
        fileName: String?,
        importMode: ImportMode?,
        subscriptionCount: Int,
        obligationCount: Int,
        settingsCount: Int,
        errorType: BackupErrorType?,
    ) {
        runCatching {
            repository.record(
                BackupOperation(
                    id = idGenerator.next(),
                    type = type,
                    startedAt = startedAt,
                    finishedAt = clock.now(),
                    status = status,
                    fileName = fileName,
                    importMode = importMode,
                    subscriptionCount = subscriptionCount,
                    obligationCount = obligationCount,
                    settingsCount = settingsCount,
                    errorType = errorType,
                ),
            )
        }
            // A cancelled scope is not a logging failure and has to keep propagating.
            .onFailure { throwable -> if (throwable is CancellationException) throw throwable }
    }
}
