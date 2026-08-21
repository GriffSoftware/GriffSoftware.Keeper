package com.griff.keeper.domain.backup

import kotlinx.coroutines.flow.Flow

/**
 * Turns a [BackupPayload] into an encrypted file image and back.
 *
 * One port, three responsibilities that are implemented as separate collaborators behind it -
 * serialization, compression and authenticated encryption. Callers get an all-or-nothing contract:
 * either a complete, verified payload comes back, or a [BackupFailureException] does.
 */
interface BackupCodec {

    /**
     * Serializes, compresses and encrypts [payload] with a key derived from [password].
     *
     * The result is a complete file image, safe to write in a single pass. [password] is only read;
     * clearing it is the caller's job, and no implementation may retain it.
     */
    suspend fun encode(payload: BackupPayload, password: CharArray): ByteArray

    /**
     * Checks whether [bytes] even looks like a Griff backup, without needing the password.
     *
     * Returns the envelope's format version. This is the gate that keeps a photo, a JSON file or a
     * truncated download out of the decryption path.
     */
    suspend fun inspect(bytes: ByteArray): Int

    /** Decrypts, authenticates, deserializes and validates. Never returns a partial payload. */
    suspend fun decode(bytes: ByteArray, password: CharArray): BackupPayload
}

/** Reads a candidate backup into memory, refusing anything that cannot plausibly be one. */
interface BackupFileReader {

    suspend fun read(source: BackupSource): ByteArray
}

/** Writes a finished, already verified backup image to the destination the user chose. */
interface BackupFileWriter {

    suspend fun write(sink: BackupSink, bytes: ByteArray)
}

/**
 * Stages a finished backup where another application can read it, e.g. an e-mail client.
 *
 * Returns an opaque handle rather than a platform URI type, so nothing above the platform layer has
 * to know how the file is exposed. Griff never sends the file itself.
 */
interface BackupFileSharing {

    suspend fun stage(fileName: String, bytes: ByteArray): SharedBackupFile

    /** Drops everything previously staged. Called before staging and after sharing. */
    suspend fun clear()
}

/** An encrypted backup that has been made readable to other apps, for as long as they need it. */
data class SharedBackupFile(
    /** Platform handle, resolved by the presentation layer. Never a filesystem path. */
    val uri: String,
    val fileName: String,
)

/** The device's own import/export log. Local bookkeeping, never part of a backup. */
interface BackupOperationRepository {

    fun observeRecent(limit: Int): Flow<List<BackupOperation>>

    suspend fun record(operation: BackupOperation)
}

/**
 * Applies an [ImportPlan] to local storage as one all-or-nothing step.
 *
 * The contract is the point of the port: a half-applied import - some records replaced, some not -
 * is worse than a failed one, so the implementation is required to be transactional.
 */
interface BackupImportRepository {

    suspend fun apply(plan: ImportPlan)
}

/** Reads and writes the preferences that are portable between devices. */
interface PortableSettingsRepository {

    suspend fun current(): PortableSettings

    suspend fun apply(settings: PortableSettings)
}

/**
 * Whether the device currently has a working internet connection.
 *
 * Used for one thing only: warning the user that a mail they are about to hand to their e-mail app
 * may sit in an outbox. Griff itself never goes online.
 */
interface NetworkAvailability {

    fun isOnline(): Boolean
}
