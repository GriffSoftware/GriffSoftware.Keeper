package com.griff.keeper.domain.testing

import com.griff.keeper.domain.backup.BackupCodec
import com.griff.keeper.domain.backup.BackupErrorType
import com.griff.keeper.domain.backup.BackupFailureException
import com.griff.keeper.domain.backup.BackupFileReader
import com.griff.keeper.domain.backup.BackupFileSharing
import com.griff.keeper.domain.backup.BackupFileWriter
import com.griff.keeper.domain.backup.BackupImportRepository
import com.griff.keeper.domain.backup.BackupOperation
import com.griff.keeper.domain.backup.BackupOperationRepository
import com.griff.keeper.domain.backup.BackupPayload
import com.griff.keeper.domain.backup.BackupSink
import com.griff.keeper.domain.backup.BackupSource
import com.griff.keeper.domain.backup.ImportMode
import com.griff.keeper.domain.backup.ImportPlan
import com.griff.keeper.domain.backup.NetworkAvailability
import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.backup.PortableSettingsRepository
import com.griff.keeper.domain.backup.SharedBackupFile
import com.griff.keeper.domain.id.BackupOperationIdGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Test doubles for the backup ports.
 *
 * They live next to the other domain doubles because they implement domain ports, and production code
 * never sees them - `testFixtures` is only on the test classpath. The real cryptography, the real
 * file format and the real Room transaction are tested against their own implementations; these
 * exist so the *decisions* around them can be tested without either.
 */

/** In-memory import/export log, newest first, with the same cap semantics as the real table. */
class FakeBackupOperationRepository(
    initial: List<BackupOperation> = emptyList(),
) : BackupOperationRepository {

    private val state = MutableStateFlow(initial)

    val recorded: List<BackupOperation> get() = state.value

    override fun observeRecent(limit: Int): Flow<List<BackupOperation>> =
        state.map { operations -> operations.sortedByDescending { it.finishedAt }.take(limit) }

    override suspend fun record(operation: BackupOperation) {
        state.value = state.value + operation
    }
}

/**
 * Applies an [ImportPlan] to the in-memory repositories.
 *
 * Honours the port's all-or-nothing contract the way the Room implementation does: when
 * [failOnApply] is set it refuses *before* touching anything, which is what a rolled-back transaction
 * looks like from the outside. That the real implementation actually gets that right is pinned
 * separately, against a real database.
 */
class FakeBackupImportRepository(
    private val subscriptions: FakeSubscriptionRepository,
    private val obligations: FakeObligationRepository,
) : BackupImportRepository {

    var failOnApply: BackupErrorType? = null

    var appliedPlans: Int = 0
        private set

    override suspend fun apply(plan: ImportPlan) {
        failOnApply?.let { throw BackupFailureException(it) }

        if (plan.mode == ImportMode.REPLACE) {
            subscriptions.stored.forEach { subscriptions.delete(it.id) }
            obligations.stored.forEach { obligations.delete(it.id) }
        }

        val existingSubscriptions = subscriptions.stored.map { it.id }.toSet()
        (plan.subscriptions.toInsert + plan.subscriptions.toUpdate).forEach { subscription ->
            if (subscription.id in existingSubscriptions) {
                subscriptions.update(subscription)
            } else {
                subscriptions.add(subscription)
            }
        }

        val existingObligations = obligations.stored.map { it.id }.toSet()
        (plan.obligations.toInsert + plan.obligations.toUpdate).forEach { obligation ->
            if (obligation.id in existingObligations) {
                obligations.update(obligation)
            } else {
                obligations.add(obligation)
            }
        }

        appliedPlans++
    }
}

/** In-memory portable preferences, so a rollback can be observed. */
class FakePortableSettingsRepository(
    initial: PortableSettings = PortableSettings.Default,
) : PortableSettingsRepository {

    var stored: PortableSettings = initial
        private set

    /** Every value this has ever held, in order, so a restore can be told from "never changed". */
    val writes: MutableList<PortableSettings> = mutableListOf()

    override suspend fun current(): PortableSettings = stored

    override suspend fun apply(settings: PortableSettings) {
        stored = settings
        writes += settings
    }
}

