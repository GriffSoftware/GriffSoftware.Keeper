package com.griff.subscriptions.domain.search

import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationTag

/**
 * Everything the obligations list is narrowed down by.
 *
 * One tag at a time is enough for the current UX; [tag] is a single nullable value rather than a
 * flag per tag, so widening it to a set later does not change any call site's shape.
 */
data class ObligationFilter(
    val period: ExpensePeriod,
    val query: String = "",
    val tag: ObligationTag? = null,
) {
    val isNarrowed: Boolean get() = query.isNotBlank() || tag != null
}

/** Applies a filter and returns the records in display order. */
fun List<Obligation>.applyFilter(filter: ObligationFilter): List<Obligation> =
    asSequence()
        .filter { it.periodDate?.let(filter.period::contains) ?: true }
        .filter { filter.tag == null || it.tag == filter.tag }
        .filter { NameMatcher.matches(it, filter.query) }
        .toList()
        .sortedByDeadline()

/**
 * Records with a deadline first, soonest (including overdue) at the top, everything else after.
 *
 * The screen exists to keep the user ahead of expiring policies and payment deadlines, so what is
 * closest in time is what belongs on top; records without any date sort by name so the order is
 * stable between launches.
 */
fun List<Obligation>.sortedByDeadline(): List<Obligation> =
    sortedWith(
        compareBy<Obligation> { it.deadline == null }
            .thenBy { it.deadline }
            .thenBy { it.name.value.lowercase() },
    )
