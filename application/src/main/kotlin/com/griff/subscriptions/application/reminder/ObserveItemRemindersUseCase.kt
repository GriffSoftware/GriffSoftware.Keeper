package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.reminder.ReminderEventStore
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.ReminderSettingsRepository
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Reminder state of a single subscription, for its details screen. */
class ObserveSubscriptionReminderUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val settings: ReminderSettingsRepository,
    private val events: ReminderEventStore,
    private val factory: ReminderItemFactory,
    private val clock: ClockProvider,
) {
    operator fun invoke(id: SubscriptionId): Flow<ItemReminderState?> = combine(
        repository.observeById(id),
        settings.observe(),
        events.observeDeliveredKeys(),
    ) { subscription, reminderSettings, delivered ->
        subscription?.let {
            val item = factory.itemOf(it, reminderSettings.defaults, clock.today(), delivered)
            factory.stateOf(item, reminderSettings.globalEnabled)
        }
    }
}

/** Reminder state of a single obligation, for its details screen. */
class ObserveObligationReminderUseCase @Inject constructor(
    private val repository: ObligationRepository,
    private val settings: ReminderSettingsRepository,
    private val events: ReminderEventStore,
    private val factory: ReminderItemFactory,
    private val clock: ClockProvider,
) {
    operator fun invoke(id: ObligationId): Flow<ItemReminderState?> = combine(
        repository.observeById(id),
        settings.observe(),
        events.observeDeliveredKeys(),
    ) { obligation, reminderSettings, delivered ->
        obligation?.let {
            val item = factory.itemOf(it, reminderSettings.defaults, clock.today(), delivered)
            factory.stateOf(item, reminderSettings.globalEnabled)
        }
    }
}
