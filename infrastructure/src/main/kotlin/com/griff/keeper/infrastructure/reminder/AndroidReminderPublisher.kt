package com.griff.keeper.infrastructure.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.griff.keeper.domain.reminder.NotificationAvailability
import com.griff.keeper.domain.reminder.ReminderNotification
import com.griff.keeper.domain.reminder.ReminderPublisher
import com.griff.keeper.infrastructure.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a reminder to the Android notification drawer.
 *
 * The only place in the app that knows what a notification is. Everything it needs has already been
 * decided by the domain, so there is no rule here to get wrong - only presentation: a monochrome
 * small icon as Android requires, the brand accent, a stable id per reminder and a deep link back to
 * the record.
 */
@Singleton
class AndroidReminderPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val availability: NotificationAvailability,
) : ReminderPublisher {

    override suspend fun publish(notification: ReminderNotification) {
        // Resolved per notification rather than once in a field: the user can change the app's
        // language while this singleton is alive, and a worker has no activity to be recreated with.
        val localized = context.withAppLocale()
        // Re-creating the channel with the same id only updates its name and description, which is
        // how the entry in the system notification settings follows the chosen language too.
        ReminderNotificationChannel.ensureCreated(localized)

        // Posting without the permission is a silent no-op on Android 13+, but checking first keeps
        // the intent explicit instead of relying on that behaviour. The check is inline rather than
        // extracted so that lint can see the guard it belongs to. Below Android 13 the permission is
        // not a runtime one and the manifest entry grants it.
        if (!availability.areNotificationsEnabled()) return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val occurrence = notification.occurrence
        val copy = ReminderNotificationTextFactory(localized).copyFor(notification)

        val contentIntent = PendingIntent.getActivity(
            context,
            occurrence.notificationId,
            ReminderDeepLinks.intentFor(context, occurrence.sourceType, occurrence.sourceId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val built = NotificationCompat.Builder(context, ReminderNotificationChannel.ID)
            .setSmallIcon(R.drawable.ic_notification_griff)
            .setColor(ContextCompat.getColor(context, R.color.griff_notification_accent))
            .setContentTitle(copy.title)
            .setSubText(copy.subText)
            .setContentText(copy.contentText)
            // Deadlines read better on two lines than truncated on one.
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Several reminders on the same day stack under one entry instead of burying the drawer,
            // while each keeps its own deep link.
            .setGroup(ReminderNotificationChannel.GROUP_KEY)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(occurrence.notificationId, built)
    }
}
