package com.griff.keeper.domain.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Constants that define what a Griff backup *is*, independently of how it is stored or encrypted.
 *
 * [SCHEMA_VERSION] is deliberately not the Room database version: the two evolve for different
 * reasons. A new column can change the database without changing what a backup carries, and a
 * backup written by an older install has to stay readable long after the schema it came from has
 * been migrated away. The two numbers are therefore allowed to drift apart on purpose.
 */
object BackupFormat {

    /** Version of the logical payload, see [BackupSchemaSupport]. */
    const val SCHEMA_VERSION: Int = 1

    /** The oldest payload this build still knows how to read, directly or through a migration. */
    const val OLDEST_SUPPORTED_SCHEMA_VERSION: Int = 1

    const val FILE_EXTENSION: String = "griffbackup"

    /**
     * Opaque bytes as far as the system is concerned.
     *
     * A more specific type would only invite other apps to claim they can open the file, which they
     * cannot: the contents are encrypted and the format is private to Griff.
     */
    const val MIME_TYPE: String = "application/octet-stream"

    /**
     * Hard ceiling on the size of a file the importer will even look at.
     *
     * The user's records are a few hundred bytes each, so a real backup is measured in kilobytes.
     * The limit exists because the file comes from a system picker and could be anything at all;
     * refusing early is cheaper - and safer - than allocating whatever was handed over.
     */
    const val MAX_FILE_BYTES: Long = 25L * 1024 * 1024

    /**
     * Ceiling on the decompressed payload.
     *
     * Without it a small, well-formed archive could ask for gigabytes of heap on decompression. The
     * limit is generous next to a realistic payload and still bounded.
     */
    const val MAX_PLAINTEXT_BYTES: Long = 32L * 1024 * 1024

    private val FILE_NAME_TIMESTAMP: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

    /** e.g. `griff-backup-2026-08-21-0054.griffbackup`. */
    fun fileName(createdAt: LocalDateTime): String =
        "griff-backup-${FILE_NAME_TIMESTAMP.format(createdAt)}.$FILE_EXTENSION"
}
