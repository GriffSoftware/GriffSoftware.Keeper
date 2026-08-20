package com.griff.subscriptions.presentation.home

import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.subscription.CalculateSubscriptionTotalsUseCase
import com.griff.subscriptions.application.subscription.SearchSubscriptionsUseCase
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.testing.FakeProviderCatalog
import com.griff.subscriptions.domain.testing.FakeSubscriptionRepository
import com.griff.subscriptions.domain.testing.testSubscription
import com.griff.subscriptions.presentation.util.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import org.junit.Rule

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSubscriptionRepository(
        listOf(
            testSubscription(id = "1", providerId = "spotify", name = "Spotify", priceMinorUnits = 3499),
            testSubscription(id = "2", providerId = "netflix", name = "Netflix", priceMinorUnits = 6700),
            testSubscription(
                id = "3",
                providerId = "seohost",
                name = "SeoHost.pl",
                priceMinorUnits = 59_900,
                billingPeriod = BillingPeriod.YEARLY,
            ),
        ),
    )

    private fun viewModel() = HomeViewModel(
        searchSubscriptions = SearchSubscriptionsUseCase(repository),
        calculateTotals = CalculateSubscriptionTotalsUseCase(),
        getProvider = GetProviderUseCase(FakeProviderCatalog()),
    )

    @Test
    fun `exposes subscriptions with normalized totals`() = runTest {
        val viewModel = viewModel()

        keepActive(viewModel.uiState)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Netflix", "SeoHost.pl", "Spotify"), state.items.map { it.name })
        // 34,99 + 67,00 + (599,00 / 12 = 49,92)
        assertEquals(15_191, state.totals.monthly.minorUnits)
        assertEquals(3, state.totalSubscriptionCount)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `filters while typing and recomputes totals for the results`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onQueryChange("spo")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Spotify"), state.items.map { it.name })
        assertEquals(3499, state.totals.monthly.minorUnits)
        assertTrue(state.isFiltered)
        assertFalse(state.hasNoResults)
    }

    @Test
    fun `reports an empty search result separately from an empty database`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onQueryChange("hbo")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasNoResults)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `an empty database yields the empty state`() = runTest {
        val viewModel = HomeViewModel(
            searchSubscriptions = SearchSubscriptionsUseCase(FakeSubscriptionRepository()),
            calculateTotals = CalculateSubscriptionTotalsUseCase(),
            getProvider = GetProviderUseCase(FakeProviderCatalog()),
        )
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(0, viewModel.uiState.value.totals.monthly.minorUnits)
    }

    @Test
    fun `custom entries are seeded by their name so monograms differ`() = runTest {
        val repository = FakeSubscriptionRepository(
            listOf(testSubscription(id = "1", providerId = "other", name = "Domena griff.pl")),
        )
        val viewModel = HomeViewModel(
            searchSubscriptions = SearchSubscriptionsUseCase(repository),
            calculateTotals = CalculateSubscriptionTotalsUseCase(),
            getProvider = GetProviderUseCase(FakeProviderCatalog()),
        )
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertEquals("Domena griff.pl", viewModel.uiState.value.items.single().logoKey)
    }
}

/** Keeps a `stateIn(WhileSubscribed)` flow active for the duration of a test. */
private fun <T> TestScope.keepActive(flow: Flow<T>) {
    flow.launchIn(backgroundScope)
}
