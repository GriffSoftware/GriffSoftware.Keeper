package com.griff.subscriptions.infrastructure.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.griff.subscriptions.domain.reminder.ReminderDefaults
import com.griff.subscriptions.domain.reminder.ReminderSettings
import com.griff.subscriptions.domain.repository.ReminderSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.reminderPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "reminder_settings",
)

/**
 * [ReminderSettingsRepository] backed by Preferences DataStore.
 *
 * A single switch is a preference, not an entity: putting it in Room would give it a table, a
 * migration and a mapper for one boolean, and would blur the line between the user's data and the
 * app's configuration.
 */
@Singleton
class ReminderSettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderSettingsRepository {

    override fun observe(): Flow<ReminderSettings> = context.reminderPreferences.data
        // A corrupt or unreadable preferences file must not take the reminders screen down with it;
        // falling back to the defaults is both safe and what a fresh install would do anyway.
        .catch { throwable -> if (throwable is IOException) emit(emptyPreferences()) else throw throwable }
        .map { preferences -> preferences.toSettings() }

    override suspend fun current(): ReminderSettings = observe().first()

    override suspend fun setGlobalEnabled(enabled: Boolean) {
        context.reminderPreferences.edit { preferences ->
            preferences[GlobalEnabledKey] = enabled
        }
    }

    /**
     * Missing preference means "on".
     *
     * A user who has never touched the switch should be reminded; whether anything actually reaches
     * them still depends on the Android notification permission, which is a different question and
     * is answered elsewhere.
     */
    private fun Preferences.toSettings() = ReminderSettings(
        globalEnabled = this[GlobalEnabledKey] ?: ReminderSettings.Default.globalEnabled,
        defaults = ReminderDefaults.Standard,
    )

    private companion object {
        val GlobalEnabledKey = booleanPreferencesKey("global_reminders_enabled")
    }
}
