package com.griff.keeper.app.language

import android.Manifest
import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.griff.keeper.app.MainActivity
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.locale.AppLanguage
import com.griff.keeper.presentation.common.locale.AppLanguages
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * What a language change must leave completely alone.
 *
 * Changing the language is a presentation concern and nothing else, but the mechanism behind it -
 * Android recreating every activity - is exactly the kind of thing that quietly resets state that
 * was being held in the wrong place. These tests use the app's real database and real settings store,
 * because the failure being guarded against is a repository or a preference being rebuilt, and a
 * fake would not have one.
 */
@RunWith(AndroidJUnit4::class)
class LanguagePreservesStateTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()


    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val polish = appContext.localizedFor(AppLanguage.POLISH)
    private val english = appContext.localizedFor(AppLanguage.ENGLISH)

    /**
     * Turning reminders on asks Android for permission to post notifications, and a switch that is
     * waiting on a system dialog never moves. Granted through the instrumentation rather than with a
     * `GrantPermissionRule`, which would mean pulling in another test artifact for one line.
     */
    @BeforeTest
    fun allowNotifications() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    @AfterTest
    fun clearTheChosenLanguage() {
        setAppLanguage(null)
    }

    @Test
    fun changingTheLanguageKeepsTheRecordsThatAreAlreadyThere() {
        launchIn(AppLanguage.POLISH).use {
            openDestination(polish, R.string.drawer_obligations)
            addObligation(polish, name = RECORD_NAME)
            awaitText(RECORD_NAME)

            switchLanguage(from = polish, fromName = "Polski", to = "English")

            // The record's own name is data, not copy: it survives the change and is not translated.
            openDestination(english, R.string.drawer_obligations)
            awaitText(RECORD_NAME)
            composeRule.onNodeWithText(RECORD_NAME).assertIsDisplayed()
            // The screen around it *is* translated.
            composeRule
                .onNodeWithText(english.getString(R.string.obligations_search_placeholder))
                .assertIsDisplayed()

            deleteObligation(english, name = RECORD_NAME)
        }
    }

    @Test
    fun changingTheLanguageKeepsTheReminderSettings() {
        launchIn(AppLanguage.POLISH).use {
            openDestination(polish, R.string.drawer_reminders)

            // Whatever the switch is on entry, flip it: the test is about the choice being kept,
            // not about which way it happens to be pointing on this device.
            val switchLabel = polish.getString(R.string.reminders_global_switch_description)
            awaitContentDescription(switchLabel)
            val wasOn = isGlobalRemindersOn(switchLabel)
            composeRule.onNodeWithContentDescription(switchLabel).performClick()
            composeRule.waitForIdle()
            assertRemindersSwitch(switchLabel, isOn = !wasOn)

            switchLanguage(from = polish, fromName = "Polski", to = "English")

            openDestination(english, R.string.drawer_reminders)
            val translatedLabel = english.getString(R.string.reminders_global_switch_description)
            awaitContentDescription(translatedLabel)
            // Recreating the activity must not restore a default over the user's choice.
            assertRemindersSwitch(translatedLabel, isOn = !wasOn)

            // Put it back, so the next test starts from the state this one found.
            composeRule.onNodeWithContentDescription(translatedLabel).performClick()
            composeRule.waitForIdle()
            assertRemindersSwitch(translatedLabel, isOn = wasOn)
        }
    }

    private fun isGlobalRemindersOn(label: String): Boolean =
        composeRule
            .onNodeWithContentDescription(label)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ToggleableState] == ToggleableState.On

    /**
     * The switch is backed by the settings store, so the new state comes back through a flow rather
     * than from the click; waiting for it is the difference between a test and a race.
     */
    private fun assertRemindersSwitch(label: String, isOn: Boolean) {
        await("reminders switch to be ${if (isOn) "on" else "off"}") {
            isGlobalRemindersOn(label) == isOn
        }
        val node = composeRule.onNodeWithContentDescription(label)
        if (isOn) node.assertIsOn() else node.assertIsOff()
    }

    private fun addObligation(context: Context, name: String) {
        val add = context.getString(R.string.obligations_add)
        awaitContentDescription(add)
        composeRule.onNodeWithContentDescription(add).performClick()

        awaitText(context.getString(R.string.obligation_form_name_hint))
        composeRule
            .onNodeWithText(context.getString(R.string.obligation_form_name_hint))
            .performTextInput(name)

        // A category is required, and the chip carries the full category name.
        composeRule
            .onNodeWithText(context.getString(R.string.obligation_category_vehicle_insurance))
            .performClick()

        composeRule
            .onNodeWithText(context.getString(R.string.obligation_form_amount_label))
            .performTextInput("1240")

        // Unpaid, so the form does not also ask for the date it was paid on.
        composeRule
            .onNodeWithText(context.getString(R.string.payment_status_unpaid))
            .performClick()

        val save = context.getString(R.string.obligation_form_save)
        composeRule.onNodeWithText(save).performScrollTo()
        composeRule.onNodeWithText(save).performClick()
        composeRule.waitForIdle()
    }

    private fun deleteObligation(context: Context, name: String) {
        composeRule.onNodeWithText(name).performClick()

        val delete = context.getString(R.string.obligation_details_delete)
        awaitText(delete)
        composeRule.onNodeWithText(delete).performScrollTo()
        composeRule.onNodeWithText(delete).performClick()

        awaitText(context.getString(R.string.delete_dialog_confirm))
        composeRule.onNodeWithText(context.getString(R.string.delete_dialog_confirm)).performClick()
        composeRule.waitForIdle()
    }

    private fun switchLanguage(from: Context, fromName: String, to: String) {
        openDrawer(from)
        val row = from.getString(
            R.string.language_item_description,
            from.getString(R.string.drawer_language),
            fromName,
        )
        awaitContentDescription(row)
        composeRule.onNodeWithContentDescription(row).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(to).performClick()
        composeRule.waitForIdle()
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
            .onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .performClick()
        composeRule.waitForIdle()
    }

    private fun openDrawer(context: Context) {
        val label = context.getString(R.string.open_menu)
        awaitContentDescription(label)
        composeRule.onNodeWithContentDescription(label).performClick()
        composeRule.waitForIdle()
    }

    /** See [LanguageSwitchTest.launchIn]: the language is applied after the launch, never before. */
    private fun launchIn(language: AppLanguage): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        if (AppLanguages.current() != language) {
            setAppLanguage(language)
        }
        val context = if (language == AppLanguage.POLISH) polish else english
        awaitContentDescription(context.getString(R.string.open_menu))
        return scenario
    }

    /**
     * A timeout names what it was waiting for.
     *
     * These flows are long, and `ComposeTimeoutException` on its own says only that something did
     * not appear - which of a dozen steps it was is left as an exercise.
     */
    private fun awaitText(text: String) {
        await("text '$text'") {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitContentDescription(description: String) {
        await("description '$description'") {
            composeRule
                .onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun await(what: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(TIMEOUT_MILLIS, condition)
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError("Timed out waiting for $what", timeout)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L

        /** A name a user would actually type, and one no translation would ever touch. */
        const val RECORD_NAME = "OC Ford"
    }
}
