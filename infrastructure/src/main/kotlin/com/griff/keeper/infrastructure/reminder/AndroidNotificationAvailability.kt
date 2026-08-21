package com.griff.keeper.infrastructure.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.griff.keeper.domain.reminder.NotificationAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks Android whether the app may post notifications at all.
 *
 * Read on every call rather than cached: the user can leave the app, switch notifications off in the
 * system settings and come back, and the screen has to tell the truth when they do.
 */
@Singleton
class AndroidNotificationAvailability @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationAvailability {

    override fun areNotificationsEnabled(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        // App level permission is not the whole story: the reminder channel itself can be blocked,
        // in which case the app is allowed to post and nothing is ever shown.
        val channel = manager.getNotificationChannelCompat(ReminderNotificationChannel.ID)
        return channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
    }
}
