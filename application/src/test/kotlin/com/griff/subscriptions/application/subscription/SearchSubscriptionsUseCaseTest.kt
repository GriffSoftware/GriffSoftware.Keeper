package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.testing.FakeSubscriptionRepository
import com.griff.subscriptions.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SearchSubscriptionsUseCaseTest {

    private val repository = FakeSubscriptionRepository(
        listOf(
            testSubscription(id = "1", name = "Spotify"),
            testSubscription(id = "2", name = "Netflix"),
            testSubscription(id = "3", name = "Google Workspace"),
        ),
    )

    private val searchSubscriptions = SearchSubscriptionsUseCase(repository)

    @Test
    fun `empty query returns everything`() = runTest {
        val result = searchSubscriptions(MutableStateFlow("")).first()

        assertEquals(3, result.matching.size)
        assertEquals(3, result.totalCount)
        assertEquals(false, result.isFiltered)
    }

    @Test
    fun `matches are case insensitive`() = runTest {
        val result = searchSubscriptions(MutableStateFlow("SPO")).first()

        assertEquals(listOf("Spotify"), result.matching.map { it.name.value })
        assertEquals(3, result.totalCount)
        assertEquals(true, result.isFiltered)
    }

    @Test
    fun `reacts to query changes`() = runTest {
        val queries = MutableStateFlow("net")

        assertEquals(listOf("Netflix"), searchSubscriptions(queries).first().matching.map { it.name.value })

        queries.value = "google"

        assertEquals(
            listOf("Google Workspace"),
            searchSubscriptions(queries).first().matching.map { it.name.value },
        )
    }
}
