package com.griff.keeper.presentation.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Whether asking for the notification permission is even a thing on this device.
 *
 * Below Android 13 notifications are granted by the manifest and can only be switched off in the
 * system settings, so the screen must offer a different way out there rather than a dialog that
 * would never appear.
 */
internal val isPostNotificationsRuntimePermission: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** The runtime permission to request, or `null` on versions that do not have one. */
internal val postNotificationsPermission: String?
    get() = if (isPostNotificationsRuntimePermission) Manifest.permission.POST_NOTIFICATIONS else null

/** Opens this app's notification settings, the only route left once the permission is refused. */
@Composable
internal fun rememberNotificationSettingsOpener(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { context.openNotificationSettings() } }
}

private fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // A device without the per-app screen still has the app details screen, and an exotic ROM may
    // have neither - a missing settings screen must never take the app down.
    runCatching { startActivity(intent) }.onFailure {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.fromParts("package", packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/**
 * Whether Android currently lets the app post notifications, re-read whenever the screen resumes.
 *
 * The details screens need the same answer as the reminders dashboard, and they need it to survive a
 * trip to the system settings and back - so it is observed rather than sampled once.
 */
@Composable
internal fun rememberSystemNotificationsEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    return enabled
}
