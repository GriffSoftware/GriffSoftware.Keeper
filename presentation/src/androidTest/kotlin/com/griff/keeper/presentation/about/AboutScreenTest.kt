package com.griff.keeper.presentation.about

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.griff.keeper.application.appinfo.AppVersion
import com.griff.keeper.presentation.R
import com.griff.keeper.presentation.common.MessageSeverity
import com.griff.keeper.presentation.common.UiMessage
import com.griff.keeper.presentation.theme.GriffKeeperTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

/**
 * The About screen is static text, so what is worth testing is that it says what it promises and
 * that its two interactions reach the caller: the platform work (the mail intent and the clipboard)
 * lives above the screen and is deliberately not driven from here.
 *
 * The content is taller than a phone, which is the point of the screen being a list; the assertions
 * scroll to what they are looking for instead of assuming it starts on screen.
 */
class AboutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val version = AppVersion(name = "1.2.0", code = 15L)

    @Test
    fun explainsWhatTheAppIsAndWhatItCanDo() {
        setContent()

        // The name is on the screen twice: as the heading and in the footer above the version.
        composeRule
            .onAllNodesWithText(context.getString(R.string.app_display_name))
            .onFirst()
            .assertIsDisplayed()
        assertShown(context.getString(R.string.about_intro))
        assertShown(context.getString(R.string.about_features_title))
        assertShown(context.getString(R.string.about_feature_reminders))
    }

    @Test
    fun statesThatTheDataStaysOnTheDevice() {
        setContent()

        assertShown(context.getString(R.string.about_privacy_title))
        assertShown(context.getString(R.string.about_privacy_description))
    }

    @Test
    fun showsTheSupportAddress() {
        setContent()

        assertShown(context.getString(R.string.about_contact_title))
        assertShown(context.getString(R.string.about_contact_email))
    }

    @Test
    fun showsTheVersionItWasGiven() {
        setContent()

        assertShown(context.getString(R.string.about_version, "1.2.0", 15L))
    }

    @Test
    fun clickingTheAddressAsksForAMailApp() {
        var emailClicks = 0
        setContent(onEmailClick = { emailClicks++ })

        val address = context.getString(R.string.about_contact_email)
        scrollTo(hasText(address))
        composeRule.onNodeWithText(address).performClick()

        assertEquals(1, emailClicks)
    }

    @Test
    fun clickingCopyAsksForTheAddressToBeCopied() {
        var copyClicks = 0
        setContent(onCopyEmail = { copyClicks++ })

        val label = context.getString(R.string.about_contact_email_copy)
        scrollTo(hasContentDescription(label))
        composeRule.onNodeWithContentDescription(label).performClick()

        assertEquals(1, copyClicks)
    }

    /**
     * The message is released only once the snackbar has run its course, which is what
     * `showMessage` suspends for, so the assertion is about what the user sees.
     */
    @Test
    fun confirmsThatTheAddressWasCopied() {
        setContent(
            message = UiMessage(
                textRes = R.string.about_contact_email_copied,
                severity = MessageSeverity.SUCCESS,
            ),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.about_contact_email_copied))
            .assertIsDisplayed()
    }

    private fun assertShown(text: String) {
        scrollTo(hasText(text))
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
    }

    private fun setContent(
        message: UiMessage? = null,
        onEmailClick: () -> Unit = {},
        onCopyEmail: () -> Unit = {},
        onMessageShown: () -> Unit = {},
    ) {
        composeRule.setContent {
            GriffKeeperTheme(dynamicColor = false) {
                AboutScreen(
                    appVersion = version,
                    message = message,
                    onOpenDrawer = {},
                    onEmailClick = onEmailClick,
                    onCopyEmail = onCopyEmail,
                    onMessageShown = onMessageShown,
                )
            }
        }
    }
}
