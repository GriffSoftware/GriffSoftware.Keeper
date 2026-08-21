package com.griff.keeper.presentation.common.format

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
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

    /**
     * Wall clock time of an instant, e.g. `00:43`.
     *
     * Takes the zone explicitly rather than reading the default: the caller already has one from the
     * clock abstraction, and a formatter that quietly consults the system is a formatter that behaves
     * differently in a test.
     */
    fun formatTime(instant: Instant, zone: ZoneId): String =
        // atZone rather than LocalTime.ofInstant: the latter only exists from API 31, while
        // ZonedDateTime has been available since java.time arrived on API 26.
        TIME.format(instant.atZone(zone).toLocalTime())

    fun formatFullDate(
        instant: Instant,
        zone: ZoneId,
        locale: Locale = MoneyFormatter.PolishLocale,
    ): String = formatFullDate(instant.atZone(zone).toLocalDate(), locale)

    fun formatShortDate(instant: Instant, zone: ZoneId): String =
        formatShortDate(instant.atZone(zone).toLocalDate())

    private fun formatter(locale: Locale): DateTimeFormatter = synchronized(cache) {
        cache.getOrPut(locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
        }
    }

    private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val ROMAN_MONTHS = listOf(
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
    )
}
