package com.griff.keeper.infrastructure.backup.crypto

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import java.nio.ByteBuffer

/**
 * The unencrypted head of a `.griffbackup` file.
 *
 * Every field here is metadata the app needs *before* it has a key: which format this is, how the
 * key is derived, which cipher was used and where the ciphertext starts. Nothing about the user is
 * in it, and nothing about the user may ever be added to it - that is the line between a header and
 * a leak.
 *
 * The whole header is fed to AES-GCM as additional authenticated data, so it is covered by the same
 * tag as the ciphertext. Editing the iteration count, swapping the salt or claiming a different
 * format version therefore fails authentication instead of quietly changing how the file is read.
 *
 * ### Layout (big endian)
 *
 * ```
 * offset  size  field
 *      0    11  MAGIC, "GRIFFBACKUP" in ASCII
 *     11     1  formatVersion
 *     12     1  kdfId
 *     13     4  kdfIterations
 *     17     1  saltLength (S)
 *     18     S  salt
 *   18+S     1  cipherId
 *   19+S     1  nonceLength (N)
 *   20+S     N  nonce
 * 20+S+N     1  compressionId
 * 21+S+N     4  ciphertextLength
 * ```
 *
 * The file is that header followed by exactly `ciphertextLength` bytes of ciphertext with the GCM
 * tag appended. The length is written down rather than inferred from the file size so that a file
 * with trailing junk - or one that was truncated - is rejected as malformed instead of being fed to
 * the cipher.
 */
internal data class BackupEnvelope(
    val formatVersion: Int,
    val kdfId: Int,
    val kdfIterations: Int,
    val salt: ByteArray,
    val cipherId: Int,
    val nonce: ByteArray,
    val compressionId: Int,
    val ciphertext: ByteArray,
) {
    /** The header bytes, which double as the AES-GCM additional authenticated data. */
    fun header(): ByteArray {
        val buffer = ByteBuffer.allocate(headerLength(salt.size, nonce.size))
        buffer.put(MAGIC)
        buffer.put(formatVersion.toByte())
        buffer.put(kdfId.toByte())
        buffer.putInt(kdfIterations)
        buffer.put(salt.size.toByte())
        buffer.put(salt)
        buffer.put(cipherId.toByte())
        buffer.put(nonce.size.toByte())
        buffer.put(nonce)
        buffer.put(compressionId.toByte())
        buffer.putInt(ciphertext.size)
        return buffer.array()
    }

    fun toByteArray(): ByteArray = header() + ciphertext

    // A data class over ByteArray needs these: the generated ones compare references, which would
    // make two envelopes with identical bytes unequal and quietly break every test that says so.
    override fun equals(other: Any?): Boolean = this === other ||
        (
            other is BackupEnvelope &&
                formatVersion == other.formatVersion &&
                kdfId == other.kdfId &&
                kdfIterations == other.kdfIterations &&
                salt.contentEquals(other.salt) &&
                cipherId == other.cipherId &&
                nonce.contentEquals(other.nonce) &&
                compressionId == other.compressionId &&
                ciphertext.contentEquals(other.ciphertext)
            )

    override fun hashCode(): Int {
        var result = formatVersion
        result = 31 * result + kdfId
        result = 31 * result + kdfIterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + cipherId
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + compressionId
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }

    companion object {
        /**
         * Marker that makes the format recognizable without trusting the file name.
         *
         * The user is free to rename a backup, and any other app is free to hand over a `.jpg` that
         * claims to be one, so identity comes from the first eleven bytes and nowhere else.
         */
        val MAGIC: ByteArray = "GRIFFBACKUP".toByteArray(Charsets.US_ASCII)

        /** Version of the *envelope*, independent of the payload's schema version. */
        const val FORMAT_VERSION: Int = 1

        const val COMPRESSION_NONE: Int = 0
        const val COMPRESSION_GZIP: Int = 1

        /** Smallest file that could possibly be well formed, used to reject stubs cheaply. */
        val MIN_FILE_LENGTH: Int = headerLength(salt = 1, nonce = 1)

        private const val MAX_SALT_LENGTH = 64
        private const val MAX_NONCE_LENGTH = 32

        private fun headerLength(salt: Int, nonce: Int): Int =
            MAGIC.size + 1 + 1 + 4 + 1 + salt + 1 + 1 + nonce + 1 + 4

        /** True when [bytes] starts with [MAGIC]. Says nothing about the rest of the file. */
        fun hasMagic(bytes: ByteArray): Boolean {
            if (bytes.size < MAGIC.size) return false
            return MAGIC.indices.all { bytes[it] == MAGIC[it] }
        }

        /**
         * Parses an untrusted file image.
         *
         * Every read is bounds checked before it happens, and every declared length is checked
         * against a ceiling, so a crafted header cannot make the parser allocate or read past the
         * buffer. Anything that does not add up is [BackupErrorType.INVALID_FILE] - one answer for
         * "this is not our file", with no detail that would help someone probe the format.
         */
        fun parse(bytes: ByteArray): BackupEnvelope {
            if (bytes.size < MIN_FILE_LENGTH || !hasMagic(bytes)) invalid()

            val buffer = ByteBuffer.wrap(bytes)
            buffer.position(MAGIC.size)

            val formatVersion = buffer.readUnsignedByte()
            val kdfId = buffer.readUnsignedByte()
            val kdfIterations = buffer.readInt(remainingAtLeast = 4)
            if (kdfIterations <= 0) invalid()

            val salt = buffer.readLengthPrefixed(MAX_SALT_LENGTH)
            val cipherId = buffer.readUnsignedByte()
            val nonce = buffer.readLengthPrefixed(MAX_NONCE_LENGTH)
            val compressionId = buffer.readUnsignedByte()

            val ciphertextLength = buffer.readInt(remainingAtLeast = 4)
            if (ciphertextLength <= 0 || ciphertextLength != buffer.remaining()) invalid()

            val ciphertext = ByteArray(ciphertextLength)
            buffer.get(ciphertext)

            return BackupEnvelope(
                formatVersion = formatVersion,
                kdfId = kdfId,
                kdfIterations = kdfIterations,
                salt = salt,
                cipherId = cipherId,
                nonce = nonce,
                compressionId = compressionId,
                ciphertext = ciphertext,
            )
        }

        private fun ByteBuffer.readUnsignedByte(): Int {
            if (remaining() < 1) invalid()
            return get().toInt() and 0xFF
        }

        private fun ByteBuffer.readInt(remainingAtLeast: Int): Int {
            if (remaining() < remainingAtLeast) invalid()
            return int
        }

        private fun ByteBuffer.readLengthPrefixed(maxLength: Int): ByteArray {
            val length = readUnsignedByte()
            if (length == 0 || length > maxLength || remaining() < length) invalid()
            return ByteArray(length).also { get(it) }
        }

        private fun invalid(): Nothing =
            throw BackupFailureException(BackupErrorType.INVALID_FILE)
    }
}
