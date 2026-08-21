package com.griff.keeper.presentation.common.locale

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.griff.keeper.presentation.R
import java.util.Locale

/**
 * A language the interface is shipped in.
 *
 * A presentation concept, not a domain one: nothing about a subscription or an insurance policy
 * changes with the language, and no record stores one. The tag is the BCP 47 language tag that
 * names the matching `values-*` resource folder, and the display name is a resource rather than a
 * string in the enum so the picker reads the same words the rest of the UI does.
 */
enum class AppLanguage(
    val languageTag: String,
    @get:StringRes val displayNameRes: Int,
) {
    POLISH("pl", R.string.language_polish),
    ENGLISH("en", R.string.language_english),
    ;

    companion object {

        /**
         * What an unsupported system language falls back to, matching the unqualified `values/`
         * resource folder. A phone set to German gets English, not Polish.
         */
        val Fallback: AppLanguage = ENGLISH

        fun forTag(tag: String): AppLanguage? =
            entries.firstOrNull { it.languageTag.equals(Locale.forLanguageTag(tag).language, true) }
    }
}

/**
 * Reads and writes the language of the app.
 *
 * There is deliberately no repository, no use case and no preference of our own behind this. Android
 * already owns "which language is this app in": from Android 13 the platform stores it, below that
 * AppCompat does (see `autoStoreLocales` in the manifest), and both survive process death, a restart
 * and a reboot. A second copy in DataStore would only be a second answer to the same question.
 */
object AppLanguages {

    /**
     * The language the UI is currently in.
     *
     * Empty locales mean the user has never chosen one, so the answer is whatever the system
     * language resolves to - and a system language the app does not ship resolves to [Fallback],
     * exactly as Android's own resource lookup does.
     */
    fun current(
        applied: LocaleListCompat = AppCompatDelegate.getApplicationLocales(),
        systemLocale: Locale = Locale.getDefault(),
    ): AppLanguage = resolve(applied, systemLocale)

    /** Applies [language] to the whole app. Android recreates the activities that are showing. */
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag),
        )
    }

    /**
     * Split out from [current] with no Android types of its own, so the fallback rules are unit
     * testable without a device.
     */
    internal fun resolve(applied: LocaleListCompat, systemLocale: Locale): AppLanguage {
        for (index in 0 until applied.size()) {
            applied[index]?.let { chosen ->
                AppLanguage.forTag(chosen.toLanguageTag())?.let { return it }
            }
        }
        return AppLanguage.forTag(systemLocale.toLanguageTag()) ?: AppLanguage.Fallback
    }
}
