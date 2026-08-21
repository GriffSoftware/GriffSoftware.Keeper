package com.griff.keeper.app

import android.app.Application
import com.griff.keeper.application.reminder.EnsureRemindersScheduledUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Composition root of the app. */
@HiltAndroidApp
class GriffKeeperApplication : Application() {

    @Inject
    lateinit var ensureRemindersScheduled: EnsureRemindersScheduledUseCase

    override fun onCreate() {
        super.onCreate()
        // Idempotent, so doing it on every launch is what keeps the schedule alive after an update,
        // a force stop or a reboot without the app owning a receiver of its own.
        ensureRemindersScheduled()
    }
}
