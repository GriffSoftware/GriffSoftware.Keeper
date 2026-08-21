package com.griff.subscriptions.infrastructure.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.griff.subscriptions.application.reminder.DeliverDueRemindersUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException

/**
 * Asks the reminder engine, once a day, what today implies.
 *
 * A daily sweep rather than one scheduled alarm per reminder: a record's dates change, records are
 * deleted and charges are settled, and every one of those would otherwise mean cancelling and
 * re-registering alarms - state that can end up describing something that no longer exists. Re-
 * evaluating the current records instead is both simpler and impossible to get out of sync.
 *
 * It also means the app needs no exact alarms. "A reminder is due today" is the information the user
 * wants; the exact minute it arrives is not.
 */
class CheckRemindersWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    /**
     * Dependencies are pulled from the Hilt singleton graph rather than injected into the worker.
     *
     * WorkManager instantiates workers by class name, so a custom factory would be the alternative;
     * an entry point keeps the wiring to these three lines and leaves the app's initialization
     * untouched.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun deliverDueReminders(): DeliverDueRemindersUseCase
    }

    override suspend fun doWork(): Result {
        val deliverDueReminders = EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .deliverDueReminders()

        return try {
            deliverDueReminders()
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            // The next daily run will look at exactly the same records, so a transient failure needs
            // no retry storm; nothing has been marked as delivered that was not shown.
            Result.retry()
        }
    }

    companion object {
        /** One scheduler for the whole app; see [WorkManagerReminderScheduler]. */
        const val UNIQUE_WORK_NAME = "griff_reminder_check"

        /** The one-off run that catches up on today when the app is opened. */
        const val CATCH_UP_WORK_NAME = "griff_reminder_check_now"
    }
}
