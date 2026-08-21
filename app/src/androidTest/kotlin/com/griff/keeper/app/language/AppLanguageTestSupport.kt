package com.griff.keeper.app.language

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.griff.keeper.presentation.common.locale.AppLanguage

/**
 * Applies a language the way the picker does, and waits for it to take effect.
 *
 * `setApplicationLocales` has to be called on the main thread and recreates whatever is showing, so
 * a test that calls it from the instrumentation thread is a test that races the platform.
 */
internal fun setAppLanguage(language: AppLanguage?) {
    val locales = language
        ?.let { LocaleListCompat.forLanguageTags(it.languageTag) }
        ?: LocaleListCompat.getEmptyLocaleList()

    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        AppCompatDelegate.setApplicationLocales(locales)
    }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

/** What the app currently considers its language, read through the same API the drawer uses. */
internal fun appliedLanguage(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()

/**
 * A context whose resources resolve against [language], for reading the expected copy without
 * changing the app's own language.
 */
internal fun Context.localizedFor(language: AppLanguage): Context {
    val configuration = Configuration(resources.configuration)
    ConfigurationCompat.setLocales(
        configuration,
        LocaleListCompat.forLanguageTags(language.languageTag),
    )
    return createConfigurationContext(configuration)
}
