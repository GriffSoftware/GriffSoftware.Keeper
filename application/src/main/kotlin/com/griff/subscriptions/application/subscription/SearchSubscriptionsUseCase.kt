package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.search.SubscriptionFilter
import com.griff.subscriptions.domain.search.applyFilter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Result of a subscription search: what matched and how big the whole collection is. */
data class SubscriptionSearchResult(
    val filter: SubscriptionFilter,
    val matching: List<Subscription>,
    val totalCount: Int,
    /** Categories present in the whole collection, so the chip row only offers useful filters. */
    val availableCategories: List<ProviderCategory>,
) {
    val isNarrowed: Boolean get() = filter.isNarrowed
}

/**
 * Streams subscriptions matching a stream of filters.
 *
 * Both the filter and the stored data are observed, so a single database subscription serves typing,
 * category switching, inserts and deletes. Matching itself lives in the domain layer.
 */
class SearchSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val getCategory: GetSubscriptionCategoryUseCase,
) {
    operator fun invoke(filters: Flow<SubscriptionFilter>): Flow<SubscriptionSearchResult> =
        combine(repository.observeAll(), filters) { subscriptions, filter ->
            val categories: Map<Subscription, ProviderCategory> =
                subscriptions.associateWith(getCategory::invoke)
            SubscriptionSearchResult(
                filter = filter,
                matching = subscriptions.applyFilter(filter) { categories.getValue(it) },
                totalCount = subscriptions.size,
                availableCategories = ProviderCategory.entries.filter { category ->
                    categories.containsValue(category)
                },
            )
        }
}
