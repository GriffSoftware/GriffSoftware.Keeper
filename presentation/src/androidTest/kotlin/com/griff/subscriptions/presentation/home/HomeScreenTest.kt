package com.griff.subscriptions.presentation.home

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.SubscriptionTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val items = listOf(
        SubscriptionListItem(
            "1", "Spotify", "spotify", ProviderCategory.MUSIC,
            BillingPeriod.MONTHLY, Money.ofUnits(34, 99),
        ),
        SubscriptionListItem(
            "2", "JetBrains", "jetbrains", ProviderCategory.SOFTWARE,
            BillingPeriod.YEARLY, Money.ofUnits(1_299),
        ),
    )

    @Test
    fun showsEmptyStateWhenThereAreNoSubscriptions() {
        setContent(HomeUiState(isLoading = false))

        composeRule.onNodeWithText(context.getString(R.string.home_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_empty_description)).assertIsDisplayed()
    }

    @Test
    fun showsSubscriptionsWithNormalizedTotals() {
        setContent(
            HomeUiState(
                isLoading = false,
                items = items,
                totals = SubscriptionTotals(Money.ofUnits(143, 24), Money.ofUnits(1_718, 88), 2),
                totalSubscriptionCount = 2,
            ),
        )

        composeRule.onNodeWithText("Spotify").assertIsDisplayed()
        composeRule.onNodeWithText("JetBrains").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_totals_title)).assertIsDisplayed()
        composeRule.onNodeWithText("143,24 zł / mies.").assertIsDisplayed()
        composeRule.onNodeWithText("1\u00A0718,88 zł / rok").assertIsDisplayed()
    }

    @Test
    fun clickingASubscriptionEmitsItsId() {
        var clickedId: String? = null
        setContent(
            state = HomeUiState(isLoading = false, items = items, totalSubscriptionCount = 2),
            onSubscriptionClick = { clickedId = it },
        )

        composeRule.onNodeWithText("JetBrains").performClick()

        assertEquals("2", clickedId)
    }

    @Test
    fun typingInSearchEmitsTheQuery() {
        val queries = mutableListOf<String>()
        setContent(
            state = HomeUiState(isLoading = false, items = items, totalSubscriptionCount = 2),
            onQueryChange = { queries += it },
        )

        composeRule
            .onNodeWithText(context.getString(R.string.home_search_placeholder))
            .performTextInput("spo")

        assertEquals(listOf("spo"), queries)
    }

    @Test
    fun totalsStayVisibleWithoutScrollingOnALongList() {
        val many = List(40) { index ->
            SubscriptionListItem(
                id = index.toString(),
                name = "Usługa $index",
                logoKey = "service-$index",
                category = ProviderCategory.OTHER,
                billingPeriod = BillingPeriod.MONTHLY,
                price = Money.ofUnits(10),
            )
        }
        setContent(
            HomeUiState(
                isLoading = false,
                items = many,
                totals = SubscriptionTotals(Money.ofUnits(400), Money.ofUnits(4_800), many.size),
                totalSubscriptionCount = many.size,
            ),
        )

        // The summary is a bottom bar, so it is on screen even though most rows are not.
        composeRule.onNodeWithText("400,00 zł / mies.").assertIsDisplayed()
        composeRule.onNodeWithText("4\u00A0800,00 zł / rok").assertIsDisplayed()
    }

    @Test
    fun showsNoResultsStateForAQueryWithoutMatches() {
        setContent(
            HomeUiState(
                isLoading = false,
                query = "hbo",
                items = emptyList(),
                totalSubscriptionCount = 2,
            ),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.no_results_title))
            .assertIsDisplayed()
        // Nothing to sum up, so the totals bar is gone.
        composeRule.onNodeWithText(context.getString(R.string.home_totals_title)).assertDoesNotExist()
    }

    @Test
    fun showsTheCategoryTagOnEveryRow() {
        setContent(HomeUiState(isLoading = false, items = items, totalSubscriptionCount = 2))

        composeRule.onNodeWithText(context.getString(R.string.category_music)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.category_software)).assertIsDisplayed()
    }

    @Test
    fun selectingATagFilterEmitsItsCategory() {
        val selected = mutableListOf<ProviderCategory?>()
        setContent(
            state = HomeUiState(
                isLoading = false,
                items = items,
                totalSubscriptionCount = 2,
                availableCategories = listOf(ProviderCategory.MUSIC, ProviderCategory.SOFTWARE),
            ),
            onCategoryChange = { selected += it },
        )

        // The list rows carry the same labels on their tags, and the filter row comes first.
        composeRule
            .onAllNodesWithText(context.getString(R.string.category_software))
            .onFirst()
            .performClick()

        assertEquals(listOf<ProviderCategory?>(ProviderCategory.SOFTWARE), selected)
    }

    private fun setContent(
        state: HomeUiState,
        onQueryChange: (String) -> Unit = {},
        onCategoryChange: (ProviderCategory?) -> Unit = {},
        onSubscriptionClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            GriffSubscriptionsTheme(dynamicColor = false) {
                HomeScreen(
                    state = state,
                    onQueryChange = onQueryChange,
                    onCategoryChange = onCategoryChange,
                    onOpenDrawer = {},
                    onSubscriptionClick = onSubscriptionClick,
                    onAddSubscription = {},
                )
            }
        }
    }
}
