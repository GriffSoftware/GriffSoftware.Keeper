package com.griff.keeper.domain.backup

import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.validation.ObligationInputValidator
import com.griff.keeper.domain.validation.PriceParser
import java.time.Instant
import java.time.LocalDate

/**
 * Decides whether this build can read a payload of a given schema version at all.
 *
 * Kept apart from the Room migrations it resembles: the two version numbers answer different
 * questions, and a build with a brand new database may still have to read a backup written by the
 * very first release.
 */
object BackupSchemaSupport {

    /**
     * @throws BackupFailureException with [BackupErrorType.UNSUPPORTED_VERSION] when the payload
     * comes from a newer app than this one, or from a schema no migration path reaches any more.
     */
    fun require(schemaVersion: Int) {
        if (schemaVersion > BackupFormat.SCHEMA_VERSION ||
            schemaVersion < BackupFormat.OLDEST_SUPPORTED_SCHEMA_VERSION
        ) {
            throw BackupFailureException(BackupErrorType.UNSUPPORTED_VERSION)
        }
    }
}

/**
 * Domain validation of a decrypted payload.
 *
 * Runs on data that has already been decrypted and authenticated, and still treats it as hostile.
 * Authentication proves the file has not been altered *since it was written*; it says nothing about
 * whether the thing that wrote it was a healthy version of this app. A file can come from a build
 * with a bug, from a device whose storage lied, or from someone who wrote their own exporter, so
 * every record goes through the same limits a typed-in record would.
 *
 * Structural parsing - "is this even a number, is that a known enum" - happens where the bytes are
 * read. What is checked here is meaning: amounts inside the range the app can add up, strings short
 * enough to belong in a record, dates a formatter can render, ids that appear once.
 *
 * Deliberately *not* checked: whether dates lie in the past or the future. A policy that expired
 * last year and a tax due in 2031 are both perfectly good history.
 */
object BackupRecordValidator {

    /** Amounts stay inside the range the price parser accepts, so totals cannot overflow. */
    const val MAX_AMOUNT_MINOR_UNITS: Long = PriceParser.MAX_UNITS * Money.MINOR_UNITS_PER_UNIT

    /**
     * A ceiling on how many records a single file may carry.
     *
     * Far above any real collection and far below what a crafted file could ask the app to allocate
     * and to render.
     */
    const val MAX_RECORDS_PER_KIND: Int = 10_000

    const val MAX_APP_VERSION_LENGTH: Int = 40

    /** Dates a `DateTimeFormatter` and the statistics calculators can handle without surprises. */
    val EARLIEST_DATE: LocalDate = LocalDate.of(1900, 1, 1)
    val LATEST_DATE: LocalDate = LocalDate.of(2999, 12, 31)

    private val EARLIEST_INSTANT: Instant = EARLIEST_DATE.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
    private val LATEST_INSTANT: Instant = LATEST_DATE.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()

    /**
     * @throws BackupFailureException with [BackupErrorType.VALIDATION_ERROR] on the first problem.
     *
     * All-or-nothing on purpose: a backup is one document, and quietly dropping the records that
     * did not pass would restore something the user never had.
     */
    fun require(payload: BackupPayload) {
        ensure(payload.appVersion.length <= MAX_APP_VERSION_LENGTH)
        ensure(payload.exportedAt.isSane())
        ensure(payload.subscriptions.size <= MAX_RECORDS_PER_KIND)
        ensure(payload.obligations.size <= MAX_RECORDS_PER_KIND)
        ensure(payload.subscriptions.distinctBy { it.id.value }.size == payload.subscriptions.size)
        ensure(payload.obligations.distinctBy { it.id.value }.size == payload.obligations.size)

        payload.subscriptions.forEach(::requireValid)
        payload.obligations.forEach(::requireValid)
        requireValid(payload.settings)
    }

    private fun requireValid(subscription: Subscription) {
        ensure(subscription.price.minorUnits in 0..MAX_AMOUNT_MINOR_UNITS)
        ensure(subscription.providerId.value.length <= MAX_PROVIDER_ID_LENGTH)
        ensure((subscription.managementUrl?.value?.length ?: 0) <= MAX_URL_LENGTH)
        ensure(subscription.nextBillingDate.isSane())
        ensure(subscription.createdAt.isSane())
        ensure(subscription.updatedAt.isSane())
    }

    private fun requireValid(obligation: Obligation) {
        ensure(obligation.amount.minorUnits in 0..MAX_AMOUNT_MINOR_UNITS)
        ensure((obligation.notes?.length ?: 0) <= ObligationInputValidator.MAX_NOTES_LENGTH)
        ensure(obligation.dueDate.isSane())
        ensure(obligation.validUntil.isSane())
        ensure(obligation.paymentDate.isSane())
        ensure(obligation.createdAt.isSane())
        ensure(obligation.updatedAt.isSane())
    }

    private fun requireValid(settings: PortableSettings) {
        val schedules = with(settings.reminderDefaults) { listOf(insurance, payment, subscription) }
        schedules.forEach { schedule ->
            ensure(schedule.rules.size <= MAX_REMINDER_RULES)
            ensure(schedule.daysBefore.all { it in 0..MAX_REMINDER_DAYS_BEFORE })
        }
    }

    private fun LocalDate?.isSane(): Boolean =
        this == null || (this >= EARLIEST_DATE && this <= LATEST_DATE)

    private fun Instant.isSane(): Boolean = this >= EARLIEST_INSTANT && this <= LATEST_INSTANT

    private fun ensure(condition: Boolean) {
        if (!condition) throw BackupFailureException(BackupErrorType.VALIDATION_ERROR)
    }

    private const val MAX_PROVIDER_ID_LENGTH = 64
    private const val MAX_URL_LENGTH = 2_048
    private const val MAX_REMINDER_RULES = 12
    private const val MAX_REMINDER_DAYS_BEFORE = 365
}
