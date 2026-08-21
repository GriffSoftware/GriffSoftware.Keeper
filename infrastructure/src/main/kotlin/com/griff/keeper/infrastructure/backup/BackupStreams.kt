package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFileReader
import com.griff.keeper.domain.backup.BackupFileWriter
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupSink
import com.griff.keeper.domain.backup.BackupSource
import com.griff.keeper.infrastructure.di.IoDispatcher
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads a file the user picked, refusing anything that cannot plausibly be a backup.
 *
 * The size is checked twice, and both checks are needed. The platform's reported size is a hint that
 * lets an oversized file be refused without reading a byte of it; the running total while reading is
 * what actually enforces the limit, because that hint can be missing or wrong. Nothing is ever
 * allocated to the size the file claims.
 */
@Singleton
class StreamingBackupFileReader @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BackupFileReader {

    override suspend fun read(source: BackupSource): ByteArray = withContext(dispatcher) {
        val declaredSize = source.sizeBytes
        if (declaredSize != null && declaredSize > BackupFormat.MAX_FILE_BYTES) {
            throw BackupFailureException(BackupErrorType.FILE_TOO_LARGE)
        }

        val bytes = try {
            source.openInputStream().use { input ->
                val output = ByteArrayOutputStream(initialCapacity(declaredSize))
                val buffer = ByteArray(CHUNK_BYTES)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > BackupFormat.MAX_FILE_BYTES) {
                        throw BackupFailureException(BackupErrorType.FILE_TOO_LARGE)
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        } catch (error: IOException) {
            throw BackupFailureException(error.storageErrorType(), error)
        }

        if (bytes.isEmpty()) throw BackupFailureException(BackupErrorType.INVALID_FILE)
        bytes
    }

    private fun initialCapacity(declaredSize: Long?): Int =
        declaredSize?.coerceIn(0, MAX_PREALLOCATED_BYTES)?.toInt() ?: CHUNK_BYTES

    private companion object {
        const val CHUNK_BYTES = 16 * 1024

        /**
         * A cap on how much is reserved up front.
         *
         * The declared size comes from outside the app, so it decides at most how big the first
         * buffer is; growth beyond that follows what was actually read.
         */
        const val MAX_PREALLOCATED_BYTES = 1L * 1024 * 1024
    }
}

/**
 * Writes a finished backup to the document the user chose.
 *
 * One `write` of an image that has already been produced, verified and re-opened. There is no
 * incremental writing here on purpose: a stream that fails halfway through could leave a file that
 * has our magic bytes, a plausible size and no way to be restored - the one failure mode a backup
 * feature must not have.
 */
@Singleton
class StreamingBackupFileWriter @Inject constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BackupFileWriter {

    override suspend fun write(sink: BackupSink, bytes: ByteArray) = withContext(dispatcher) {
        try {
            sink.openOutputStream().use { output ->
                output.write(bytes)
                output.flush()
            }
        } catch (error: IOException) {
            throw BackupFailureException(error.storageErrorType(), error)
        }
    }
}

/**
 * Tells "the disk is full" apart from every other I/O failure.
 *
 * Android surfaces a full volume as an `IOException` whose cause names the `ENOSPC` errno, so the
 * chain is inspected rather than the top-level type. Worth the awkwardness: "there is no space for
 * the backup" is something the user can act on, while "could not write the file" is not.
 */
internal fun Throwable.storageErrorType(): BackupErrorType {
    var current: Throwable? = this
    // Bounded, because a cause chain is allowed to be cyclic and this runs on a failure path.
    repeat(MAX_CAUSE_DEPTH) {
        val throwable = current ?: return BackupErrorType.IO_ERROR
        val message = throwable.message.orEmpty()
        if (message.contains("ENOSPC", ignoreCase = true) ||
            message.contains("No space left", ignoreCase = true)
        ) {
            return BackupErrorType.INSUFFICIENT_STORAGE
        }
        current = throwable.cause?.takeIf { it !== throwable }
    }
    return BackupErrorType.IO_ERROR
}

private const val MAX_CAUSE_DEPTH = 8
