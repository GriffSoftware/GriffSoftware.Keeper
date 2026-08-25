package com.griff.keeper.infrastructure.settings

import com.griff.keeper.domain.backup.PortableSettings
import com.griff.keeper.domain.backup.PortableSettingsRepository
import com.griff.keeper.domain.reminder.ReminderDefaults
import com.griff.keeper.domain.repository.AppCurrencyRepository
import com.griff.keeper.domain.repository.ReminderSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The preferences a backup carries, read from and written to the stores that already own them.
 *
 * A view over existing repositories rather than a store of its own: duplicating the app-wide reminder
 * switch into a "backup settings" file would create a second answer to the same question, and the two
 * would drift the first time one of them was written without the other.
 *
 * ### What is portable, and what is not
 *
 * Portable, and therefore exported:
 * - the app-wide reminder switch, from the `reminder_settings` preferences;
 * - each record's own reminder flag, which travels inside the record itself;
 * - the active app currency, from the `app_currency` preference - it is a decision about how to read
 *   the very amounts in this file, not device-bound state.
 *
 * Not portable, and therefore never exported:
 * - the Android notification permission and the notification channel - granted by the system to this
 *   installation, meaningless anywhere else;
 * - the reminder delivery ledger, which says what *this* device has already shown the user;
 * - the import/export history, which describes this installation's own operations;
 * - anything in a cache, any temporary URI, and any identifier of the device or the process.
 *
 * [ReminderDefaults] is included in the payload for forward compatibility but cannot be applied:
 * the schedules are constants in this build, with nowhere to store an override. A later version that
 * lets the user edit them will be able to read today's files unchanged.
 *
 * Applying [appCurrency] here only ever happens as part of an already decided import (see
 * `ImportBackupUseCase`): it is a plain preference write, never the transactional record conversion a
 * user-initiated currency *switch* requires - that is `ChangeAppCurrencyUseCase`'s job, not this
 * class's.
 */
@Singleton
class PortableSettingsDataStore @Inject constructor(
    private val reminderSettings: ReminderSettingsRepository,
    private val appCurrency: AppCurrencyRepository,
) : PortableSettingsRepository {

    override suspend fun current(): PortableSettings {
        val settings = reminderSettings.current()
        return PortableSettings(
            globalRemindersEnabled = settings.globalEnabled,
            reminderDefaults = settings.defaults,
            appCurrency = appCurrency.current(),
        )
    }

    /**
     * Writes the portable preferences.
     *
     * Two preferences, two atomic edits - which is what lets the import use case treat this as a step
     * it can reverse if the database write that follows it fails.
     */
    override suspend fun apply(settings: PortableSettings) {
        reminderSettings.setGlobalEnabled(settings.globalRemindersEnabled)
        appCurrency.set(settings.appCurrency)
    }
}
