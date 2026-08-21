package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.infrastructure.backup.crypto.BackupCipher
import com.griff.keeper.infrastructure.backup.crypto.BackupEnvelope
import com.griff.keeper.infrastructure.backup.serialization.JsonBackupSerializer

/**
 * The whole `.griffbackup` pipeline, in one small, dependency-free object.
 *
 * ```
 * payload -> JSON -> GZIP -> AES-256-GCM -> envelope
 * envelope -> format check -> decrypt+authenticate -> GUNZIP -> JSON -> validated payload
 * ```
 *
 * Pure and synchronous by design: no coroutines, no Android, no injection. That is what makes the
 * security-critical part of the feature something a unit test can exercise end to end - round trip,
 * wrong password, flipped byte, truncated file, random file - without a device in the loop. The
 * suspending, injectable adapter is [EncryptedBackupCodec], which adds nothing but a dispatcher.
 */
internal object BackupFileCodec {

    fun encode(payload: BackupPayload, password: CharArray): ByteArray {
        val plaintext = JsonBackupSerializer.serialize(payload)
        val compressed = BackupCompression.compress(plaintext)
        return BackupCipher
            .seal(compressed, password, BackupEnvelope.COMPRESSION_GZIP)
            .toByteArray()
    }

    /**
     * The level-one check: does this even claim to be one of ours?
     *
     * Returns the envelope's format version. Runs before the user is asked for a password, so a
     * photo or a half-downloaded file is refused for what it is instead of being reported as a
     * failed password - and so the expensive key derivation is never spent on it.
     */
    fun inspect(bytes: ByteArray): Int {
        if (bytes.size > BackupFormat.MAX_FILE_BYTES) {
            throw BackupFailureException(BackupErrorType.FILE_TOO_LARGE)
        }
        val envelope = BackupEnvelope.parse(bytes)
        if (envelope.formatVersion > BackupEnvelope.FORMAT_VERSION) {
            throw BackupFailureException(BackupErrorType.UNSUPPORTED_VERSION)
        }
        if (envelope.compressionId != BackupEnvelope.COMPRESSION_NONE &&
            envelope.compressionId != BackupEnvelope.COMPRESSION_GZIP
        ) {
            throw BackupFailureException(BackupErrorType.UNSUPPORTED_VERSION)
        }
        return envelope.formatVersion
    }

    fun decode(bytes: ByteArray, password: CharArray): BackupPayload {
        inspect(bytes)
        val envelope = BackupEnvelope.parse(bytes)
        val compressed = BackupCipher.open(envelope, password)
        val plaintext = when (envelope.compressionId) {
            BackupEnvelope.COMPRESSION_GZIP -> BackupCompression.decompress(compressed)
            else -> compressed
        }
        if (plaintext.size > BackupFormat.MAX_PLAINTEXT_BYTES) {
            throw BackupFailureException(BackupErrorType.FILE_TOO_LARGE)
        }
        return JsonBackupSerializer.deserialize(plaintext)
    }
}
