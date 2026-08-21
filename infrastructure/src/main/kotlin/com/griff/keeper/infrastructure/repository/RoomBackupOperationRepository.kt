package com.griff.keeper.infrastructure.repository

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupOperation
import com.griff.keeper.domain.backup.BackupOperationRepository
import com.griff.keeper.domain.backup.BackupOperationStatus
import com.griff.keeper.domain.backup.BackupOperationType
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.infrastructure.database.dao.BackupOperationDao
import com.griff.keeper.infrastructure.database.entity.BackupOperationEntity
import com.griff.keeper.infrastructure.di.IoDispatcher
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Room backed implementation of [BackupOperationRepository]. */
@Singleton
class RoomBackupOperationRepository @Inject constructor(
    private val dao: BackupOperationDao,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BackupOperationRepository {

    override fun observeRecent(limit: Int): Flow<List<BackupOperation>> =
        dao.observeRecent(limit)
            .map { entities -> entities.mapNotNull(::toDomain) }
            .flowOn(dispatcher)

    override suspend fun record(operation: BackupOperation) = withContext(dispatcher) {
        dao.insert(operation.toEntity())
        dao.trimTo(MAX_ENTRIES)
    }

    private fun BackupOperation.toEntity() = BackupOperationEntity(
        id = id,
        type = type.name,
        startedAtEpochMillis = startedAt.toEpochMilli(),
        finishedAtEpochMillis = finishedAt.toEpochMilli(),
        status = status.name,
        fileName = fileName,
        importMode = importMode?.name,
        subscriptionCount = subscriptionCount,
        obligationCount = obligationCount,
        settingsCount = settingsCount,
        errorType = errorType?.name,
    )

    /**
     * `null` for a row this build cannot read.
     *
     * A log entry written by a newer version - a type or a status this one has no name for - is
     * skipped rather than crashing the list or being shown as something it is not. Losing one line
     * of history is a fair price for a screen that always opens.
     */
    private fun toDomain(entity: BackupOperationEntity): BackupOperation? {
        val type = enumOrNull<BackupOperationType>(entity.type) ?: return null
        val status = enumOrNull<BackupOperationStatus>(entity.status) ?: return null
        return BackupOperation(
            id = entity.id,
            type = type,
            startedAt = Instant.ofEpochMilli(entity.startedAtEpochMillis),
            finishedAt = Instant.ofEpochMilli(entity.finishedAtEpochMillis),
            status = status,
            fileName = entity.fileName,
            importMode = entity.importMode?.let { enumOrNull<ImportMode>(it) },
            subscriptionCount = entity.subscriptionCount,
            obligationCount = entity.obligationCount,
            settingsCount = entity.settingsCount,
            // An unknown category still means "it failed", so it degrades to UNKNOWN instead of
            // dropping the entry: the outcome is the part the user cares about.
            errorType = entity.errorType?.let {
                enumOrNull<BackupErrorType>(it) ?: BackupErrorType.UNKNOWN
            },
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }

    private companion object {
        /**
         * How many entries the log keeps.
         *
         * Comfortably more than the screen shows, so scrolling back a little always works, and still
         * a fixed ceiling rather than a table that grows for the life of the install.
         */
        const val MAX_ENTRIES = 100
    }
}
