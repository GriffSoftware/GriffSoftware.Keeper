package com.griff.keeper.infrastructure.backup

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.infrastructure.backup.crypto.BackupEnvelope
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The security-critical part of the feature, exercised end to end without a device.
 *
 * Every case here is one the app has to survive in the field: a password that does not match, a file
 * that was edited, a download that stopped halfway, a photo picked by mistake, and a backup written
 * by a version that does not exist yet.
 */
class BackupFileCodecTest {

    private val password = BackupTestPayloads.PASSWORD.toCharArray()

    @Test
    fun `round trip returns the original payload`() {
        val payload = BackupTestPayloads.payload(subscriptions = 3, obligations = 2)

        val restored = BackupFileCodec.decode(
            BackupFileCodec.encode(payload, password),
            password,
        )

        assertEquals(payload, restored)
    }

    @Test
    fun `exported file does not contain the plaintext of a record`() {
        val payload = BackupTestPayloads.payload()

        val image = BackupFileCodec.encode(payload, password)
        val asText = image.toString(Charsets.ISO_8859_1)

        // The user must not be able to read their data by opening the file in a text editor.
        assertFalse(asText.contains("Service 0"))
        assertFalse(asText.contains("Policy 0"))
        assertFalse(asText.contains("schemaVersion"))
    }

    @Test
    fun `two exports of the same data produce different ciphertext`() {
        val payload = BackupTestPayloads.payload()

        val first = BackupFileCodec.encode(payload, password)
        val second = BackupFileCodec.encode(payload, password)

        // A fresh salt and a fresh nonce per backup: identical input must not reveal that it is
        // identical, and a nonce must never be reused with a key.
        assertFalse(first.contentEquals(second))

        val firstEnvelope = BackupEnvelope.parse(first)
        val secondEnvelope = BackupEnvelope.parse(second)
        assertFalse(firstEnvelope.salt.contentEquals(secondEnvelope.salt))
        assertFalse(firstEnvelope.nonce.contentEquals(secondEnvelope.nonce))

        // Both still decrypt to the same thing.
        assertEquals(
            BackupFileCodec.decode(first, password),
            BackupFileCodec.decode(second, password),
        )
    }

    @Test
    fun `wrong password is refused`() {
        val image = BackupFileCodec.encode(BackupTestPayloads.payload(), password)

        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(image, "OtherPassword".toCharArray())
        }

