package com.griff.keeper.domain.search

import com.griff.keeper.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NameMatcherTest {

    private val subscriptions = listOf(
        testSubscription(id = "1", name = "Spotify"),
        testSubscription(id = "2", name = "Netflix"),
        testSubscription(id = "3", name = "Google Workspace"),
    )

    @Test
    fun `matches a case insensitive fragment`() {
        assertEquals(listOf("Spotify"), subscriptions.filterByQuery("spo").map { it.name.value })
        assertEquals(listOf("Spotify"), subscriptions.filterByQuery("SPOTIFY").map { it.name.value })
        assertEquals(listOf("Google Workspace"), subscriptions.filterByQuery("work").map { it.name.value })
    }

    @Test
    fun `trims the query`() {
        assertEquals(listOf("Netflix"), subscriptions.filterByQuery("  net ").map { it.name.value })
    }

    @Test
    fun `blank query matches everything`() {
        assertEquals(3, subscriptions.filterByQuery("   ").size)
        assertEquals(3, subscriptions.filterByQuery("").size)
    }

    @Test
    fun `unknown fragment matches nothing`() {
        assertTrue(subscriptions.filterByQuery("hbo").isEmpty())
        assertFalse(NameMatcher.matches("Spotify", "tidal"))
    }
}
