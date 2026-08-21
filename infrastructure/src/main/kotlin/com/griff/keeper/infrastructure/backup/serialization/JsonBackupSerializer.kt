package com.griff.keeper.infrastructure.backup.serialization

import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFormat
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.BackupRecordValidator
import com.griff.keeper.domain.backup.BackupSchemaSupport
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Turns a payload into the plaintext bytes that get encrypted, and back.
 *
 * JSON is an implementation detail the user never sees: it is compressed and encrypted before it
 * reaches a file. It is chosen for the boring reasons - a schema that can grow a field without
 * breaking old readers, and a parser that only ever produces the data classes it was handed.
 *
 * Reading is where the care goes. The order is fixed and each step guards the next: parse, then
 * check the schema version, then apply any migrations, then map into domain records, then validate
 * them. Version before mapping, because a payload from a newer app must be refused rather than
 * half-understood; migration before mapping, because the mapper only knows the current shape.
 */
internal object JsonBackupSerializer {

    private val json = Json {
        // A file written by a newer build may carry fields this one has never heard of. Ignoring
        // them is what lets an older app still read - and refuse politely - instead of crashing.
        ignoreUnknownKeys = true
        encodeDefaults = true
        // Nothing in the format is polymorphic, so there is no class discriminator to abuse.
        classDiscriminator = "#unused"
    }

    fun serialize(payload: BackupPayload): ByteArray =
        json.encodeToString(BackupDtoMapper.toDto(payload)).toByteArray(Charsets.UTF_8)

    fun deserialize(bytes: ByteArray): BackupPayload {
        val dto = try {
            json.decodeFromString<BackupPayloadDto>(bytes.toString(Charsets.UTF_8))
        } catch (error: SerializationException) {
            throw BackupFailureException(BackupErrorType.VALIDATION_ERROR, error)
        } catch (error: IllegalArgumentException) {
            throw BackupFailureException(BackupErrorType.VALIDATION_ERROR, error)
        }

        BackupSchemaSupport.require(dto.schemaVersion)
        val current = BackupPayloadMigrations.migrateToCurrent(dto)

        val payload = BackupDtoMapper.toDomain(current)
        BackupRecordValidator.require(payload)
        return payload
    }
}

/**
 * Logical migrations of the payload, one step per schema version.
 *
 * The same idea as a Room migration and deliberately not the same mechanism: this chain upgrades a
 * *document* that may have been sitting in someone's Drive for a year, while a Room migration
 * upgrades the database on this device. Their version numbers move for unrelated reasons, so tying
 * them together would mean a database change could orphan existing backups.
 *
 * Schema 1 is the current shape, so the chain is empty. It exists now, with the version check and
 * the seam in place, because retrofitting one after the first incompatible change is exactly the
 * situation where old files get dropped.
 */
internal object BackupPayloadMigrations {

    fun migrateToCurrent(dto: BackupPayloadDto): BackupPayloadDto {
        var current = dto
        while (current.schemaVersion < BackupFormat.SCHEMA_VERSION) {
            current = migrateOnce(current)
        }
        return current
    }

    @Suppress("UNUSED_PARAMETER")
    private fun migrateOnce(dto: BackupPayloadDto): BackupPayloadDto =
        // No step exists yet. Reaching this point means a version passed the support check without
        // a migration to match, which is a bug in this file rather than a problem with the file.
        throw BackupFailureException(BackupErrorType.UNSUPPORTED_VERSION)
}
