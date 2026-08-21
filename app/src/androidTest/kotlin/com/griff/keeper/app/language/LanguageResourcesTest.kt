package com.griff.keeper.app.language

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.locale.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.runner.RunWith

/**
 * The copy each language actually resolves to, checked against the real merged resources.
 *
 * This is where the words themselves are pinned down; the UI tests then only have to prove that the
 * app is *using* the right language. Cheap, and it fails on the exact resource that is wrong rather
 * than on "a node was not found".
 */
@RunWith(AndroidJUnit4::class)
class LanguageResourcesTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val polish = appContext.localizedFor(AppLanguage.POLISH)
    private val english = appContext.localizedFor(AppLanguage.ENGLISH)

    @Test
    fun theMainDestinationsAreNamedInPolish() {
        assertEquals("Subskrypcje", polish.getString(R.string.drawer_subscriptions))
        assertEquals("Ubezpieczenia i opłaty", polish.getString(R.string.drawer_obligations))
        assertEquals("Statystyki", polish.getString(R.string.drawer_statistics))
        assertEquals("Przypomnienia", polish.getString(R.string.drawer_reminders))
        assertEquals("Język", polish.getString(R.string.drawer_language))
    }

    @Test
    fun theMainDestinationsAreNamedInEnglish() {
        assertEquals("Subscriptions", english.getString(R.string.drawer_subscriptions))
        assertEquals("Insurance & obligations", english.getString(R.string.drawer_obligations))
        assertEquals("Statistics", english.getString(R.string.drawer_statistics))
        assertEquals("Reminders", english.getString(R.string.drawer_reminders))
        assertEquals("Language", english.getString(R.string.drawer_language))
    }

    @Test
    fun languageNamesAreSelfNamesInBothLanguages() {
        // Someone who switched by accident has to be able to recognise their own language in the
        // picker, so "Polski" is never rendered as "Polish".
        listOf(polish, english).forEach { context ->
            assertEquals("Polski", context.getString(R.string.language_polish))
            assertEquals("English", context.getString(R.string.language_english))
        }
    }

    @Test
    fun theLanguagePickerIsTranslated() {
        assertEquals("Wybierz język", polish.getString(R.string.language_dialog_title))
        assertEquals("Choose language", english.getString(R.string.language_dialog_title))
        assertEquals("Anuluj", polish.getString(R.string.action_cancel))
        assertEquals("Cancel", english.getString(R.string.action_cancel))
    }

    @Test
    fun polishPluralsUseTheThreeFormsPolishNeeds() {
        // The reason plurals are resources and not an `if (days == 1)`.
        assertEquals("Za 1 dzień", quantity(polish, R.plurals.reminders_relative_days, 1))
        assertEquals("Za 2 dni", quantity(polish, R.plurals.reminders_relative_days, 2))
        assertEquals("Za 5 dni", quantity(polish, R.plurals.reminders_relative_days, 5))
        assertEquals("Za 22 dni", quantity(polish, R.plurals.reminders_relative_days, 22))

        assertEquals("1 dzień", quantity(polish, R.plurals.reminders_days_before, 1))
        assertEquals("2 dni", quantity(polish, R.plurals.reminders_days_before, 2))
        assertEquals("5 dni", quantity(polish, R.plurals.reminders_days_before, 5))
    }

    @Test
    fun englishPluralsUseTheTwoFormsEnglishNeeds() {
        assertEquals("In 1 day", quantity(english, R.plurals.reminders_relative_days, 1))
        assertEquals("In 2 days", quantity(english, R.plurals.reminders_relative_days, 2))
        assertEquals("In 5 days", quantity(english, R.plurals.reminders_relative_days, 5))

        assertEquals("1 day", quantity(english, R.plurals.reminders_days_before, 1))
        assertEquals("2 days", quantity(english, R.plurals.reminders_days_before, 2))
        assertEquals("5 days", quantity(english, R.plurals.reminders_days_before, 5))
    }

    @Test
    fun deadlinePluralsAreTranslated() {
        assertEquals("Wygasa za 30 dni", quantity(polish, R.plurals.deadline_expires_in, 30))
        assertEquals("Expires in 30 days", quantity(english, R.plurals.deadline_expires_in, 30))
        assertEquals("Termin za 1 dzień", quantity(polish, R.plurals.deadline_due_in, 1))
        assertEquals("Due in 1 day", quantity(english, R.plurals.deadline_due_in, 1))
    }

    @Test
    fun obligationTerminologyReadsNaturallyInEnglish() {
        // Literal translations of the Polish would read badly here, so the wording is checked.
        assertEquals("Zapłacono", polish.getString(R.string.payment_status_paid))
        assertEquals("Paid", english.getString(R.string.payment_status_paid))
        assertEquals("Do zapłaty", polish.getString(R.string.payment_status_unpaid))
        assertEquals("To pay", english.getString(R.string.payment_status_unpaid))
    }

    @Test
    fun categoriesAndTagsAreTranslated() {
        assertEquals("Muzyka", polish.getString(R.string.category_music))
        assertEquals("Music", english.getString(R.string.category_music))
        assertEquals("Inne", polish.getString(R.string.category_other))
        assertEquals("Other", english.getString(R.string.category_other))

        // The badge in a list row: short in both languages, and `OC` means nothing in English.
        assertEquals("OC", polish.getString(R.string.tag_vehicle_insurance))
        assertEquals("Vehicle", english.getString(R.string.tag_vehicle_insurance))
        assertEquals(
            "Ubezpieczenie pojazdu",
            polish.getString(R.string.obligation_category_vehicle_insurance),
        )
        assertEquals(
            "Vehicle insurance",
            english.getString(R.string.obligation_category_vehicle_insurance),
        )
    }

    @Test
    fun feedbackAndValidationAreTranslated() {
        assertEquals("Subskrypcja została dodana.", polish.getString(R.string.subscription_added))
        assertEquals("Subscription added.", english.getString(R.string.subscription_added))
        assertEquals("Zmiany zostały zapisane.", polish.getString(R.string.obligation_updated))
        assertEquals("Changes saved.", english.getString(R.string.obligation_updated))
        assertEquals(
            "Kopia zapasowa została utworzona.",
            polish.getString(R.string.data_transfer_export_success),
        )
        assertEquals(
            "Backup created successfully.",
            english.getString(R.string.data_transfer_export_success),
        )
        assertEquals("Podaj cenę", polish.getString(R.string.form_error_price_empty))
        assertEquals("Enter a price", english.getString(R.string.form_error_price_empty))
        assertEquals("Wybierz kategorię", polish.getString(R.string.obligation_form_error_category_missing))
        assertEquals("Pick a category", english.getString(R.string.obligation_form_error_category_missing))
    }

    @Test
    fun accessibilityDescriptionsAreTranslated() {
        // TalkBack must never speak a different language from the screen it is reading.
        assertEquals("Otwórz menu", polish.getString(R.string.open_menu))
        assertEquals("Open menu", english.getString(R.string.open_menu))
        assertEquals("Godło Griff Keeper", polish.getString(R.string.about_emblem_description))
        assertEquals("Griff Keeper emblem", english.getString(R.string.about_emblem_description))
        assertNotEquals(
            polish.getString(R.string.reminders_global_switch_description),
            english.getString(R.string.reminders_global_switch_description),
        )
    }

    @Test
    fun theAboutScreenIsTranslated() {
        assertNotEquals(
            polish.getString(R.string.about_intro),
            english.getString(R.string.about_intro),
        )
        assertEquals("Kontakt i pomoc", polish.getString(R.string.about_contact_title))
        assertEquals("Contact & support", english.getString(R.string.about_contact_title))
        // An address is not copy, and does not change with the language.
        assertEquals(
            polish.getString(R.string.about_contact_email),
            english.getString(R.string.about_contact_email),
        )
        assertEquals("contact@griffsoftware.com", english.getString(R.string.about_contact_email))
    }

    @Test
    fun theBrandNameIsTheSameInBothLanguages() {
        assertEquals(
            polish.getString(R.string.app_display_name),
            english.getString(R.string.app_display_name),
        )
        assertEquals("Griff Keeper", english.getString(R.string.app_display_name))
    }

    @Test
    fun anUnsupportedSystemLanguageFallsBackToEnglish() {
        val german = appContext.localizedForTag("de-DE")

        // Nothing ships in German, so Android resolves to the unqualified values/ folder.
        assertEquals("Subscriptions", german.getString(R.string.drawer_subscriptions))
        assertEquals("Language", german.getString(R.string.drawer_language))
    }

    private fun quantity(context: Context, plural: Int, count: Int): String =
        context.resources.getQuantityString(plural, count, count)

    private fun Context.localizedForTag(tag: String): Context {
        val configuration = android.content.res.Configuration(resources.configuration)
        androidx.core.os.ConfigurationCompat.setLocales(
            configuration,
            androidx.core.os.LocaleListCompat.forLanguageTags(tag),
        )
        return createConfigurationContext(configuration)
    }
}
