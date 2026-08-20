package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.search.filterByQuery
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Result of a subscription search: what matched and how big the whole collection is. */
data class SubscriptionSearchResult(
    val query: String,
    val matching: List<Subscription>,
    val totalCount: Int,
) {
    val isFiltered: Boolean get() = query.isNotBlank()
}

/**
 * Streams subscriptions matching a stream of queries.
 *
 * Both the query and the stored data are observed, so a single database subscription serves typing,
 * inserts and deletes. Matching itself lives in the domain layer.
 */
class SearchSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(queries: Flow<String>): Flow<SubscriptionSearchResult> =
        combine(repository.observeAll(), queries) { subscriptions, query ->
            SubscriptionSearchResult(
                query = query,
                matching = subscriptions.filterByQuery(query),
                totalCount = subscriptions.size,
            )
        }
}
