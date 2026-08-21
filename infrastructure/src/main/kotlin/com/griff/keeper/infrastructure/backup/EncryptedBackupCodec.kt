package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupCodec
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.infrastructure.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * [BackupCodec] backed by [BackupFileCodec].
 *
 * Exists purely to move the work off the main thread and to be injectable; every decision about the
 * format lives in the pure object it delegates to. Key derivation is intentionally slow and JSON
 * parsing is not free, so none of it may run on the UI thread.
 */
@Singleton
class EncryptedBackupCodec @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BackupCodec {

    override suspend fun encode(payload: BackupPayload, password: CharArray): ByteArray =
        withContext(dispatcher) { BackupFileCodec.encode(payload, password) }

    override suspend fun inspect(bytes: ByteArray): Int =
        withContext(dispatcher) { BackupFileCodec.inspect(bytes) }

    override suspend fun decode(bytes: ByteArray, password: CharArray): BackupPayload =
        withContext(dispatcher) { BackupFileCodec.decode(bytes, password) }
}
