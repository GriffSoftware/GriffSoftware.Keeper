package com.griff.keeper.domain.backup

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription

/** What an import will do to one kind of record, in numbers the user can read. */
data class ImportCounts(
    val inserted: Int,
    val updated: Int,
    val unchanged: Int,
    /** Local records the import removes. Always zero for [ImportMode.MERGE]. */
    val removed: Int,
) {
    /** How many records the imported data ends up contributing. */
    val touched: Int get() = inserted + updated

    companion object {
        val Empty: ImportCounts = ImportCounts(0, 0, 0, 0)
    }
}

/** The decided fate of one kind of record: exactly which rows to write, and how many to leave. */
data class ImportSection<T>(
    val toInsert: List<T>,
    val toUpdate: List<T>,
    val unchanged: Int,
    val removed: Int,
) {
    val counts: ImportCounts = ImportCounts(
        inserted = toInsert.size,
        updated = toUpdate.size,
        unchanged = unchanged,
        removed = removed,
    )

    companion object {
        fun <T> empty(): ImportSection<T> = ImportSection(emptyList(), emptyList(), 0, 0)
    }
}

/**
 * The complete, decided outcome of an import - computed before a single row is written.
 *
 * Splitting the decision from the write is what makes the operation reviewable and testable: the
 * plan can be shown to the user, asserted on in a test and handed to a transaction, and the
 * persistence layer never has to make a judgement call of its own.
 */
data class ImportPlan(
    val mode: ImportMode,
    val subscriptions: ImportSection<Subscription>,
    val obligations: ImportSection<Obligation>,
    val settings: PortableSettings,
    /**
     * Records that share a name with a local one but not an id.
     *
     * Reported, never acted on: two records called "Spotify" can be two deliberate rows, and only a
     * stable id is evidence that they are the same thing.
     */
    val possibleDuplicates: Int,
) {
    /** True when the plan would replace every local portable record. */
    val isReplacing: Boolean get() = mode == ImportMode.REPLACE

    val importedSubscriptionCount: Int get() = subscriptions.counts.touched

    val importedObligationCount: Int get() = obligations.counts.touched
}

/** The local portable data an import is planned against. */
data class LocalDataSnapshot(
    val subscriptions: List<Subscription>,
    val obligations: List<Obligation>,
    val appCurrency: Currency = Currency.Default,
) {
    val isEmpty: Boolean get() = subscriptions.isEmpty() && obligations.isEmpty()

    val recordCount: Int get() = subscriptions.size + obligations.size

    companion object {
        val Empty: LocalDataSnapshot = LocalDataSnapshot(emptyList(), emptyList(), Currency.Default)
    }
}
