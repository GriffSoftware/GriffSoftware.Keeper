package com.griff.keeper.infrastructure.backup.crypto

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Authenticated encryption of a backup payload with a password the user chooses.
 *
 * ### Why a password and not the Android Keystore
 *
 * A Keystore key is bound to the device that created it, which is exactly the property a backup must
 * not have: the file exists so it can be restored on a *different* phone, which has no such key.
 * There is also no shared secret baked into the app - a constant in the APK, in `BuildConfig`, in an
 * asset or hidden in a native library is recoverable by anyone who unzips the download, so it would
 * be decoration rather than protection. The only key material is derived from what the user typed,
 * and the app has no way to recover a forgotten password. That is a deliberate consequence of being
 * offline, not an oversight.
 *
 * ### Primitives
 *
 * - **PBKDF2WithHmacSHA256**, [ITERATIONS] rounds, a fresh 128-bit salt per backup, 256-bit output.
 *   A password is not a key: without a deliberately slow derivation, a stolen file could be attacked
 *   at the speed of the attacker's hardware. PBKDF2 is chosen over the stronger memory-hard family
 *   (scrypt, Argon2) because it is the one such function every supported Android version ships in
 *   the platform: no third-party crypto dependency, no bundled native code, and an implementation
 *   the platform keeps patched. The iteration count is written into the file, so raising it later
 *   does not orphan existing backups.
 * - **AES-256-GCM**, a fresh 96-bit nonce per backup, 128-bit tag. GCM gives confidentiality *and*
 *   authentication in one pass, which is what makes "the file was edited" a detectable condition
 *   rather than a decryption that returns plausible garbage. The header is passed as additional
 *   authenticated data, so the crypto parameters are as tamper-evident as the payload.
 *
 * Salt and nonce come from [SecureRandom], never from a counter and never reused: two exports of
 * identical data with an identical password produce different keys and different ciphertext.
 */
internal object BackupCipher {

    const val KDF_PBKDF2_HMAC_SHA256: Int = 1
    const val CIPHER_AES_256_GCM: Int = 1

    /**
     * Deliberately expensive.
     *
     * High enough that guessing a password against a stolen file is slow, low enough that unlocking
     * a backup on a phone is a moment rather than a wait. Recorded in the envelope, so this constant
     * can grow with hardware without breaking older files.
     */
    const val ITERATIONS: Int = 210_000

    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val NONCE_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_ALGORITHM = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    private val random = SecureRandom()

    /**
     * Encrypts [plaintext] and returns a complete envelope.
     *
     * [password] is read, never copied into a `String` and never retained; the derived key is zeroed
     * before returning.
     */
    fun seal(plaintext: ByteArray, password: CharArray, compressionId: Int): BackupEnvelope {
        val salt = randomBytes(SALT_LENGTH_BYTES)
        val nonce = randomBytes(NONCE_LENGTH_BYTES)

        val key = deriveKey(password, salt, ITERATIONS)
        try {
            // The header has to exist before the ciphertext, because it is authenticated with it -
            // and it declares the ciphertext's length. The length is known up front: GCM adds the
            // tag and nothing else.
            val ciphertextLength = plaintext.size + TAG_LENGTH_BITS / Byte.SIZE_BITS
            val header = BackupEnvelope(
                formatVersion = BackupEnvelope.FORMAT_VERSION,
                kdfId = KDF_PBKDF2_HMAC_SHA256,
                kdfIterations = ITERATIONS,
                salt = salt,
                cipherId = CIPHER_AES_256_GCM,
                nonce = nonce,
                compressionId = compressionId,
                ciphertext = ByteArray(ciphertextLength),
            ).header()

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, nonce))
                updateAAD(header)
            }
            val ciphertext = cipher.doFinal(plaintext)
            check(ciphertext.size == ciphertextLength) { "Unexpected ciphertext length" }

            return BackupEnvelope(
                formatVersion = BackupEnvelope.FORMAT_VERSION,
                kdfId = KDF_PBKDF2_HMAC_SHA256,
                kdfIterations = ITERATIONS,
                salt = salt,
                cipherId = CIPHER_AES_256_GCM,
                nonce = nonce,
                compressionId = compressionId,
                ciphertext = ciphertext,
            )
        } finally {
            key.zeroize()
        }
    }

    /**
     * Authenticates and decrypts [envelope].
     *
     * A wrong password and an altered file are the same outcome here, and are reported as the same
     * category: GCM cannot tell them apart, and inventing a distinction would be a guess presented
     * as a fact. Nothing is returned unless the tag verifies, so a partially decrypted payload can
     * never reach the importer.
     */
    fun open(envelope: BackupEnvelope, password: CharArray): ByteArray {
        if (envelope.kdfId != KDF_PBKDF2_HMAC_SHA256 ||
            envelope.cipherId != CIPHER_AES_256_GCM
        ) {
            throw BackupFailureException(BackupErrorType.UNSUPPORTED_VERSION)
        }

        val key = deriveKey(password, envelope.salt, envelope.kdfIterations)
        try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(TAG_LENGTH_BITS, envelope.nonce),
                )
                updateAAD(envelope.header())
            }
            return cipher.doFinal(envelope.ciphertext)
        } catch (error: GeneralSecurityException) {
            throw BackupFailureException(BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED, error)
        } finally {
            key.zeroize()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        try {
            val derived = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec)
            val encoded = derived.encoded
            try {
                return SecretKeySpec(encoded, KEY_ALGORITHM)
            } finally {
                encoded.fill(0)
            }
        } catch (error: GeneralSecurityException) {
            throw BackupFailureException(BackupErrorType.UNKNOWN, error)
        } finally {
            // Drops the copy of the password the spec made when it was constructed.
            spec.clearPassword()
        }
    }

    /**
     * Best effort wipe of the derived key.
     *
     * `SecretKeySpec` copies the bytes it is given and hands out defensive copies, so its internal
     * array cannot be cleared from outside; `destroy()` is the only sanctioned way and most JCA
     * providers do not implement it. What the app *can* control it does: the password's copy inside
     * `PBEKeySpec` is cleared, the raw derived bytes are zeroed as soon as the key object exists, and
     * the key object itself lives no longer than the single operation that needs it. The residual
     * exposure is one short-lived object rather than a key kept for the life of the process.
     */
    private fun SecretKeySpec.zeroize() {
        runCatching { destroy() }
    }

    private fun randomBytes(length: Int): ByteArray = ByteArray(length).also(random::nextBytes)
}