        assertEquals(BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED, failure.errorType)
    }

    @Test
    fun `a single flipped byte of ciphertext is refused`() {
        val image = BackupFileCodec.encode(BackupTestPayloads.payload(), password)
        val tampered = image.copyOf()
        // Well inside the ciphertext, past the header.
        val index = tampered.lastIndex - 8
        tampered[index] = (tampered[index].toInt() xor 0x01).toByte()

        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(tampered, password)
        }

        assertEquals(BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED, failure.errorType)
    }

    @Test
    fun `editing the crypto header is refused`() {
        val image = BackupFileCodec.encode(BackupTestPayloads.payload(), password)
        val tampered = image.copyOf()
        // The salt lives just after magic, format version, KDF id and the iteration count.
        val saltStart = BackupEnvelope.MAGIC.size + 1 + 1 + 4 + 1
        tampered[saltStart] = (tampered[saltStart].toInt() xor 0x7F).toByte()

        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(tampered, password)
        }

        // The header is additional authenticated data, so changing it breaks the tag rather than
        // silently changing how the file is read.
        assertEquals(BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED, failure.errorType)
    }

    @Test
    fun `a file without the magic marker is not a backup`() {
        val photo = ByteArray(4096) { (it % 251).toByte() }
        // JPEG start of image, so the bytes look like a real file rather than noise.
        photo[0] = 0xFF.toByte()
        photo[1] = 0xD8.toByte()

        val failure = assertFailsWith<BackupFailureException> { BackupFileCodec.inspect(photo) }

        assertEquals(BackupErrorType.INVALID_FILE, failure.errorType)
    }

    @Test
    fun `a JSON file is not a backup`() {
        val json = """{"subscriptions":[]}""".toByteArray(Charsets.UTF_8)

        val failure = assertFailsWith<BackupFailureException> { BackupFileCodec.inspect(json) }

        assertEquals(BackupErrorType.INVALID_FILE, failure.errorType)
    }

    @Test
    fun `a truncated backup is refused without crashing`() {
        val image = BackupFileCodec.encode(BackupTestPayloads.payload(), password)

        val truncated = image.copyOf(image.size - 16)

        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.decode(truncated, password)
        }
        // The declared ciphertext length no longer matches what is there, which is a malformed file
        // rather than a decryption problem.
        assertEquals(BackupErrorType.INVALID_FILE, failure.errorType)
    }

    @Test
    fun `an empty file is refused`() {
        val failure = assertFailsWith<BackupFailureException> {
            BackupFileCodec.inspect(ByteArray(0))
        }

        assertEquals(BackupErrorType.INVALID_FILE, failure.errorType)
    }

    @Test
    fun `an oversized file is refused before it is parsed`() {
        val huge = ByteArray((BackupFormat.MAX_FILE_BYTES + 1).toInt())

        val failure = assertFailsWith<BackupFailureException> { BackupFileCodec.inspect(huge) }

        assertEquals(BackupErrorType.FILE_TOO_LARGE, failure.errorType)
    }

    @Test
    fun `inspect accepts a real backup and reports its format version`() {
        val image = BackupFileCodec.encode(BackupTestPayloads.payload(), password)

        assertEquals(BackupEnvelope.FORMAT_VERSION, BackupFileCodec.inspect(image))
        assertTrue(BackupEnvelope.hasMagic(image))
    }

    @Test
    fun `an envelope from a newer format version is refused`() {
        val image = BackupFileCodec.encode(BackupTestPayloads.payload(), password)
        val bumped = image.copyOf()
        bumped[BackupEnvelope.MAGIC.size] = (BackupEnvelope.FORMAT_VERSION + 1).toByte()

        val failure = assertFailsWith<BackupFailureException> { BackupFileCodec.inspect(bumped) }

        assertEquals(BackupErrorType.UNSUPPORTED_VERSION, failure.errorType)
    }

    @Test
    fun `an uncompressed payload is still readable`() {
        val json = BackupTestPayloads.json()
        val image = BackupTestPayloads.encryptedFile(
            plaintext = json.toByteArray(Charsets.UTF_8),
            compress = false,
        )

        val payload = BackupFileCodec.decode(image, password)

        assertEquals(1, payload.subscriptions.size)
    }

    @Test
    fun `compression round trips`() {
        val original = BackupTestPayloads.json().toByteArray(Charsets.UTF_8)

        val restored = BackupCompression.decompress(BackupCompression.compress(original))

        assertContentEquals(original, restored)
    }

    @Test
    fun `a payload that expands past the limit is refused`() {
        // A highly compressible plaintext larger than the decompressed ceiling: small on disk,
        // enormous in memory. Exactly the shape of a decompression bomb.
        val oversized = ByteArray((BackupFormat.MAX_PLAINTEXT_BYTES + 1024).toInt())
        val archive = BackupCompression.compress(oversized)
        assertTrue(archive.size < BackupFormat.MAX_FILE_BYTES)

        val failure = assertFailsWith<BackupFailureException> {
            BackupCompression.decompress(archive)
        }

        assertEquals(BackupErrorType.FILE_TOO_LARGE, failure.errorType)
    }

    @Test
    fun `data that is not a GZIP stream is refused`() {
        val failure = assertFailsWith<BackupFailureException> {
            BackupCompression.decompress(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        }

        assertEquals(BackupErrorType.INVALID_FILE, failure.errorType)
    }
}
