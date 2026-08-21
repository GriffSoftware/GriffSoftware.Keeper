package com.griff.keeper.presentation.common.format

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats dates in the language the app is running in: `21 sierpnia 2026` in Polish,
 * `August 21, 2026` in English.
 *
 * Nothing about the wording or the order of the parts is written here - `FormatStyle.LONG` and CLDR
 * decide both, per locale, which is why a new language needs no change to this file. The locale
 * defaults to [Locale.getDefault], which `MainActivity` keeps in step with the per-app language.
 */
object DateFormatter {

    private val cache = HashMap<Locale, DateTimeFormatter>()

    fun formatFullDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        formatter(locale).format(date)

    /**
     * Compact form for list rows, e.g. `11.03.2027`.
     *
     * Numeric on purpose and identical in every language: it has to fit next to an amount in a list
     * row, where a localized month name would not.
     */
    fun formatShortDate(date: LocalDate): String = SHORT_DATE.format(date)

    /** Short label for chart axes, e.g. `IX`. Roman numerals read the same in both languages. */
    fun formatRomanMonth(month: YearMonth): String = ROMAN_MONTHS[month.monthValue - 1]

    fun formatMonthAndYear(month: YearMonth, locale: Locale = Locale.getDefault()): String =
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
        locale: Locale = Locale.getDefault(),
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
