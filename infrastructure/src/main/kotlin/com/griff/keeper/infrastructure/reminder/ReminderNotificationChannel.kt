package com.griff.keeper.infrastructure.reminder

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.griff.keeper.infrastructure.R

/**
 * The single channel every reminder is posted to.
 *
 * Default importance on purpose: a policy that expires in a month or a subscription that renews next
 * week deserves a place in the drawer, not the full-screen, sound-and-vibration treatment Android
 * reserves for alarms. One channel rather than one per kind, so the user has a single, meaningful
 * switch instead of three that all mean "reminders".
 */
internal object ReminderNotificationChannel {

    const val ID: String = "reminders"

    /** Groups the individual reminders so a busy day collapses into one entry in the drawer. */
    const val GROUP_KEY: String = "com.griff.keeper.REMINDERS"

    fun ensureCreated(context: Context) {
        val channel = NotificationChannelCompat.Builder(ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.reminder_channel_name))
            .setDescription(context.getString(R.string.reminder_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
