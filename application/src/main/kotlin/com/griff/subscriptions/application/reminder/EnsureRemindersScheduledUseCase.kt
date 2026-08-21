package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.domain.reminder.ReminderScheduler
import javax.inject.Inject

/**
 * Makes sure the reminder check is registered, whatever the current settings are.
 *
 * Registered unconditionally on purpose: the check itself reads the global switch and the system
 * permission on every run, so a user who turns reminders back on - or grants the permission from the
 * system settings - is covered without the app having to notice.
 */
class EnsureRemindersScheduledUseCase @Inject constructor(
    private val scheduler: ReminderScheduler,
) {
    operator fun invoke() = scheduler.ensureScheduled()
}
