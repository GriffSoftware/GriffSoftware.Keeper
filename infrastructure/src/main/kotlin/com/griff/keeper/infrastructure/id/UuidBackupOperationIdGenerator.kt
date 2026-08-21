package com.griff.keeper.infrastructure.id

import com.griff.keeper.domain.id.BackupOperationIdGenerator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Random UUIDs, so a log entry never collides with one written a moment earlier. */
@Singleton
class UuidBackupOperationIdGenerator @Inject constructor() : BackupOperationIdGenerator {
    override fun next(): String = UUID.randomUUID().toString()
}
