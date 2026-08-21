package com.griff.subscriptions.presentation.form

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.griff.subscriptions.domain.validation.SubscriptionField
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class SubscriptionFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val catalogOption = ProviderOption("spotify", "Spotify", "spotify", isOther = false)
    private val otherOption = ProviderOption("other", "Other", "other", isOther = true)

    @Test
    fun saveIsDisabledUntilTheFormIsValid() {
        setContent(SubscriptionFormUiState(providerOptions = listOf(catalogOption, otherOption)))

        composeRule.onNodeWithText(context.getString(R.string.form_save)).assertIsNotEnabled()
    }

    @Test
    fun saveIsEnabledForAValidForm() {
        setContent(
            SubscriptionFormUiState(
                selectedProvider = catalogOption,
                name = "Spotify",
                price = "34,99",
                isSaveEnabled = true,
            ),
        )

        composeRule.onNodeWithText(context.getString(R.string.form_save)).assertIsEnabled()
    }

    @Test
    fun showsValidationErrorForAnInvalidPrice() {
        setContent(
            SubscriptionFormUiState(
                selectedProvider = catalogOption,
                price = "34,555",
                fieldErrors = mapOf(SubscriptionField.PRICE to R.string.form_error_price_decimals),
            ),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.form_error_price_decimals))
            .assertIsDisplayed()
    }

    @Test
    fun nameFieldIsHiddenForCatalogServices() {
        setContent(SubscriptionFormUiState(selectedProvider = catalogOption))

        composeRule.onNodeWithText(context.getString(R.string.form_name_label)).assertDoesNotExist()
    }

    @Test
    fun nameFieldIsShownForTheOtherOption() {
        setContent(SubscriptionFormUiState(selectedProvider = otherOption))

        composeRule.onNodeWithText(context.getString(R.string.form_name_label)).assertIsDisplayed()
    }

    @Test
    fun selectingAProviderEmitsTheOption() {
        var selected: ProviderOption? = null
        setContent(
            state = SubscriptionFormUiState(providerOptions = listOf(catalogOption, otherOption)),
            onProviderSelected = { selected = it },
        )

        composeRule.onNodeWithText("Spotify").performClick()

        assertEquals(catalogOption, selected)
    }

    private fun setContent(
        state: SubscriptionFormUiState,
        onProviderSelected: (ProviderOption) -> Unit = {},
    ) {
        composeRule.setContent {
            GriffSubscriptionsTheme(dynamicColor = false) {
                SubscriptionFormScreen(
                    state = state,
                    onNavigateUp = {},
                    onProviderQueryChange = {},
                    onProviderSelected = onProviderSelected,
                    onProviderCleared = {},
                        onNameChange = {},
                    onCategoryChange = {},
                    onPriceChange = {},
                    onBillingPeriodChange = {},
                    onNextBillingDateChange = {},
                    onManagementUrlChange = {},
                    onRemindersEnabledChange = {},
                    onSave = {},
                    onMessageShown = {},
                )
            }
        }
    }
}
