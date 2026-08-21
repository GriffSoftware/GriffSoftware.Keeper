package com.griff.keeper.infrastructure.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.griff.keeper.domain.reminder.ReminderSourceType

/**
 * Where a reminder takes the user when they tap it.
 *
 * Addressed by URI rather than by activity class: the notification is built in the infrastructure
 * layer, which has no business knowing which activity the app happens to host its navigation in.
 * Restricting the intent to the app's own package keeps the link private to the app even though the
 * scheme is a real one.
 */
internal object ReminderDeepLinks {

    const val SCHEME: String = "griff"
    const val SUBSCRIPTION_HOST: String = "subscription"
    const val OBLIGATION_HOST: String = "obligation"

    fun uriFor(sourceType: ReminderSourceType, sourceId: String): Uri {
        val host = when (sourceType) {
            ReminderSourceType.SUBSCRIPTION -> SUBSCRIPTION_HOST
            ReminderSourceType.OBLIGATION -> OBLIGATION_HOST
        }
        return "$SCHEME://$host/${Uri.encode(sourceId)}".toUri()
    }

    fun intentFor(context: Context, sourceType: ReminderSourceType, sourceId: String): Intent =
        Intent(Intent.ACTION_VIEW, uriFor(sourceType, sourceId)).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
}
