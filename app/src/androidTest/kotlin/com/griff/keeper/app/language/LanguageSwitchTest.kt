package com.griff.keeper.app.language

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.keeper.app.MainActivity
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.locale.AppLanguage
import com.griff.keeper.presentation.common.locale.AppLanguages
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Changing the language from the drawer, and everything that must not change with it.
 *
 * The activity is launched by the test rather than by a rule, because choosing a language recreates
 * it: a rule that owns the launch would be holding a reference to an activity the platform has just
 * thrown away. For the same reason the starting language is applied *after* the launch rather than
 * before it - launching into a locale change that has not been applied yet means racing the
 * platform, and [launchIn] instead goes through the same recreation the assertions already wait for.
 */
@RunWith(AndroidJUnit4::class)
class LanguageSwitchTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val polish = appContext.localizedFor(AppLanguage.POLISH)
    private val english = appContext.localizedFor(AppLanguage.ENGLISH)

    @AfterTest
    fun clearTheChosenLanguage() {
        // Back to "the user never chose", so one test cannot decide what the next one starts in.
        setAppLanguage(null)
    }

    @Test
    fun theDrawerShowsTheLanguageThatIsActive() {
        launchIn(AppLanguage.POLISH).use {
            openDrawer(polish)

            // Label and current value in one row, which is also what TalkBack reads.
            composeRule
                .onNodeWithContentDescription(languageRowDescription(polish, "Polski"))
                .assertIsDisplayed()
        }
    }

    @Test
    fun switchingFromPolishToEnglishTranslatesTheWholeApp() {
        launchIn(AppLanguage.POLISH).use {
            openDrawer(polish)
            openLanguagePicker(polish, "Polski")

            // The picker itself is in the language that is still active.
            composeRule.onNodeWithText(polish.getString(R.string.language_dialog_title))
                .assertIsDisplayed()
            composeRule.onNodeWithText("English").performClick()

            // The screen behind the drawer is translated, not only the drawer.
            awaitText(english.getString(R.string.subscriptions_search_placeholder))
            assertEquals(AppLanguage.ENGLISH, AppLanguages.current())

            openDrawer(english)
            composeRule
                .onNodeWithContentDescription(languageRowDescription(english, "English"))
                .assertIsDisplayed()
            composeRule.onNodeWithText(english.getString(R.string.drawer_statistics))
                .assertIsDisplayed()
        }
    }

    @Test
    fun switchingFromEnglishToPolishTranslatesTheWholeApp() {
        launchIn(AppLanguage.ENGLISH).use {
            openDrawer(english)
            openLanguagePicker(english, "English")

            composeRule.onNodeWithText(english.getString(R.string.language_dialog_title))
                .assertIsDisplayed()
            composeRule.onNodeWithText("Polski").performClick()

            awaitText(polish.getString(R.string.subscriptions_search_placeholder))
            assertEquals(AppLanguage.POLISH, AppLanguages.current())

            openDrawer(polish)
            composeRule
                .onNodeWithContentDescription(languageRowDescription(polish, "Polski"))
                .assertIsDisplayed()
            composeRule.onNodeWithText(polish.getString(R.string.drawer_statistics))
                .assertIsDisplayed()
        }
    }

    @Test
    fun theChosenLanguageSurvivesRelaunchingTheApp() {
        launchIn(AppLanguage.POLISH).use {
            openDrawer(polish)
            openLanguagePicker(polish, "Polski")
            composeRule.onNodeWithText("English").performClick()
            awaitText(english.getString(R.string.subscriptions_search_placeholder))
        }

        // A fresh launch, with the language read back through the platform's own storage rather than
        // anything the test kept. A real process death cannot be forced from here - the test process
        // would go down with the app - so this covers the activity being started again from scratch.
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText(english.getString(R.string.subscriptions_search_placeholder))
            assertEquals(AppLanguage.ENGLISH, AppLanguages.current())
            openDrawer(english)
            composeRule
                .onNodeWithContentDescription(languageRowDescription(english, "English"))
                .assertIsDisplayed()
        }
    }

    @Test
    fun dismissingThePickerLeavesTheLanguageAlone() {
        launchIn(AppLanguage.POLISH).use {
            openDrawer(polish)
            openLanguagePicker(polish, "Polski")
            composeRule.onNodeWithText(polish.getString(R.string.action_cancel)).performClick()

            composeRule.waitForIdle()
            assertEquals(AppLanguage.POLISH, AppLanguages.current())
            composeRule
                .onNodeWithContentDescription(languageRowDescription(polish, "Polski"))
                .assertIsDisplayed()
        }
    }

    @Test
    fun thePickerDoesNotComeBackByItselfAfterTheLanguageChanges() {
        launchIn(AppLanguage.POLISH).use {
            openDrawer(polish)
            openLanguagePicker(polish, "Polski")
            composeRule.onNodeWithText("English").performClick()

            awaitText(english.getString(R.string.subscriptions_search_placeholder))
            composeRule.waitForIdle()

            // Recreating the activity must not restore "the picker was open", in either language.
            composeRule
                .onAllNodesWithText(english.getString(R.string.language_dialog_title))
                .assertCountEquals(0)
            composeRule
                .onAllNodesWithText(polish.getString(R.string.language_dialog_title))
                .assertCountEquals(0)
        }
    }

    @Test
    fun theUserStaysOnTheDestinationTheyWereReading() {
        launchIn(AppLanguage.POLISH).use {
            // About, rather than the start destination, is the case that would go unnoticed: an
            // activity recreation that loses the back stack lands the user back on the subscriptions
            // list, and switching language from the start destination would look identical either way.
            openDestination(polish, R.string.drawer_about)
            awaitText(polish.getString(R.string.about_features_title))

            openDrawer(polish)
            openLanguagePicker(polish, "Polski")
            composeRule.onNodeWithText("English").performClick()

            // Same screen, new language. Navigation state survives the recreation.
            awaitText(english.getString(R.string.about_features_title))
            composeRule
                .onNodeWithText(english.getString(R.string.about_features_title))
                .assertIsDisplayed()
        }
    }

    @Test
    fun changingTheLanguageDoesNotChangeTheTheme() {
        val nightModeBefore = AppCompatDelegate.getDefaultNightMode()

        launchIn(AppLanguage.POLISH).use { scenario ->
            val uiModeBefore = uiMode(scenario)

            openDrawer(polish)
            openLanguagePicker(polish, "Polski")
            composeRule.onNodeWithText("English").performClick()
            awaitText(english.getString(R.string.subscriptions_search_placeholder))

            // Light stays light and dark stays dark: a locale change reconfigures the locale and
            // nothing else.
            assertEquals(uiModeBefore, uiMode(scenario))
            assertEquals(nightModeBefore, AppCompatDelegate.getDefaultNightMode())
        }
    }

    /**
     * Launches the app and brings it to [language], waiting until the UI is actually in it.
     *
     * Setting the language before the launch would leave a locale change pending against an activity
     * that does not exist yet, and the launch then races the recreation.
     */
    private fun launchIn(language: AppLanguage): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        if (AppLanguages.current() != language) {
            setAppLanguage(language)
        }
        awaitContentDescription(contextFor(language).getString(R.string.open_menu))
        return scenario
    }

    /** Reads the night-mode bits of the configuration the activity is actually running with. */
    private fun uiMode(scenario: ActivityScenario<MainActivity>): Int {
        var mode = -1
        scenario.onActivity { activity ->
            mode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        }
        return mode
    }

    private fun contextFor(language: AppLanguage): Context = when (language) {
        AppLanguage.POLISH -> polish
        AppLanguage.ENGLISH -> english
    }

    /**
     * Navigates through the drawer.
     *
     * The drawer item is matched on its role as well as its text: a destination and the screen it
     * opens are named with the same string, and picking the screen's own title instead would leave
     * the drawer open with every later click landing on the scrim.
     */
    private fun openDestination(context: Context, labelRes: Int) {
        openDrawer(context)
        val label = context.getString(labelRes)
        awaitText(label)
        composeRule
            .onNode(
                hasText(label) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
            )
            .performClick()
        composeRule.waitForIdle()
    }

    private fun openDrawer(context: Context) {
        val label = context.getString(R.string.open_menu)
        awaitContentDescription(label)
        composeRule.onNodeWithContentDescription(label).performClick()
        composeRule.waitForIdle()
    }

    private fun openLanguagePicker(context: Context, languageName: String) {
        val description = languageRowDescription(context, languageName)
        awaitContentDescription(description)
        composeRule.onNodeWithContentDescription(description).performClick()
        composeRule.waitForIdle()
    }

    private fun languageRowDescription(context: Context, languageName: String): String =
        context.getString(
            R.string.language_item_description,
            context.getString(R.string.drawer_language),
            languageName,
        )

    /**
     * Applying a language recreates the activity, so the next assertion has to wait for the new
     * composition rather than for the old one to settle.
     */
    private fun awaitText(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitContentDescription(description: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
