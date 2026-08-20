package com.griff.subscriptions.domain.model

import java.time.LocalDate

/** Discriminator of [PaymentState], used by forms and by persistence. */
enum class PaymentStatus {
    PAID,
    UNPAID,
}

/**
 * Whether an obligation has been paid, and when.
 *
 * Modelled as a sealed type rather than as a `status` plus a nullable date, because "paid" without
 * a date cannot be attributed to a year and would silently disappear from every expense figure.
 * The date is therefore part of [Paid] and the invariant is impossible to break.
 */
sealed interface PaymentState {

    /** Settled on [paidOn]; that date is what yearly and monthly expenses are attributed to. */
    data class Paid(val paidOn: LocalDate) : PaymentState

    /** Still open. The deadline, if any, lives in [Obligation.dueDate]. */
    data object Unpaid : PaymentState

    val status: PaymentStatus
        get() = when (this) {
            is Paid -> PaymentStatus.PAID
            Unpaid -> PaymentStatus.UNPAID
        }

    /** The settlement date, or `null` while the obligation is open. */
    val paymentDate: LocalDate?
        get() = when (this) {
            is Paid -> paidOn
            Unpaid -> null
        }

    val isPaid: Boolean get() = this is Paid

    companion object {
        /** Builds a state from a status and an optional date, or `null` when the two disagree. */
        fun ofOrNull(status: PaymentStatus, paymentDate: LocalDate?): PaymentState? =
            when (status) {
                PaymentStatus.PAID -> paymentDate?.let(::Paid)
                PaymentStatus.UNPAID -> Unpaid
            }
    }
}
