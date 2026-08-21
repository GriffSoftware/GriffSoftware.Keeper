package com.griff.subscriptions.domain.reminder

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Shows a reminder to the user. Implemented by the platform layer.
 *
 * The port takes a fully decided [ReminderNotification]: the domain has already established what is
 * being said and why, so the implementation only chooses how it looks. Nothing about channels,
 * icons or intents leaks back into the domain.
 */
interface ReminderPublisher {

    suspend fun publish(notification: ReminderNotification)
}

/**
 * Remembers which reminders have already been delivered.
 *
 * Deduplication cannot live in memory: the worker runs in a process that is killed between runs, so
 * "have I already said this?" has to survive a restart. Only the key and the time are kept - this is
 * bookkeeping, not a history feature.
 */
interface ReminderEventStore {

    /**
     * Streams the delivered keys so the screens correct themselves the moment one goes out: an
     * item whose "30 days" notice has just fired should start advertising the next one, not the
     * one the user has already seen.
     */
    fun observeDeliveredKeys(): Flow<Set<String>>

    suspend fun deliveredKeys(): Set<String>

    suspend fun markDelivered(key: String, sentAt: Instant)

    /** Drops bookkeeping the engine can no longer need, so the table cannot grow without bound. */
    suspend fun deleteSentBefore(threshold: Instant)
}

/**
 * Whether the operating system currently lets the app post notifications.
 *
 * A user preference and a system permission are different things and the UI has to be able to tell
 * them apart, so this is a port of its own rather than a flag folded into the settings.
 */
interface NotificationAvailability {

    fun areNotificationsEnabled(): Boolean
}

/** Makes sure the periodic reminder check is registered with the platform's scheduler. */
interface ReminderScheduler {

    fun ensureScheduled()
}
