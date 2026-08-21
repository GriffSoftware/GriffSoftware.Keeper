package com.griff.keeper.domain.backup

import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.reminder.ReminderDefaults
import java.time.Instant

/**
 * Everything a backup carries, as domain objects.
 *
 * The list is exhaustive by design, and what it leaves out matters as much as what it holds. Device
 * bound state is not portable and is never included: the Android notification permission, the
 * notification channel, the reminder deduplication ledger (importing "already told the user about
 * this" from another phone would silence reminders that this phone has never shown), the
 * import/export history and anything living in a cache.
 */
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Instant,
    val appVersion: String,
    val subscriptions: List<Subscription>,
    val obligations: List<Obligation>,
    val settings: PortableSettings,
) {
    fun summary(): BackupSummary = BackupSummary(
        schemaVersion = schemaVersion,
        createdAt = exportedAt,
        appVersion = appVersion,
        subscriptionCount = subscriptions.size,
        obligationCount = obligations.size,
        hasSettings = true,
    )
}

/**
 * User preferences that mean the same thing on any device.
 *
 * The app-wide reminder switch is a decision the user made about their data, so it travels with it.
 * [reminderDefaults] is carried for completeness - the rules are constants in this build, so an
 * import cannot change them, but writing them into the file means a later version that *does* let
 * the user edit them can read old backups without a schema bump.
 */
data class PortableSettings(
    val globalRemindersEnabled: Boolean,
    val reminderDefaults: ReminderDefaults,
) {
    companion object {
        val Default: PortableSettings = PortableSettings(
            globalRemindersEnabled = true,
            reminderDefaults = ReminderDefaults.Standard,
        )
    }
}

/**
 * What a backup says about itself, shown in the import preview before anything is written.
 *
 * Derived from the decrypted payload, never from the file name or from the unencrypted header: the
 * user can rename a file, and the header is deliberately free of anything about their data.
 */
data class BackupSummary(
    val schemaVersion: Int,
    val createdAt: Instant,
    val appVersion: String,
    val subscriptionCount: Int,
    val obligationCount: Int,
    val hasSettings: Boolean,
) {
    val recordCount: Int get() = subscriptionCount + obligationCount
}

/** What the level-one check can tell about a file without knowing the password. */
data class BackupFileInfo(
    val fileName: String?,
    val sizeBytes: Long,
    val formatVersion: Int,
)
