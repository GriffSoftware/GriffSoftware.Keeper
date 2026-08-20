package com.griff.subscriptions.domain.model

/**
 * Aggregated cost of a set of obligations for one period.
 *
 * [paid] is money that actually left the account inside the period; [outstanding] is what is still
 * open. The two are kept apart on purpose - adding them together would present a plan as history.
 */
data class ObligationTotals(
    val paid: Money,
    val outstanding: Money,
    val paidCount: Int,
    val outstandingCount: Int,
    val largestPaid: Money,
) {
    val count: Int get() = paidCount + outstandingCount

    companion object {
        val Empty = ObligationTotals(
            paid = Money.ZERO,
            outstanding = Money.ZERO,
            paidCount = 0,
            outstandingCount = 0,
            largestPaid = Money.ZERO,
        )
    }
}