/**
 * Stands in for encryption without doing any.
 *
 * A file is the index of a registered payload plus a password, so a wrong password and a corrupt file
 * are both reproducible without cryptography. The real primitives are exercised by the codec's own
 * tests, which is where they belong.
 */
class FakeBackupCodec : BackupCodec {

    private val stored = mutableMapOf<String, Pair<BackupPayload, String>>()

    var failOnInspect: BackupErrorType? = null
    var failOnEncode: BackupErrorType? = null

    /** Registers a payload and returns the bytes that "contain" it. */
    fun register(payload: BackupPayload, password: String): ByteArray {
        val token = "fake-backup-${stored.size}"
        stored[token] = payload to password
        return token.toByteArray(Charsets.UTF_8)
    }

    override suspend fun encode(payload: BackupPayload, password: CharArray): ByteArray {
        failOnEncode?.let { throw BackupFailureException(it) }
        return register(payload, String(password))
    }

    override suspend fun inspect(bytes: ByteArray): Int {
        failOnInspect?.let { throw BackupFailureException(it) }
        if (bytes.toString(Charsets.UTF_8) !in stored) {
            throw BackupFailureException(BackupErrorType.INVALID_FILE)
        }
        return 1
    }

    override suspend fun decode(bytes: ByteArray, password: CharArray): BackupPayload {
        val entry = stored[bytes.toString(Charsets.UTF_8)]
            ?: throw BackupFailureException(BackupErrorType.INVALID_FILE)
        if (entry.second != String(password)) {
            throw BackupFailureException(BackupErrorType.WRONG_PASSWORD_OR_CORRUPTED)
        }
        return entry.first
    }
}

/** Reads whatever the source hands over, with optional failure injection. */
class FakeBackupFileReader : BackupFileReader {

    var failOnRead: BackupErrorType? = null

    override suspend fun read(source: BackupSource): ByteArray {
        failOnRead?.let { throw BackupFailureException(it) }
        return source.openInputStream().use { it.readBytes() }
    }
}

class FakeBackupFileWriter : BackupFileWriter {

    var failOnWrite: BackupErrorType? = null

    var written: ByteArray? = null
        private set

    override suspend fun write(sink: BackupSink, bytes: ByteArray) {
        failOnWrite?.let { throw BackupFailureException(it) }
        sink.openOutputStream().use { it.write(bytes) }
        written = bytes
    }
}

/** Records what would have been staged for another app to read. */
class FakeBackupFileSharing : BackupFileSharing {

    var failOnStage: BackupErrorType? = null

    var staged: SharedBackupFile? = null
        private set

    var clearCount: Int = 0
        private set

    override suspend fun stage(fileName: String, bytes: ByteArray): SharedBackupFile {
        failOnStage?.let { throw BackupFailureException(it) }
        return SharedBackupFile(uri = "content://test/$fileName", fileName = fileName)
            .also { staged = it }
    }

    override suspend fun clear() {
        clearCount++
        staged = null
    }
}

class FakeNetworkAvailability(var online: Boolean = true) : NetworkAvailability {
    override fun isOnline(): Boolean = online
}

/** Predictable log ids: `backup-1`, `backup-2`, ... */
class SequentialBackupOperationIdGenerator : BackupOperationIdGenerator {
    private var counter = 0

    override fun next(): String {
        counter++
        return "backup-$counter"
    }
}

/** A destination that keeps what was written, so a test can look at the bytes. */
class InMemoryBackupSink : BackupSink {

    private val buffer = ByteArrayOutputStream()

    var failOnOpen: Boolean = false

    val bytes: ByteArray get() = buffer.toByteArray()

    override fun openOutputStream(): OutputStream {
        if (failOnOpen) throw IOException("Simulated write failure")
        return buffer
    }
}

/** A file the user "picked", built from bytes a test already has. */
class InMemoryBackupSource(
    private val content: ByteArray,
    override val displayName: String? = "griff-backup-2026-08-21-0043.griffbackup",
    override val sizeBytes: Long? = content.size.toLong(),
) : BackupSource {

    override fun openInputStream(): InputStream = ByteArrayInputStream(content)
}
