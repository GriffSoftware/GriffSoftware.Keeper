package com.griff.keeper.presentation.obligations

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.PaymentState
import com.griff.keeper.presentation.R
import java.time.LocalDate

/**
 * How urgent a record's deadline is.
 *
 * Only [SOON] and [OVERDUE] are ever emphasized, and both always come with words - "Wygasa za 12
 * dni", "Termin minął" - so the state never depends on the color alone.
 */
enum class DeadlineUrgency {
    /** Far enough away to be ordinary information. */
    NORMAL,

    /** Close enough that the user may still want to react. */
    SOON,

    /** The date has passed. */
    OVERDUE,
}

/** What a row or a details screen says about a deadline, and how loudly. */
data class DeadlineStatus(
    val urgency: DeadlineUrgency,
    @param:PluralsRes val daysPluralRes: Int? = null,
    val days: Int = 0,
    @param:StringRes val textRes: Int? = null,
) {
    companion object {

        /** A deadline inside two weeks is close enough to be worth acting on. */
        const val SOON_THRESHOLD_DAYS: Long = 14

        /**
         * Reads the record's deadline the way its category means it.
         *
         * A settled insurance is about its cover running out; an open charge is about its payment
         * deadline. Records without a deadline get no marker at all rather than a reassuring one.
         */
        fun of(obligation: Obligation, today: LocalDate): DeadlineStatus? {
            val days = obligation.daysUntilDeadline(today) ?: return null
            val isExpiry = obligation.payment is PaymentState.Paid ||
                (obligation.dueDate == null && obligation.validUntil != null)

            return when {
                days < 0 -> DeadlineStatus(
                    urgency = DeadlineUrgency.OVERDUE,
                    textRes = if (isExpiry) R.string.deadline_expired else R.string.deadline_overdue,
                )

                days == 0L -> DeadlineStatus(
                    urgency = DeadlineUrgency.SOON,
                    textRes = if (isExpiry) {
                        R.string.deadline_expires_today
                    } else {
                        R.string.deadline_due_today
                    },
                )

                days <= SOON_THRESHOLD_DAYS -> DeadlineStatus(
                    urgency = DeadlineUrgency.SOON,
                    daysPluralRes = if (isExpiry) {
                        R.plurals.deadline_expires_in
                    } else {
                        R.plurals.deadline_due_in
                    },
                    days = days.toInt(),
                )

                else -> DeadlineStatus(urgency = DeadlineUrgency.NORMAL)
            }
        }
    }
}
