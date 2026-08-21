package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFormat
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException

/**
 * GZIP with a ceiling on the decompressed size.
 *
 * Compression is worth it - the payload is repetitive JSON and shrinks by an order of magnitude -
 * but it also means a small file can ask for a large allocation. The expansion happens through a
 * fixed buffer with a running total, so a crafted archive stops at [BackupFormat.MAX_PLAINTEXT_BYTES]
 * instead of at whatever the heap allows.
 */
internal object BackupCompression {

    fun compress(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(bytes.size / EXPECTED_RATIO + GZIP_OVERHEAD)
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    /**
     * @throws BackupFailureException with [BackupErrorType.FILE_TOO_LARGE] when the archive expands
     * past the limit, and [BackupErrorType.INVALID_FILE] when it is not a GZIP stream at all.
     */
    fun decompress(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream(bytes.size * EXPECTED_RATIO)
        val buffer = ByteArray(CHUNK_BYTES)
        var total = 0L

        try {
            GZIPInputStream(bytes.inputStream(), CHUNK_BYTES).use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > BackupFormat.MAX_PLAINTEXT_BYTES) {
                        throw BackupFailureException(BackupErrorType.FILE_TOO_LARGE)
                    }
                    output.write(buffer, 0, read)
                }
            }
        } catch (error: ZipException) {
            throw BackupFailureException(BackupErrorType.INVALID_FILE, error)
        } catch (error: java.io.IOException) {
            throw BackupFailureException(BackupErrorType.INVALID_FILE, error)
        }

        return output.toByteArray()
    }

    /** Rough shrink factor of the payload's JSON, used only to size the initial buffer. */
    private const val EXPECTED_RATIO = 8
    private const val GZIP_OVERHEAD = 64
    private const val CHUNK_BYTES = 16 * 1024
}
