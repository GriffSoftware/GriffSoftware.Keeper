package com.griff.keeper.domain.search

import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.Subscription

/**
 * What the subscription list is narrowed down by: free text and at most one category.
 *
 * Mirrors [ObligationFilter] so both lists behave the same way; a single nullable [category] keeps
 * the UX simple while leaving room for a set later.
 */
data class SubscriptionFilter(
    val query: String = "",
    val category: ProviderCategory? = null,
) {
    val isNarrowed: Boolean get() = query.isNotBlank() || category != null
}

/**
 * Applies a filter, ordering is left untouched.
 *
 * The category of each subscription is resolved through [categoryOf], because a catalog entry's
 * category lives in the catalog rather than on the record.
 */
fun List<Subscription>.applyFilter(
    filter: SubscriptionFilter,
    categoryOf: (Subscription) -> ProviderCategory,
): List<Subscription> =
    filter { subscription ->
        (filter.category == null || categoryOf(subscription) == filter.category) &&
            NameMatcher.matches(subscription, filter.query)
    }
