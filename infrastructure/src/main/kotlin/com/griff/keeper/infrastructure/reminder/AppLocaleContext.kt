package com.griff.keeper.infrastructure.reminder

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * A context whose resources resolve against the language the *user picked for this app*, not the one
 * the phone happens to be set to.
 *
 * Reminders are built by a daily worker that runs with no activity alive, so there is nothing to
 * inherit a configuration from. On Android 13+ the platform has already applied the per-app locale
 * to the process and this returns an equivalent context; below that, AppCompat only ever reconfigures
 * activities, and the application context a worker is handed still carries the *system* language -
 * which is exactly how a notification ends up in the wrong language while every screen is right.
 *
 * The empty case is deliberate: no stored locales means the user never chose one, so the system
 * language is the correct answer and Android's own resource fallback (to the unqualified `values/`,
 * English) applies.
 */
internal fun Context.withAppLocale(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this

    val configuration = Configuration(resources.configuration)
    ConfigurationCompat.setLocales(configuration, locales)
    return createConfigurationContext(configuration)
}

/**
 * The locale [this] context resolves resources against.
 *
 * Notification copy mixes resource strings with dates and amounts, and the two have to come from the
 * same language: `java.time` and `java.text` read a [Locale], not a resource folder.
 */
internal fun Context.resolvedLocale(): Locale =
    ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.getDefault()
