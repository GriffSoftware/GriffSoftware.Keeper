package com.griff.keeper.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.keeper.BuildConfig
import com.griff.keeper.presentation.R
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Smoke test of the composition root: it starts the real activity with the real Hilt graph, so a
 * broken binding or navigation setup fails here instead of on a user's device.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun startsOnTheSubscriptionsScreen() {
        // The title alone is ambiguous: the drawer uses the same label for this destination.
        composeRule
            .onNodeWithText(context.getString(R.string.subscriptions_search_placeholder))
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.subscriptions_add))
            .assertIsDisplayed()
    }

    @Test
    fun drawerOpensAndShowsBothDestinations() {
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.open_menu))
            .performClick()

        composeRule.onNodeWithText(context.getString(R.string.drawer_statistics)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.drawer_about)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.app_tagline)).assertIsDisplayed()
    }

    @Test
    fun drawerOpensTheAboutScreen() {
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.open_menu))
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.drawer_about)).performClick()

        // The features heading rather than the title: the title is also the drawer label.
        composeRule
            .onNodeWithText(context.getString(R.string.about_features_title))
            .assertIsDisplayed()
        // The version comes from the real BuildConfig of the build under test, and it sits at the
        // end of a screen that is taller than the device.
        val version = context.getString(
            R.string.about_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toLong(),
        )
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(version))
        composeRule.onNodeWithText(version).assertIsDisplayed()
    }
}
