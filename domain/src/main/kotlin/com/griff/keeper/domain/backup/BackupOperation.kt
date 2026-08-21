package com.griff.keeper.domain.backup

import java.time.Instant

/** Whether an operation wrote a backup or read one back in. */
enum class BackupOperationType {
    EXPORT,
    IMPORT,
}

/** How an operation ended. A cancelled file picker is neither and is never recorded. */
enum class BackupOperationStatus {
    SUCCESS,
    FAILED,
}

/** What an import does to the data that is already on the device. */
enum class ImportMode {
    /** Keeps local records and adds - or refreshes - the ones the backup carries. */
    MERGE,

    /** Discards the local portable data and replaces it with the backup's. */
    REPLACE,
}

/**
 * Why an operation failed, in categories the UI can explain.
 *
 * Deliberately coarse: this is what gets persisted, and a persisted error must not be able to carry
 * a stack trace, a file path, a decrypted record or anything else about the user's data. The
 * presentation layer turns the category into a sentence.
 */
enum class BackupErrorType {
    /** Not a Griff backup at all: wrong magic, unreadable header, empty file. */
    INVALID_FILE,

    /** The file is too large to be a plausible backup, so it was refused before being parsed. */
    FILE_TOO_LARGE,

    /**
     * The password did not open the file, or the contents were altered after it was written.
     *
     * The two are one category on purpose - AES-GCM cannot tell them apart, and pretending
     * otherwise would be a guess dressed up as a diagnosis.
     */
    WRONG_PASSWORD_OR_CORRUPTED,

    /** Written by a newer Griff, or by a schema this build has no migration for. */
    UNSUPPORTED_VERSION,

    /** The file could not be read or written. */
    IO_ERROR,

    /** The file opened, but its contents are not a valid set of Griff records. */
    VALIDATION_ERROR,

    /** Not enough free space to create or to restore the backup. */
    INSUFFICIENT_STORAGE,

    UNKNOWN,
}

/**
 * One entry of the device's import/export log.
 *
 * This is *not* part of the user's data and is never exported: it describes what happened on this
 * installation, which is meaningless on another device. It also survives a REPLACE import, so that
 * the very import that wiped the records is still visible in the history right afterwards.
 */
data class BackupOperation(
    val id: String,
    val type: BackupOperationType,
    val startedAt: Instant,
    val finishedAt: Instant,
    val status: BackupOperationStatus,
    /** Name of the file as far as the app knows it; `null` when the user cancelled the naming. */
    val fileName: String?,
    /** Only meaningful for [BackupOperationType.IMPORT]. */
    val importMode: ImportMode?,
    val subscriptionCount: Int,
    val obligationCount: Int,
    /** How many portable settings blocks the operation moved; today either 0 or 1. */
    val settingsCount: Int,
    /** `null` exactly when [status] is [BackupOperationStatus.SUCCESS]. */
    val errorType: BackupErrorType?,
) {
    val recordCount: Int get() = subscriptionCount + obligationCount
}
