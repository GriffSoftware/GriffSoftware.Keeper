package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.domain.reminder.ReminderEventStore
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.ReminderSettingsRepository
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Streams the whole reminders screen from the records themselves.
 *
 * Everything is recomputed on every change instead of being stored: a date the user edits, a record
 * they delete and a reminder that has just gone out all reach the screen through the same path, and
 * no reminder state can be left behind pointing at something that no longer exists.
 */
class ObserveReminderDashboardUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val settings: ReminderSettingsRepository,
    private val events: ReminderEventStore,
    private val factory: ReminderItemFactory,
    private val clock: ClockProvider,
) {
    operator fun invoke(): Flow<ReminderDashboard> = combine(
        subscriptions.observeAll(),
        obligations.observeAll(),
        settings.observe(),
        events.observeDeliveredKeys(),
    ) { subscriptionList, obligationList, reminderSettings, delivered ->
        val today = clock.today()
        val defaults = reminderSettings.defaults

        val items = buildList {
            subscriptionList.mapTo(this) { factory.itemOf(it, defaults, today, delivered) }
            obligationList.mapTo(this) { factory.itemOf(it, defaults, today, delivered) }
        }

        ReminderDashboard(
            globalEnabled = reminderSettings.globalEnabled,
            defaults = defaults,
            // Most urgent first; records the engine cannot schedule keep a stable order by name so
            // the bottom section does not reshuffle on every emission.
            items = items.sortedWith(
                compareBy(
                    { it.nextReminder?.fireDate ?: LocalDate.MAX },
                    { it.title.lowercase() },
                ),
            ),
        )
    }
}
