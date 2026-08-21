package com.griff.keeper.domain.backup

import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription
import java.time.Instant

/**
 * Decides what an import does, without touching anything.
 *
 * ### Merge strategy
 *
 * Both aggregates carry a stable, storage-independent id - a UUID minted once when the record is
 * created and never rewritten - so identity survives a trip through a backup file and the merge can
 * be decided on it alone. For a record the backup and the device both know:
 *
 * - identical content: nothing to do. This is what makes importing the same file twice a no-op
 *   instead of a source of duplicates.
 * - different content: the newer `updatedAt` wins. Both models update that field on every write, so
 *   it is a real "last edited" timestamp and not a creation date in disguise. Ties keep the local
 *   row, because rewriting a record with an identical timestamp gains nothing and loses the local
 *   one for no reason.
 *
 * A record the device does not know is inserted. A local record the backup does not mention is left
 * alone in [ImportMode.MERGE] - a merge adds, it does not prune - and dropped in
 * [ImportMode.REPLACE].
 *
 * Records that merely *look* alike (same name, different id) are counted and reported, never
 * merged: only the id is evidence.
 *
 * ### Settings
 *
 * Portable preferences follow the backup in both modes. They are a single small block that
 * describes an intent ("remind me"), not a set of records to reconcile, so a partial merge of it
 * would be a guess; the file the user chose to restore is the more recent statement of that intent.
 */
object BackupMerger {

    fun plan(
        mode: ImportMode,
        local: LocalDataSnapshot,
        payload: BackupPayload,
    ): ImportPlan = when (mode) {
        ImportMode.MERGE -> ImportPlan(
            mode = mode,
            subscriptions = mergeSection(
                local = local.subscriptions,
                incoming = payload.subscriptions,
                id = { it.id.value },
                updatedAt = Subscription::updatedAt,
            ),
            obligations = mergeSection(
                local = local.obligations,
                incoming = payload.obligations,
                id = { it.id.value },
                updatedAt = Obligation::updatedAt,
            ),
            settings = payload.settings,
            possibleDuplicates = countPossibleDuplicates(local, payload),
        )

        ImportMode.REPLACE -> ImportPlan(
            mode = mode,
            subscriptions = ImportSection(
                toInsert = payload.subscriptions,
                toUpdate = emptyList(),
                unchanged = 0,
                removed = local.subscriptions.size,
            ),
            obligations = ImportSection(
                toInsert = payload.obligations,
                toUpdate = emptyList(),
                unchanged = 0,
                removed = local.obligations.size,
            ),
            settings = payload.settings,
            possibleDuplicates = 0,
        )
    }

    private fun <T> mergeSection(
        local: List<T>,
        incoming: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> Instant,
    ): ImportSection<T> {
        val localById = local.associateBy(id)
        val toInsert = mutableListOf<T>()
        val toUpdate = mutableListOf<T>()
        var unchanged = 0

        // A malformed file could name the same id twice; the first occurrence decides, so the
        // outcome cannot depend on which of two writes happened to land last.
        val seen = mutableSetOf<String>()

        for (record in incoming) {
            val recordId = id(record)
            if (!seen.add(recordId)) continue

            val existing = localById[recordId]
            when {
                existing == null -> toInsert += record
                existing == record -> unchanged++
                updatedAt(record) > updatedAt(existing) -> toUpdate += record
                else -> unchanged++
            }
        }

        return ImportSection(
            toInsert = toInsert,
            toUpdate = toUpdate,
            unchanged = unchanged,
            removed = 0,
        )
    }

    /**
     * Counts incoming records whose name matches a local record with a different id.
     *
     * Names are compared case-insensitively after trimming, which is how a person reads them. The
     * number is a hint for the preview - "you may end up with two of these" - and never an action.
     */
    private fun countPossibleDuplicates(
        local: LocalDataSnapshot,
        payload: BackupPayload,
    ): Int {
        val subscriptions = countNameCollisions(
            localNamesById = local.subscriptions.associate { it.id.value to it.name.value },
            incoming = payload.subscriptions.map { it.id.value to it.name.value },
        )
        val obligations = countNameCollisions(
            localNamesById = local.obligations.associate { it.id.value to it.name.value },
            incoming = payload.obligations.map { it.id.value to it.name.value },
        )
        return subscriptions + obligations
    }

    private fun countNameCollisions(
        localNamesById: Map<String, String>,
        incoming: List<Pair<String, String>>,
    ): Int {
        val localNames = localNamesById.values.mapTo(mutableSetOf()) { it.normalizedName() }
        return incoming.count { (id, name) ->
            id !in localNamesById && name.normalizedName() in localNames
        }
    }

    private fun String.normalizedName(): String = trim().lowercase()
}
