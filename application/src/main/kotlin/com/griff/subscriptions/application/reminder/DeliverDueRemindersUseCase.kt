package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.domain.reminder.NotificationAvailability
import com.griff.subscriptions.domain.reminder.ReminderAvailability
import com.griff.subscriptions.domain.reminder.ReminderCandidate
import com.griff.subscriptions.domain.reminder.ReminderEventStore
import com.griff.subscriptions.domain.reminder.ReminderNotification
import com.griff.subscriptions.domain.reminder.ReminderPlanner
import com.griff.subscriptions.domain.reminder.ReminderPublisher
import com.griff.subscriptions.domain.reminder.ReminderSource
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.ReminderSettingsRepository
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Works out which reminders today implies, shows them, and records that it did.
 *
 * This is the whole engine, and it is a pure re-evaluation of the current records: nothing is
 * scheduled per reminder, so an edited date, a renewed subscription, a settled charge or a deleted
 * record simply produce a different answer on the next run. There is no queue that can end up
 * describing a record that no longer exists.
 *
 * Called from a background worker, but knows nothing about one - the platform decides when to ask.
 */
class DeliverDueRemindersUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val settings: ReminderSettingsRepository,
    private val events: ReminderEventStore,
    private val availability: NotificationAvailability,
    private val publisher: ReminderPublisher,
    private val factory: ReminderItemFactory,
    private val clock: ClockProvider,
) {

    /** Returns how many reminders were published, which the caller can log or assert on. */
    suspend operator fun invoke(): Int {
        val reminderSettings = settings.current()
        val systemEnabled = availability.areNotificationsEnabled()

        // Nothing is marked as delivered while the user cannot see it: a reminder suppressed today
        // by a global switch has not been given, and must still be possible tomorrow.
        if (!reminderSettings.globalEnabled || !systemEnabled) return 0

        val today = clock.today()
        val defaults = reminderSettings.defaults
        val delivered = events.deliveredKeys().toMutableSet()

        val candidates = buildList {
            subscriptions.observeAll().first()
                .mapNotNullTo(this) { factory.candidateOf(it, defaults) }
            obligations.observeAll().first()
                .mapNotNullTo(this) { factory.candidateOf(it, defaults) }
        }

        var published = 0
        for (candidate in candidates) {
            if (!isEffective(candidate, reminderSettings.globalEnabled, systemEnabled)) continue

            for (occurrence in ReminderPlanner.dueOn(candidate, today)) {
                if (!delivered.add(occurrence.key)) continue

                publisher.publish(
                    ReminderNotification(
                        occurrence = occurrence,
                        title = candidate.title,
                        amount = candidate.amount,
                        currency = candidate.currency,
                        billingPeriod = (candidate.source as? ReminderSource.Subscription)
                            ?.billingPeriod,
                    ),
                )
                events.markDelivered(occurrence.key, clock.now())
                published++
            }
        }

        events.deleteSentBefore(clock.now().minus(HistoryRetention))
        return published
    }

    private fun isEffective(
        candidate: ReminderCandidate,
        globalEnabled: Boolean,
        systemEnabled: Boolean,
    ): Boolean = ReminderAvailability.isEffective(
        globalEnabled = globalEnabled,
        itemEnabled = candidate.remindersEnabled,
        systemNotificationsEnabled = systemEnabled,
    )

    private companion object {
        /**
         * Long enough that a reminder cannot be repeated within any plausible cycle, short enough
         * that the table stays a deduplication ledger rather than a history nobody asked for.
         */
        val HistoryRetention: Duration = Duration.ofDays(400)
    }
}
