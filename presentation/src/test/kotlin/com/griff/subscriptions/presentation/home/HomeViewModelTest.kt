package com.griff.subscriptions.presentation.home

import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.subscription.CalculateSubscriptionTotalsUseCase
import com.griff.subscriptions.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.subscriptions.application.subscription.SearchSubscriptionsUseCase
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.testing.FakeProviderCatalog
import com.griff.subscriptions.domain.testing.FakeSubscriptionRepository
import com.griff.subscriptions.domain.testing.testSubscription
import com.griff.subscriptions.presentation.util.MainDispatcherRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalog = FakeProviderCatalog()

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
            testSubscription(
                id = "4",
                providerId = "other",
                name = "Apple Music",
                categoryOverride = ProviderCategory.MUSIC,
                priceMinorUnits = 2499,
            ),
        ),
    )

    private fun viewModel(source: SubscriptionRepository = repository) = HomeViewModel(
        searchSubscriptions = SearchSubscriptionsUseCase(
            repository = source,
            getCategory = GetSubscriptionCategoryUseCase(catalog),
        ),
        calculateTotals = CalculateSubscriptionTotalsUseCase(),
        getProvider = GetProviderUseCase(catalog),
        getCategory = GetSubscriptionCategoryUseCase(catalog),
    )

    @Test
    fun `exposes subscriptions with normalized totals`() = runTest {
        val viewModel = viewModel()

        keepActive(viewModel.uiState)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(
            listOf("Apple Music", "Netflix", "SeoHost.pl", "Spotify"),
            state.items.map { it.name },
        )
        // 24,99 + 34,99 + 67,00 + (599,00 / 12 = 49,92)
        assertEquals(17_690, state.totals.monthly.minorUnits)
        assertEquals(4, state.totalSubscriptionCount)
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
        val viewModel = viewModel(FakeSubscriptionRepository())
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(0, viewModel.uiState.value.totals.monthly.minorUnits)
    }

    @Test
    fun `custom entries are seeded by their name so monograms differ`() = runTest {
        val viewModel = viewModel(
            FakeSubscriptionRepository(
                listOf(testSubscription(id = "1", providerId = "other", name = "Domena griff.pl")),
            ),
        )
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertEquals("Domena griff.pl", viewModel.uiState.value.items.single().logoKey)
    }

    @Test
    fun `every row carries the category its tag is drawn from`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        val categories = viewModel.uiState.value.items.associate { it.name to it.category }
        assertEquals(ProviderCategory.MUSIC, categories["Spotify"])
        assertEquals(ProviderCategory.VIDEO, categories["Netflix"])
        // A custom entry keeps the category the user picked for it.
        assertEquals(ProviderCategory.MUSIC, categories["Apple Music"])
    }

    @Test
    fun `filtering by tag keeps only that category`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onCategoryChange(ProviderCategory.MUSIC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Apple Music", "Spotify"), state.items.map { it.name })
        assertFalse(state.items.any { it.name == "Netflix" })
        assertTrue(state.isFiltered)
    }

    @Test
    fun `search and tag filter apply at the same time`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        viewModel.onCategoryChange(ProviderCategory.MUSIC)
        viewModel.onQueryChange("apple")
        advanceUntilIdle()

        assertEquals(listOf("Apple Music"), viewModel.uiState.value.items.map { it.name })

        viewModel.onQueryChange("netflix")
        advanceUntilIdle()

        // Netflix matches the text but not the tag, so nothing is shown.
        assertTrue(viewModel.uiState.value.hasNoResults)
    }

    @Test
    fun `only categories present in the data are offered as filters`() = runTest {
        val viewModel = viewModel()
        keepActive(viewModel.uiState)
        advanceUntilIdle()

        assertEquals(
            listOf(ProviderCategory.VIDEO, ProviderCategory.MUSIC, ProviderCategory.HOSTING),
            viewModel.uiState.value.availableCategories,
        )
    }
}

/** Keeps a `stateIn(WhileSubscribed)` flow active for the duration of a test. */
private fun <T> TestScope.keepActive(flow: Flow<T>) {
    flow.launchIn(backgroundScope)
}
