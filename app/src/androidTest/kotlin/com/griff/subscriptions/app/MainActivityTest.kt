package com.griff.subscriptions.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.griff.subscriptions.presentation.R
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
        // The title alone is ambiguous: the drawer uses the same label for the home destination.
        composeRule
            .onNodeWithText(context.getString(R.string.home_search_placeholder))
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.home_add_subscription))
            .assertIsDisplayed()
    }

    @Test
    fun drawerOpensAndShowsBothDestinations() {
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.open_menu))
            .performClick()

        composeRule.onNodeWithText(context.getString(R.string.drawer_statistics)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.app_tagline)).assertIsDisplayed()
    }
}
