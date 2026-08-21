package com.griff.subscriptions.domain.repository

import com.griff.subscriptions.domain.reminder.ReminderSettings
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for the app-wide reminder configuration.
 *
 * Separate from the record repositories on purpose: this is a handful of preferences, not entities,
 * and storing them next to the user's subscriptions would give a checkbox the same weight - and the
 * same migration cost - as their data.
 */
interface ReminderSettingsRepository {

    fun observe(): Flow<ReminderSettings>

    suspend fun current(): ReminderSettings

    suspend fun setGlobalEnabled(enabled: Boolean)
}
