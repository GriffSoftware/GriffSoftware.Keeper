package com.griff.subscriptions.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * A calendar window expenses are looked at through.
 *
 * Deliberately calendar based (a real month, a real year, a run of whole months) rather than an
 * arbitrary date range: an amount is booked to the year and month it was actually paid in, which is
 * the only attribution the user can reconcile with a bank statement.
 */
sealed interface ExpensePeriod {

    data class Month(val yearMonth: YearMonth) : ExpensePeriod {
        override val months: List<YearMonth> get() = listOf(yearMonth)
    }

    data class Year(val year: Int) : ExpensePeriod {
        override val months: List<YearMonth>
            get() = (1..MONTHS_PER_YEAR).map { YearMonth.of(year, it) }
    }

    /** A run of whole months, used for the rolling twelve month view. */
    data class Range(val from: YearMonth, val toInclusive: YearMonth) : ExpensePeriod {
        override val months: List<YearMonth>
            get() = buildList {
                var current = from
                while (!current.isAfter(toInclusive)) {
                    add(current)
                    current = current.plusMonths(1)
                }
            }
    }

    /** Every month the period spans, in chronological order. Never empty. */
    val months: List<YearMonth>

    val start: LocalDate get() = months.first().atDay(1)

    val endInclusive: LocalDate get() = months.last().atEndOfMonth()

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)

    /**
     * The same part of the calendar seen as a single month.
     *
     * When the period still covers today, today's month is what the user means by "this month" -
     * anything else would silently move them to January. Otherwise the window's first month wins.
     */
    fun asMonth(today: LocalDate): ExpensePeriod = when (this) {
        is Month -> this
        else -> Month(if (contains(today)) YearMonth.from(today) else months.first())
    }

    /** The same part of the calendar seen as a whole year, following the rule of [asMonth]. */
    fun asYear(today: LocalDate): ExpensePeriod = when (this) {
        is Year -> this
        else -> Year(if (contains(today)) today.year else months.first().year)
    }

    /** The same kind of period, shifted by [amount] units (months for a month, years for a year). */
    fun shifted(amount: Long): ExpensePeriod = when (this) {
        is Month -> Month(yearMonth.plusMonths(amount))
        is Year -> Year(year + amount.toInt())
        is Range -> Range(from.plusMonths(amount), toInclusive.plusMonths(amount))
    }

    companion object {
        private const val MONTHS_PER_YEAR = 12

        fun of(month: YearMonth): ExpensePeriod = Month(month)

        fun of(year: Int): ExpensePeriod = Year(year)

        /** The calendar year [today] falls into - the default the obligations screen opens with. */
        fun currentYear(today: LocalDate): ExpensePeriod = Year(today.year)

        fun currentMonth(today: LocalDate): ExpensePeriod = Month(YearMonth.from(today))

        /** The twelve months ending with the one [today] falls into. */
        fun trailingYear(today: LocalDate): ExpensePeriod {
            val current = YearMonth.from(today)
            return Range(from = current.minusMonths(MONTHS_PER_YEAR - 1L), toInclusive = current)
        }
    }
}
