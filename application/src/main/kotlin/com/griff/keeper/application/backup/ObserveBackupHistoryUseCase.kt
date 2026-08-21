package com.griff.keeper.application.backup

import com.griff.keeper.domain.backup.BackupOperation
import com.griff.keeper.domain.backup.BackupOperationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Streams the device's import/export log, newest first.
 *
 * Capped rather than unbounded: this is a "what did I do recently" list, not an audit trail, and an
 * ever growing one would eventually be a scrolling exercise instead of an answer.
 */
class ObserveBackupHistoryUseCase @Inject constructor(
    private val repository: BackupOperationRepository,
) {
    operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<BackupOperation>> =
        repository.observeRecent(limit)

    companion object {
        const val DEFAULT_LIMIT: Int = 20
    }
}
