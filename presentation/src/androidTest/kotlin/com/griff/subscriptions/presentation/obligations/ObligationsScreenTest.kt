package com.griff.subscriptions.presentation.obligations

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.ObligationTotals
import com.griff.subscriptions.presentation.R
import com.griff.subscriptions.presentation.theme.GriffSubscriptionsTheme
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class ObligationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today = LocalDate.of(2026, 8, 21)

    private val items = listOf(
        ObligationListItem(
            id = "1",
            name = "OC Ford",
            category = ObligationCategory.VEHICLE_INSURANCE,
            amount = Money.ofUnits(1_240),
            isPaid = true,
            paymentDate = LocalDate.of(2026, 3, 12),
            dueDate = null,
            validUntil = LocalDate.of(2027, 3, 11),
            deadline = DeadlineStatus(DeadlineUrgency.NORMAL),
        ),
        ObligationListItem(
            id = "2",
            name = "Podatek od gruntu",
            category = ObligationCategory.LAND_TAX,
            amount = Money.ofUnits(320),
            isPaid = false,
            paymentDate = null,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
            deadline = DeadlineStatus(
                urgency = DeadlineUrgency.SOON,
                daysPluralRes = R.plurals.deadline_due_in,
                days = 5,
            ),
        ),
    )

    private val totals = ObligationTotals(
        paid = Money.ofUnits(4_820),
        outstanding = Money.ofUnits(320),
        paidCount = 4,
        outstandingCount = 1,
        largestPaid = Money.ofUnits(1_420),
    )

    @Test
    fun showsEmptyStateWhenNothingIsStored() {
        setContent(
            ObligationsUiState(
                period = ExpensePeriod.Year(2026),
                today = today,
                isLoading = false,
            ),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.obligations_empty_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.obligations_empty_description))
            .assertIsDisplayed()
    }

    @Test
    fun showsRecordsWithTheirTagsAndTheYearlyTotal() {
        setContent(state(items = items))

        composeRule.onNodeWithText("OC Ford").assertIsDisplayed()
        composeRule.onNodeWithText("Podatek od gruntu").assertIsDisplayed()
        composeRule
            .onAllNodesWithText(context.getString(R.string.tag_vehicle_insurance))
            .onFirst()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.obligations_paid_in_year, 2026))
            .assertIsDisplayed()
        composeRule.onNodeWithText("4 820,00 zł").assertIsDisplayed()
    }

    @Test
    fun marksAnApproachingDeadlineWithWords() {
        setContent(state(items = items))

        composeRule
            .onNodeWithText(context.resources.getQuantityString(R.plurals.deadline_due_in, 5, 5))
            .assertIsDisplayed()
    }

    @Test
    fun clickingARecordEmitsItsId() {
        var clickedId: String? = null
        setContent(state(items = items), onObligationClick = { clickedId = it })

        composeRule.onNodeWithText("Podatek od gruntu").performClick()

        assertEquals("2", clickedId)
    }

    @Test
    fun typingInSearchEmitsTheQuery() {
        val queries = mutableListOf<String>()
        setContent(state(items = items), onQueryChange = { queries += it })

        composeRule
            .onNodeWithText(context.getString(R.string.obligations_search_placeholder))
            .performTextInput("ford")

        assertEquals(listOf("ford"), queries)
    }

    @Test
    fun steppingThePeriodEmitsTheNeighbouringYear() {
        val periods = mutableListOf<ExpensePeriod>()
        setContent(state(items = items), onPeriodChange = { periods += it })

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.period_next))
            .performClick()

        assertEquals(listOf<ExpensePeriod>(ExpensePeriod.Year(2027)), periods)
    }

    @Test
    fun showsFilterAwareEmptyStateWhenNothingMatches() {
        setContent(
            state(items = emptyList(), query = "hbo").copy(totalCount = 5),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.no_results_title))
            .assertIsDisplayed()
        // Not "add your first record": there are records, they are simply filtered out.
        composeRule
            .onNodeWithText(context.getString(R.string.obligations_empty_title))
            .assertDoesNotExist()
    }

    private fun state(
        items: List<ObligationListItem>,
        query: String = "",
    ) = ObligationsUiState(
        period = ExpensePeriod.Year(2026),
        today = today,
        isLoading = false,
        query = query,
        availableTags = listOf(ObligationTag.VEHICLE, ObligationTag.TAX),
        items = items,
        totals = totals,
        totalCount = items.size.coerceAtLeast(1),
    )

    private fun setContent(
        state: ObligationsUiState,
        onQueryChange: (String) -> Unit = {},
        onTagChange: (ObligationTag?) -> Unit = {},
        onPeriodChange: (ExpensePeriod) -> Unit = {},
        onObligationClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            GriffSubscriptionsTheme(dynamicColor = false) {
                ObligationsScreen(
                    state = state,
                    onQueryChange = onQueryChange,
                    onTagChange = onTagChange,
                    onPeriodChange = onPeriodChange,
                    onOpenDrawer = {},
                    onObligationClick = onObligationClick,
                    onAddObligation = {},
                )
            }
        }
    }
}
