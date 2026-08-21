package com.griff.keeper.infrastructure.reminder

import android.content.Context
import android.content.res.Configuration
import androidx.core.os.LocaleListCompat
import androidx.core.os.ConfigurationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.reminder.ReminderKind
import com.griff.keeper.domain.reminder.ReminderNotification
import com.griff.keeper.domain.reminder.ReminderOccurrence
import com.griff.keeper.domain.reminder.ReminderSourceType
import com.griff.keeper.infrastructure.R
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/**
 * A reminder speaks the language the user chose for the app.
 *
 * The one place in the app where the wrong language is easy to ship and hard to notice. Notifications
 * are built by a daily worker with no activity alive, so nothing has applied the per-app locale to
 * the context it is handed - and below Android 13 the application context still carries the *system*
 * language. An app in English on a Polish phone would then show English screens and Polish
 * notifications, and no screenshot would ever reveal it.
 *
 * The locale is applied to the context directly, exactly as [withAppLocale] does when the user has
 * chosen a language, so the assertions hold on every API level the app supports rather than only on
 * the ones where the platform happens to do the work.
 */
@RunWith(AndroidJUnit4::class)
class ReminderNotificationLocalizationTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val polish = localizedContext("pl")
    private val english = localizedContext("en")

    @Test
    fun insuranceExpiryIsWrittenInPolish() {
        val copy = ReminderNotificationTextFactory(polish).copyFor(insuranceExpiry(days = 7))

        assertEquals("OC Ford", copy.title)
        assertEquals("Ubezpieczenie wygasa za 7 dni", copy.subText)
        assertTrue(copy.contentText.contains("września"), copy.contentText)
    }

    @Test
    fun insuranceExpiryIsWrittenInEnglish() {
        val copy = ReminderNotificationTextFactory(english).copyFor(insuranceExpiry(days = 7))

        // The record's own name is user data and is never translated.
        assertEquals("OC Ford", copy.title)
        assertEquals("Insurance expires in 7 days", copy.subText)
        assertTrue(copy.contentText.contains("September"), copy.contentText)
    }

    @Test
    fun polishPluralsAreCorrectOnTheFirstSecondAndFifthDay() {
        // The reason plurals are resources: Polish needs three forms where English needs two.
        assertEquals(
            "Ubezpieczenie wygasa za 2 dni",
            ReminderNotificationTextFactory(polish).copyFor(insuranceExpiry(days = 2)).subText,
        )
        assertEquals(
            "Ubezpieczenie wygasa za 5 dni",
            ReminderNotificationTextFactory(polish).copyFor(insuranceExpiry(days = 5)).subText,
        )
        // One day out is its own phrase, not a plural.
        assertEquals(
            "Ubezpieczenie wygasa jutro",
            ReminderNotificationTextFactory(polish).copyFor(insuranceExpiry(days = 1)).subText,
        )
    }

    @Test
    fun englishPluralsAreCorrectOnTheFirstSecondAndFifthDay() {
        assertEquals(
            "Insurance expires in 2 days",
            ReminderNotificationTextFactory(english).copyFor(insuranceExpiry(days = 2)).subText,
        )
        assertEquals(
            "Insurance expires in 5 days",
            ReminderNotificationTextFactory(english).copyFor(insuranceExpiry(days = 5)).subText,
        )
        assertEquals(
            "Insurance expires tomorrow",
            ReminderNotificationTextFactory(english).copyFor(insuranceExpiry(days = 1)).subText,
        )
    }

    @Test
    fun anAmountCarriesTheCurrencySymbolOfTheActiveLanguage() {
        val polishCopy = ReminderNotificationTextFactory(polish).copyFor(paymentDue())
        val englishCopy = ReminderNotificationTextFactory(english).copyFor(paymentDue())

        // The same money, named the way each reader expects: a comma and `zł` against a dot and
        // `PLN`. The grouping separator is deliberately left out of the assertion - CLDR uses a
        // non-breaking space for Polish and has changed which one more than once.
        assertTrue(polishCopy.contentText.contains("240,00 zł"), polishCopy.contentText)
        assertTrue(englishCopy.contentText.contains("240.00 PLN"), englishCopy.contentText)
    }

    @Test
    fun aRenewalNamesTheDayAndMonthInTheOrderTheLanguageUses() {
        val polishCopy = ReminderNotificationTextFactory(polish).copyFor(renewal())
        val englishCopy = ReminderNotificationTextFactory(english).copyFor(renewal())

        assertTrue(polishCopy.contentText.contains("28 sierpnia"), polishCopy.contentText)
        assertTrue(englishCopy.contentText.contains("August 28"), englishCopy.contentText)
    }

    @Test
    fun everyLineOfTheNotificationChangesWithTheLanguage() {
        val polishCopy = ReminderNotificationTextFactory(polish).copyFor(insuranceExpiry(days = 30))
        val englishCopy = ReminderNotificationTextFactory(english).copyFor(insuranceExpiry(days = 30))

        // Half-translated copy is the failure mode being guarded against: subtext from one language
        // next to a date from another.
        assertNotEquals(polishCopy.subText, englishCopy.subText)
        assertNotEquals(polishCopy.contentText, englishCopy.contentText)
    }

    @Test
    fun theNotificationChannelIsNamedInBothLanguages() {
        assertEquals("Przypomnienia", polish.getString(R.string.reminder_channel_name))
        assertEquals("Reminders", english.getString(R.string.reminder_channel_name))
        assertTrue(polish.getString(R.string.reminder_channel_description).isNotBlank())
        assertTrue(english.getString(R.string.reminder_channel_description).isNotBlank())
    }

    @Test
    fun aContextWithNoChosenLanguageIsLeftAlone() {
        // No stored locales means the user never picked one, and the system language is the right
        // answer - so withAppLocale has nothing to override.
        val locales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            assertEquals(
                ConfigurationCompat.getLocales(appContext.resources.configuration)[0],
                appContext.withAppLocale().resolvedLocale(),
            )
        }
    }

    private fun localizedContext(languageTag: String): Context {
        val configuration = Configuration(appContext.resources.configuration)
        ConfigurationCompat.setLocales(
            configuration,
            LocaleListCompat.forLanguageTags(languageTag),
        )
        return appContext.createConfigurationContext(configuration)
    }

    private fun insuranceExpiry(days: Int) = ReminderNotification(
        occurrence = ReminderOccurrence(
            sourceType = ReminderSourceType.OBLIGATION,
            sourceId = "obligation-1",
            kind = ReminderKind.INSURANCE_EXPIRY,
            targetDate = LocalDate.of(2026, 9, 20),
            daysBefore = days,
        ),
        title = "OC Ford",
        amount = Money.ofMinorUnits(124_000),
        currency = Currency.PLN,
        billingPeriod = null,
    )

    private fun paymentDue() = ReminderNotification(
        occurrence = ReminderOccurrence(
            sourceType = ReminderSourceType.OBLIGATION,
            sourceId = "obligation-2",
            kind = ReminderKind.PAYMENT_DUE,
            targetDate = LocalDate.of(2026, 9, 20),
            daysBefore = 7,
        ),
        title = "Podatek od nieruchomości",
        amount = Money.ofMinorUnits(124_000),
        currency = Currency.PLN,
        billingPeriod = null,
    )

    private fun renewal() = ReminderNotification(
        occurrence = ReminderOccurrence(
            sourceType = ReminderSourceType.SUBSCRIPTION,
            sourceId = "subscription-1",
            kind = ReminderKind.SUBSCRIPTION_RENEWAL,
            targetDate = LocalDate.of(2026, 8, 28),
            daysBefore = 7,
        ),
        title = "Spotify",
        amount = Money.ofUnits(34, 99),
        currency = Currency.PLN,
        billingPeriod = BillingPeriod.MONTHLY,
    )
}
