package com.griff.keeper.domain.backup

import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The rules that decide what an import does, with nothing else in the way. */
class BackupMergerTest {

    private val earlier = Instant.parse("2026-01-01T00:00:00Z")
    private val later = Instant.parse("2026-06-01T00:00:00Z")

    @Test
    fun `merge keeps local records and adds the ones only the backup has`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(
                testSubscription(id = "A", name = "Spotify"),
                testSubscription(id = "B", name = "Netflix"),
            ),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(
                testSubscription(id = "A", name = "Spotify"),
                testSubscription(id = "C", name = "ChatGPT"),
            ),
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, local, payload)

        // Spotify is already there and identical, so it is not written again; only ChatGPT is new.
        assertEquals(listOf("C"), plan.subscriptions.toInsert.map { it.id.value })
        assertTrue(plan.subscriptions.toUpdate.isEmpty())
        assertEquals(1, plan.subscriptions.counts.unchanged)
        assertEquals(0, plan.subscriptions.counts.removed)
    }

    @Test
    fun `importing the same backup twice changes nothing the second time`() {
        val payload = payload(
            subscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            obligations = listOf(testObligation(id = "X", name = "OC Ford")),
        )
        val afterFirstImport = LocalDataSnapshot(
            subscriptions = payload.subscriptions,
            obligations = payload.obligations,
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, afterFirstImport, payload)

        assertTrue(plan.subscriptions.toInsert.isEmpty())
        assertTrue(plan.subscriptions.toUpdate.isEmpty())
        assertTrue(plan.obligations.toInsert.isEmpty())
        assertTrue(plan.obligations.toUpdate.isEmpty())
        assertEquals(1, plan.subscriptions.counts.unchanged)
        assertEquals(1, plan.obligations.counts.unchanged)
    }

    @Test
    fun `the newer version of a record wins`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(
                testSubscription(id = "A", name = "Spotify", createdAt = earlier),
            ),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(
                testSubscription(
                    id = "A",
                    name = "Spotify Family",
                    priceMinorUnits = 5_999,
                    createdAt = later,
                ),
            ),
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, local, payload)

        assertEquals(1, plan.subscriptions.toUpdate.size)
        assertEquals("Spotify Family", plan.subscriptions.toUpdate.single().name.value)
        assertEquals(Money.ofMinorUnits(5_999), plan.subscriptions.toUpdate.single().price)
    }

    @Test
    fun `an older version from the backup does not overwrite a newer local record`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(
                testSubscription(id = "A", name = "Spotify Family", createdAt = later),
            ),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(testSubscription(id = "A", name = "Spotify", createdAt = earlier)),
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, local, payload)

        assertTrue(plan.subscriptions.toUpdate.isEmpty())
        assertEquals(1, plan.subscriptions.counts.unchanged)
    }

    @Test
    fun `an equal timestamp keeps the local record`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(
                testSubscription(id = "A", name = "Local", createdAt = earlier),
            ),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(testSubscription(id = "A", name = "Backup", createdAt = earlier)),
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, local, payload)

        // Rewriting a record whose timestamp says it is the same age gains nothing and would lose
        // the local one for no reason.
        assertTrue(plan.subscriptions.toUpdate.isEmpty())
        assertEquals(1, plan.subscriptions.counts.unchanged)
    }

    @Test
    fun `records with the same name but different ids are never merged`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(testSubscription(id = "B", name = "Spotify")),
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, local, payload)

        // Two rows called Spotify can be two deliberate records; only an id is evidence.
        assertEquals(listOf("B"), plan.subscriptions.toInsert.map { it.id.value })
        assertEquals(1, plan.possibleDuplicates)
    }

    @Test
    fun `a name collision is reported case insensitively`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(testSubscription(id = "A", name = "spotify")),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(testSubscription(id = "B", name = "Spotify")),
        )

        assertEquals(1, BackupMerger.plan(ImportMode.MERGE, local, payload).possibleDuplicates)
    }

    @Test
    fun `a matching id is not counted as a possible duplicate`() {
        val local = LocalDataSnapshot(
            subscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
            obligations = emptyList(),
        )
        val payload = payload(
            subscriptions = listOf(testSubscription(id = "A", name = "Spotify")),
        )

        assertEquals(0, BackupMerger.plan(ImportMode.MERGE, local, payload).possibleDuplicates)
    }

    @Test
    fun `replace writes the whole backup and drops the rest`() {
        val local = LocalDataSnapshot(
            subscriptions = List(5) { testSubscription(id = "local-$it", name = "Local $it") },
            obligations = List(3) { testObligation(id = "obl-$it", name = "Policy $it") },
        )
        val payload = payload(
            subscriptions = List(2) { testSubscription(id = "backup-$it", name = "Backup $it") },
            obligations = listOf(testObligation(id = "backup-obl", name = "Backup policy")),
        )

        val plan = BackupMerger.plan(ImportMode.REPLACE, local, payload)

        assertEquals(2, plan.subscriptions.toInsert.size)
        assertEquals(1, plan.obligations.toInsert.size)
        assertEquals(5, plan.subscriptions.counts.removed)
        assertEquals(3, plan.obligations.counts.removed)
        assertTrue(plan.isReplacing)
    }

    @Test
    fun `a repeated id inside the backup is applied once`() {
        val payload = payload(
            subscriptions = listOf(
                testSubscription(id = "A", name = "First", createdAt = earlier),
                testSubscription(id = "A", name = "Second", createdAt = later),
            ),
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, LocalDataSnapshot.Empty, payload)

        assertEquals(1, plan.subscriptions.toInsert.size)
        // The first occurrence decides, so the result cannot depend on write order.
        assertEquals("First", plan.subscriptions.toInsert.single().name.value)
    }

    @Test
    fun `portable settings follow the backup in both modes`() {
        val payload = payload(globalRemindersEnabled = false)

        listOf(ImportMode.MERGE, ImportMode.REPLACE).forEach { mode ->
            val plan = BackupMerger.plan(mode, LocalDataSnapshot.Empty, payload)
            assertEquals(false, plan.settings.globalRemindersEnabled, "mode=$mode")
        }
    }

    @Test
    fun `an empty device restores everything the backup carries`() {
        val payload = payload(
            subscriptions = List(12) { testSubscription(id = "s-$it", name = "Service $it") },
            obligations = List(6) { testObligation(id = "o-$it", name = "Policy $it") },
        )

        val plan = BackupMerger.plan(ImportMode.MERGE, LocalDataSnapshot.Empty, payload)

        assertEquals(12, plan.importedSubscriptionCount)
        assertEquals(6, plan.importedObligationCount)
    }

    private fun payload(
        subscriptions: List<com.griff.keeper.domain.model.Subscription> = emptyList(),
        obligations: List<com.griff.keeper.domain.model.Obligation> = emptyList(),
        globalRemindersEnabled: Boolean = true,
    ) = BackupPayload(
        schemaVersion = BackupFormat.SCHEMA_VERSION,
        exportedAt = later,
        appVersion = "1.3.0",
        subscriptions = subscriptions,
        obligations = obligations,
        settings = PortableSettings.Default.copy(
            globalRemindersEnabled = globalRemindersEnabled,
        ),
    )
}
