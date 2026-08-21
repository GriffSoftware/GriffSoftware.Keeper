package com.griff.keeper.presentation.common.locale

import androidx.core.os.LocaleListCompat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The fallback rules, which decide what a fresh install shows before the user has chosen anything.
 *
 * `LocaleListCompat` is one of the few AndroidX types with no Android framework behind it, so this
 * runs as a plain JVM test.
 */
class AppLanguageTest {

    @Test
    fun `an applied language wins over the system language`() {
        assertEquals(
            AppLanguage.POLISH,
            AppLanguages.resolve(LocaleListCompat.forLanguageTags("pl"), Locale.US),
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguages.resolve(LocaleListCompat.forLanguageTags("en"), polish()),
        )
    }

    @Test
    fun `a fresh install follows a supported system language`() {
        assertEquals(
            AppLanguage.POLISH,
            AppLanguages.resolve(LocaleListCompat.getEmptyLocaleList(), polish()),
        )
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguages.resolve(LocaleListCompat.getEmptyLocaleList(), Locale.US),
        )
    }

    @Test
    fun `an unsupported system language falls back to english`() {
        // Matches the unqualified values/ folder, which is what Android's own resource lookup does.
        listOf("de-DE", "fr-FR", "es-ES", "ja-JP").forEach { tag ->
            assertEquals(
                AppLanguage.ENGLISH,
                AppLanguages.resolve(
                    LocaleListCompat.getEmptyLocaleList(),
                    Locale.forLanguageTag(tag),
                ),
                "System language $tag should fall back to English",
            )
        }
    }

    @Test
    fun `a region qualified tag still resolves to its language`() {
        assertEquals(
            AppLanguage.ENGLISH,
            AppLanguages.resolve(LocaleListCompat.forLanguageTags("en-GB"), polish()),
        )
        assertEquals(
            AppLanguage.POLISH,
            AppLanguages.resolve(LocaleListCompat.forLanguageTags("pl-PL"), Locale.US),
        )
    }

    @Test
    fun `the first supported entry of a locale list wins`() {
        // The platform can hand back a list; an unsupported first entry is skipped rather than
        // dropping the whole list on the floor.
        assertEquals(
            AppLanguage.POLISH,
            AppLanguages.resolve(LocaleListCompat.forLanguageTags("de-DE,pl-PL"), Locale.US),
        )
    }

    @Test
    fun `language tags name the resource folders`() {
        assertEquals("pl", AppLanguage.POLISH.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.Fallback)
    }

    @Test
    fun `an unknown tag is not a language`() {
        assertNull(AppLanguage.forTag("de"))
    }

    private fun polish(): Locale = Locale.forLanguageTag("pl-PL")
}
