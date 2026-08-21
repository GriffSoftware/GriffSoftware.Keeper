package com.griff.keeper.application.subscription

import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.search.SubscriptionFilter
import com.griff.keeper.domain.testing.FakeProviderCatalog
import com.griff.keeper.domain.testing.FakeSubscriptionRepository
import com.griff.keeper.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SearchSubscriptionsUseCaseTest {

    private val repository = FakeSubscriptionRepository(
        listOf(
            testSubscription(id = "1", providerId = "spotify", name = "Spotify"),
            testSubscription(id = "2", providerId = "netflix", name = "Netflix"),
            testSubscription(id = "3", providerId = "seohost", name = "SeoHost.pl"),
            testSubscription(
                id = "4",
                providerId = "other",
                name = "Apple Music",
                categoryOverride = ProviderCategory.MUSIC,
            ),
        ),
    )

    private val useCase = SearchSubscriptionsUseCase(
        repository = repository,
        getCategory = GetSubscriptionCategoryUseCase(FakeProviderCatalog()),
    )

    @Test
    fun `an empty query returns everything`() = runTest {
        val result = useCase(MutableStateFlow(SubscriptionFilter())).first()

        assertEquals(4, result.matching.size)
        assertEquals(4, result.totalCount)
        assertFalse(result.isNarrowed)
    }

    @Test
    fun `filters by a case insensitive fragment`() = runTest {
        val result = useCase(MutableStateFlow(SubscriptionFilter(query = "NET"))).first()

        assertEquals(listOf("Netflix"), result.matching.map { it.name.value })
        assertEquals(4, result.totalCount)
        assertTrue(result.isNarrowed)
    }

    @Test
    fun `reacts to a new query on the same stream`() = runTest {
        val filters = MutableStateFlow(SubscriptionFilter(query = "spo"))

        assertEquals(listOf("Spotify"), useCase(filters).first().matching.map { it.name.value })

        filters.value = SubscriptionFilter(query = "seo")
        assertEquals(listOf("SeoHost.pl"), useCase(filters).first().matching.map { it.name.value })
    }

    @Test
    fun `filters by category, using the catalog for known services`() = runTest {
        val filters = MutableStateFlow(SubscriptionFilter(category = ProviderCategory.MUSIC))

        // Spotify is MUSIC in the catalog; "Apple Music" is a custom entry the user categorized.
        assertEquals(
            listOf("Apple Music", "Spotify"),
            useCase(filters).first().matching.map { it.name.value }.sorted(),
        )
    }

    @Test
    fun `a category filter excludes other categories`() = runTest {
        val result = useCase(
            MutableStateFlow(SubscriptionFilter(category = ProviderCategory.MUSIC)),
        ).first()

        assertFalse(result.matching.any { it.name.value == "Netflix" })
    }

    @Test
    fun `query and category apply together`() = runTest {
        val result = useCase(
            MutableStateFlow(
                SubscriptionFilter(query = "apple", category = ProviderCategory.MUSIC),
            ),
        ).first()

        assertEquals(listOf("Apple Music"), result.matching.map { it.name.value })

        val mismatched = useCase(
            MutableStateFlow(
                SubscriptionFilter(query = "netflix", category = ProviderCategory.MUSIC),
            ),
        ).first()

        assertTrue(mismatched.matching.isEmpty())
        assertEquals(4, mismatched.totalCount)
    }

    @Test
    fun `offers only the categories that are actually present`() = runTest {
        val result = useCase(MutableStateFlow(SubscriptionFilter())).first()

        assertEquals(
            listOf(ProviderCategory.VIDEO, ProviderCategory.MUSIC, ProviderCategory.HOSTING),
            result.availableCategories,
        )
    }
}
