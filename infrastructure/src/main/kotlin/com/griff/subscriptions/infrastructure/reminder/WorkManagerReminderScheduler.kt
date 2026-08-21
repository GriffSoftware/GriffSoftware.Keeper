package com.griff.subscriptions.infrastructure.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.griff.subscriptions.domain.reminder.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the daily reminder check with WorkManager.
 *
 * WorkManager rather than an alarm: it already survives a reboot and a force stop, it respects Doze
 * instead of fighting it, and it needs no `BOOT_COMPLETED` receiver of the app's own. No constraints
 * are attached - every piece of data the check needs is on the device, so it has to work in airplane
 * mode as well as on Wi-Fi.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderScheduler {

    override fun ensureScheduled() {
        val workManager = WorkManager.getInstance(context)

        workManager.enqueueUniquePeriodicWork(
            CheckRemindersWorker.UNIQUE_WORK_NAME,
            // UPDATE keeps the existing schedule and its history instead of restarting the interval
            // on every launch, which KEEP would do only by ignoring changes to the request itself.
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<CheckRemindersWorker>(
                repeatInterval = Interval,
                flexTimeInterval = Flex,
            ).build(),
        )

        // Periodic work first runs inside its flex window, which on a fresh install is most of a day
        // away - long enough to miss a reminder that is due today. A single catch-up run on launch
        // closes that gap; it is safe to repeat because the check is idempotent, and REPLACE means
        // several launches in a row queue one run rather than a pile of them.
        workManager.enqueueUniqueWork(
            CheckRemindersWorker.CATCH_UP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CheckRemindersWorker>().build(),
        )
    }

    private companion object {
        /**
         * Once a day is enough: the shortest reminder in the app is "one day before", and a deadline
         * a day away does not need to be re-checked every fifteen minutes.
         */
        val Interval: Duration = Duration.ofDays(1)

        /**
         * A wide flex window lets Android batch the check with whatever else it is already waking up
         * for, which is the difference between a background task and a battery complaint.
         */
        val Flex: Duration = Duration.ofHours(6)
    }
}
