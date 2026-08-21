package com.griff.subscriptions.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A periodic insurance policy, tax or other recurring charge owned by the user.
 *
 * The three dates are *not* interchangeable and each answers a different question:
 * - [PaymentState.Paid.paidOn] - when money actually left the account. Only this date decides which
 *   year or month an expense is booked to (see [ExpensePeriod]).
 * - [validUntil] - when cover ends. Drives expiry reminders, never expense attribution: a policy
 *   paid in December 2026 and valid until December 2027 is a 2026 expense.
 * - [dueDate] - the deadline of an unpaid charge, for costs that have no cover period at all.
 *
 * Not every category needs every date, so all three are optional in the model; the form decides
 * which ones it puts forward.
 */
data class Obligation(
    val id: ObligationId,
    val name: ObligationName,
    val category: ObligationCategory,
    val amount: Money,
    val currency: Currency,
    val payment: PaymentState,
    val dueDate: LocalDate?,
    val validUntil: LocalDate?,
    val notes: String?,
    /**
     * Whether this record may produce reminders.
     *
     * Independent of the app-wide switch, for the same reason as on
     * [com.griff.subscriptions.domain.model.Subscription]: the global switch blocks delivery, it
     * does not edit the user's per-record choices.
     */
    val remindersEnabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val tag: ObligationTag get() = category.tag

    val isPaid: Boolean get() = payment.isPaid

    val paymentDate: LocalDate? get() = payment.paymentDate

    /**
     * The date the user has to act on next, or `null` when the record has no deadline at all.
     *
     * An open charge is about its payment deadline; a settled one is about the end of its cover.
     * Reminders about expiring policies will be able to work off exactly this value.
     */
    val deadline: LocalDate?
        get() = when (payment) {
            is PaymentState.Paid -> validUntil ?: dueDate
            PaymentState.Unpaid -> dueDate ?: validUntil
        }

    /**
     * The date the record is booked to when a period filter is applied.
     *
     * Settled records are booked by their payment date - never by their expiry - so the filter
     * cannot pull a policy into a year it was not paid in. Open records fall back to the deadline
     * they are waiting for; a record without any date is never filtered out by a period.
     */
    val periodDate: LocalDate? get() = payment.paymentDate ?: dueDate ?: validUntil

    /** Days left until [deadline], negative once it has passed, `null` without a deadline. */
    fun daysUntilDeadline(today: LocalDate): Long? =
        deadline?.let { today.until(it, java.time.temporal.ChronoUnit.DAYS) }
}
