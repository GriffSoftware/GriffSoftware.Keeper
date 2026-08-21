package com.griff.keeper.presentation.common.format

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Formats dates the way a Polish user expects them, e.g. `14 września 2026`. */
object DateFormatter {

    private val cache = HashMap<Locale, DateTimeFormatter>()

    fun formatFullDate(date: LocalDate, locale: Locale = MoneyFormatter.PolishLocale): String =
        formatter(locale).format(date)

    /** Compact form for list rows, e.g. `11.03.2027`. */
    fun formatShortDate(date: LocalDate): String = SHORT_DATE.format(date)

    /** Short label for chart axes, e.g. `IX`. */
    fun formatRomanMonth(month: YearMonth): String = ROMAN_MONTHS[month.monthValue - 1]

    fun formatMonthAndYear(month: YearMonth, locale: Locale = MoneyFormatter.PolishLocale): String =
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month)

    private fun formatter(locale: Locale): DateTimeFormatter = synchronized(cache) {
        cache.getOrPut(locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
        }
    }

    private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val ROMAN_MONTHS = listOf(
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
    )
}
