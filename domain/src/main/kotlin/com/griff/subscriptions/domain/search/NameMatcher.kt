package com.griff.subscriptions.domain.search

import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.Provider
import com.griff.subscriptions.domain.model.Subscription

/**
 * Case- and whitespace-insensitive "contains" matching used by every search field in the app.
 *
 * Lower-casing uses the root locale so results do not depend on the device language.
 */
object NameMatcher {

    fun matches(subscription: Subscription, query: String): Boolean =
        matches(subscription.name.value, query)

    fun matches(obligation: Obligation, query: String): Boolean =
        matches(obligation.name.value, query)

    fun matches(provider: Provider, query: String): Boolean =
        matches(provider.displayName, query)

    fun matches(text: String, query: String): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return true
        return normalize(text).contains(normalizedQuery)
    }

    private fun normalize(value: String): String = value.trim().lowercase()
}

@JvmName("filterSubscriptionsByQuery")
fun List<Subscription>.filterByQuery(query: String): List<Subscription> =
    filter { NameMatcher.matches(it, query) }

@JvmName("filterObligationsByQuery")
fun List<Obligation>.filterByQuery(query: String): List<Obligation> =
    filter { NameMatcher.matches(it, query) }

@JvmName("filterProvidersByQuery")
fun List<Provider>.filterByQuery(query: String): List<Provider> =
    filter { NameMatcher.matches(it, query) }
