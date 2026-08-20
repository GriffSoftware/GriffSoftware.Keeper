package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.domain.calculation.ObligationCostCalculator
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.ObligationTotals
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.search.ObligationFilter
import com.griff.subscriptions.domain.search.applyFilter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * What the obligations screen shows: the records that survived the filter, the totals for them and
 * enough context to tell "nothing stored yet" apart from "nothing matches these filters".
 */
data class ObligationSearchResult(
    val filter: ObligationFilter,
    val matching: List<Obligation>,
    val totals: ObligationTotals,
    val totalCount: Int,
    /** Tags present in the whole collection, so the chip row only offers filters that can match. */
    val availableTags: List<ObligationTag>,
) {
    val isNarrowed: Boolean get() = filter.isNarrowed
}

/**
 * Streams obligations narrowed down by a stream of filters.
 *
 * Both the filter and the stored data are observed, so one database subscription serves typing,
 * period switching, inserts and deletes. Filtering and summing themselves live in the domain layer.
 */
class SearchObligationsUseCase @Inject constructor(
    private val repository: ObligationRepository,
) {
    operator fun invoke(filters: Flow<ObligationFilter>): Flow<ObligationSearchResult> =
        combine(repository.observeAll(), filters) { obligations, filter ->
            val matching = obligations.applyFilter(filter)
            ObligationSearchResult(
                filter = filter,
                matching = matching,
                // The summary describes what the list currently shows, the way the subscription
                // screen does; the screen says so in its label when a filter is active.
                totals = ObligationCostCalculator.totals(matching, filter.period),
                totalCount = obligations.size,
                availableTags = ObligationTag.entries.filter { tag ->
                    obligations.any { it.tag == tag }
                },
            )
        }
}
