package com.griff.keeper.presentation.common.format

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DateFormatterTest {

    private val polish: Locale = Locale.forLanguageTag("pl-PL")
    private val english: Locale = Locale.forLanguageTag("en-US")

    /** The same day, named in each language. */
    private val date: LocalDate = LocalDate.of(2026, 8, 21)

    @Test
    fun `formats dates in polish`() {
        assertEquals("14 września 2026", DateFormatter.formatFullDate(LocalDate.of(2026, 9, 14), polish))
    }

    @Test
    fun `formats the same date in each language`() {
        val polishText = DateFormatter.formatFullDate(date, polish)
        val englishText = DateFormatter.formatFullDate(date, english)

        // The exact punctuation comes from CLDR and is not worth pinning down; that the month is
        // named in the right language, and that the two differ, is the behaviour under test.
        assertTrue(polishText.contains("sierpnia"), polishText)
        assertTrue(polishText.contains("21") && polishText.contains("2026"), polishText)
        assertTrue(englishText.contains("August"), englishText)
        assertTrue(englishText.contains("21") && englishText.contains("2026"), englishText)
        assertNotEquals(polishText, englishText)
    }

    @Test
    fun `follows the default locale when none is given`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(polish)
            assertTrue(DateFormatter.formatFullDate(date).contains("sierpnia"))
            Locale.setDefault(english)
            assertTrue(DateFormatter.formatFullDate(date).contains("August"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `formats roman month labels for charts`() {
        // Roman numerals are the same in both languages, which is why the chart axis uses them.
        assertEquals("IX", DateFormatter.formatRomanMonth(YearMonth.of(2026, 9)))
        assertEquals("XII", DateFormatter.formatRomanMonth(YearMonth.of(2026, 12)))
        assertEquals("I", DateFormatter.formatRomanMonth(YearMonth.of(2027, 1)))
    }

    @Test
    fun `formats month and year`() {
        assertEquals("wrzesień 2026", DateFormatter.formatMonthAndYear(YearMonth.of(2026, 9), polish))
        assertEquals("September 2026", DateFormatter.formatMonthAndYear(YearMonth.of(2026, 9), english))
    }

    @Test
    fun `short dates stay numeric in both languages`() {
        // Deliberately locale independent: it has to fit next to an amount in a list row.
        assertEquals("21.08.2026", DateFormatter.formatShortDate(date))
    }
}
