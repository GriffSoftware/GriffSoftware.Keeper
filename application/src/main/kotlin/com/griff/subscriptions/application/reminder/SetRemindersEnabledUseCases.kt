package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.ReminderSettingsRepository
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import javax.inject.Inject

/**
 * Flips the app-wide reminder switch.
 *
 * Only the preference is written. The per-record flags are deliberately left alone, so that turning
 * reminders back on restores the user's earlier choices instead of a row of `true`s.
 */
class SetGlobalRemindersEnabledUseCase @Inject constructor(
    private val settings: ReminderSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = settings.setGlobalEnabled(enabled)
}

/** Turns reminders on or off for one subscription. */
class SetSubscriptionRemindersEnabledUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(id: SubscriptionId, enabled: Boolean): Result<Unit> {
        val existing = repository.findById(id)
            ?: return Result.failure(NoSuchElementException("Subscription $id does not exist"))
        if (existing.remindersEnabled == enabled) return Result.success(Unit)

        repository.update(existing.copy(remindersEnabled = enabled, updatedAt = clock.now()))
        return Result.success(Unit)
    }
}

/** Turns reminders on or off for one obligation. */
class SetObligationRemindersEnabledUseCase @Inject constructor(
    private val repository: ObligationRepository,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(id: ObligationId, enabled: Boolean): Result<Unit> {
        val existing = repository.findById(id)
            ?: return Result.failure(NoSuchElementException("Obligation $id does not exist"))
        if (existing.remindersEnabled == enabled) return Result.success(Unit)

        repository.update(existing.copy(remindersEnabled = enabled, updatedAt = clock.now()))
        return Result.success(Unit)
    }
}
