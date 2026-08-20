package com.griff.subscriptions.presentation.obligations

import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.ObligationTotals
import com.griff.subscriptions.presentation.common.UiMessage
import java.time.LocalDate

/**
 * One row of the obligations list.
 *
 * The row shows a single date, the one the record is actually about: when it was paid, when the cover
 * ends, or when it is due. Putting all three on a row would be noise, and the details screen is one
 * tap away.
 */
data class ObligationListItem(
    val id: String,
    val name: String,
    val category: ObligationCategory,
    val amount: Money,
    val isPaid: Boolean,
    val paymentDate: LocalDate?,
    val dueDate: LocalDate?,
    val validUntil: LocalDate?,
    val deadline: DeadlineStatus?,
)

/**
 * Immutable state rendered by the obligations screen.
 *
 * [period] has no default: which window the screen opens on depends on the current date, which is
 * the clock's answer to give, not a constant's.
 */
data class ObligationsUiState(
    val period: ExpensePeriod,
    /** Today, so the period selector can tell "this month" from "the first month of the year". */
    val today: LocalDate,
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedTag: ObligationTag? = null,
    val availableTags: List<ObligationTag> = emptyList(),
    val items: List<ObligationListItem> = emptyList(),
    val totals: ObligationTotals = ObligationTotals.Empty,
    val totalCount: Int = 0,
    val message: UiMessage? = null,
) {
    val isNarrowed: Boolean get() = query.isNotBlank() || selectedTag != null

    /** Nothing stored at all - as opposed to filters that matched nothing. */
    val isEmpty: Boolean get() = !isLoading && totalCount == 0

    val hasNoResults: Boolean get() = !isLoading && totalCount > 0 && items.isEmpty()
}
