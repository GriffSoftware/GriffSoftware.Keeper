package com.griff.subscriptions.presentation.obligations

import com.griff.subscriptions.application.obligation.SearchObligationsUseCase
import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.testing.FakeObligationRepository
import com.griff.subscriptions.domain.testing.FixedClockProvider
import com.griff.subscriptions.domain.testing.testObligation
import com.griff.subscriptions.presentation.util.MainDispatcherRule
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/** The clock is frozen at 2026-08-20, see [FixedClockProvider]. */
class ObligationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeObligationRepository(
        listOf(
            testObligation(
                id = "1",
                name = "OC Ford",
                amountMinorUnits = 124_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
                validUntil = LocalDate.of(2027, 3, 11),
            ),
            testObligation(
                id = "2",
                name = "OC ZS775VG",
                amountMinorUnits = 98_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 5, 20)),
                validUntil = LocalDate.of(2027, 5, 19),
            ),
            testObligation(
                id = "3",
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                amountMinorUnits = 32_000,
                payment = PaymentState.Unpaid,
                dueDate = LocalDate.of(2026, 8, 28),
                validUntil = null,
            ),
            testObligation(
                id = "4",
                name = "Ubezpieczenie domu",
                category = ObligationCategory.HOME_INSURANCE,
                amountMinorUnits = 78_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 6, 3)),
                validUntil = LocalDate.of(2027, 6, 2),
            ),
        ),
    )

    private fun viewModel(source: ObligationRepository = repository) = ObligationsViewModel(
        searchObligations = SearchObligationsUseCase(source),
        clock = FixedClockProvider(),
    )

    @Test
    fun `opens on the current year with the paid total for it`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ExpensePeriod.Year(2026), state.period)
        // 1 240 + 980 + 780; the unpaid tax is reported separately.
        assertEquals(300_000, state.totals.paid.minorUnits)
        assertEquals(32_000, state.totals.outstanding.minorUnits)
        assertEquals(4, state.totalCount)
    }

    @Test
    fun `exposes today so the period selector can offer the current month`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 8, 20), viewModel.uiState.value.today)
    }

    @Test
    fun `an empty database yields the empty state`() = runTest {
        val viewModel = viewModel(FakeObligationRepository())
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.hasNoResults)
    }

    @Test
    fun `searching finds a record by a fragment of its name`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onQueryChange("ford")
        advanceUntilIdle()
        assertEquals(listOf("OC Ford"), viewModel.uiState.value.items.map { it.name })

        viewModel.onQueryChange("ZS775")
        advanceUntilIdle()
        assertEquals(listOf("OC ZS775VG"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `filtering by tag keeps only that tag`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onTagChange(ObligationTag.VEHICLE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("OC Ford", "OC ZS775VG"), state.items.map { it.name })
        assertFalse(state.items.any { it.name == "Podatek od gruntu" })
        assertTrue(state.isNarrowed)
    }

    @Test
    fun `search and tag filter apply at the same time`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onTagChange(ObligationTag.VEHICLE)
        viewModel.onQueryChange("ford")
        advanceUntilIdle()
        assertEquals(listOf("OC Ford"), viewModel.uiState.value.items.map { it.name })

        viewModel.onTagChange(ObligationTag.TAX)
        advanceUntilIdle()
        // "ford" still matches a name, but not inside the tax tag.
        assertTrue(viewModel.uiState.value.hasNoResults)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `switching the period re-filters and recomputes the summary`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onPeriodChange(ExpensePeriod.Month(YearMonth.of(2026, 3)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("OC Ford"), state.items.map { it.name })
        assertEquals(124_000, state.totals.paid.minorUnits)
    }

    @Test
    fun `a year the records were not paid in shows no results, not an empty database`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onPeriodChange(ExpensePeriod.Year(2031))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `rows carry the single date the record is about`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items.associateBy { it.name }
        val ford = items.getValue("OC Ford")
        val tax = items.getValue("Podatek od gruntu")

        assertTrue(ford.isPaid)
        assertEquals(LocalDate.of(2026, 3, 12), ford.paymentDate)
        assertFalse(tax.isPaid)
        assertNull(tax.paymentDate)
        assertEquals(LocalDate.of(2026, 8, 28), tax.dueDate)
    }

    @Test
    fun `a deadline inside two weeks is flagged as approaching`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items.associateBy { it.name }

        // The tax is due on 2026-08-28, eight days after the frozen "today".
        assertEquals(DeadlineUrgency.SOON, items.getValue("Podatek od gruntu").deadline?.urgency)
        assertEquals(8, items.getValue("Podatek od gruntu").deadline?.days)
        // The policies expire in 2027, which is ordinary information.
        assertEquals(DeadlineUrgency.NORMAL, items.getValue("OC Ford").deadline?.urgency)
    }

    @Test
    fun `an expired policy is flagged as overdue`() = runTest {
        val viewModel = viewModel(
            FakeObligationRepository(
                listOf(
                    testObligation(
                        id = "1",
                        name = "Ubezpieczenie drona",
                        category = ObligationCategory.DRONE_INSURANCE,
                        payment = PaymentState.Paid(LocalDate.of(2025, 8, 2)),
                        validUntil = LocalDate.of(2026, 8, 1),
                    ),
                ),
            ),
        )
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        // The record was paid in 2025, so the default 2026 window shows it only via its expiry...
        viewModel.onPeriodChange(ExpensePeriod.Year(2025))
        advanceUntilIdle()

        val item = viewModel.uiState.value.items.single()
        assertEquals(DeadlineUrgency.OVERDUE, item.deadline?.urgency)
    }

    @Test
    fun `the nearest deadline comes first`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(
            listOf("Podatek od gruntu", "OC Ford", "OC ZS775VG", "Ubezpieczenie domu"),
            viewModel.uiState.value.items.map { it.name },
        )
    }

    @Test
    fun `only tags present in the data are offered as filters`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(
            listOf(ObligationTag.VEHICLE, ObligationTag.HOME, ObligationTag.TAX),
            viewModel.uiState.value.availableTags,
        )
    }
}

/** Keeps a `stateIn(WhileSubscribed)` flow active for the duration of a test. */
private fun <T> TestScope.keepActive(flow: Flow<T>) {
    flow.launchIn(backgroundScope)
}
