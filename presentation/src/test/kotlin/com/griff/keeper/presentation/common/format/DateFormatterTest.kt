package com.griff.keeper.presentation.common.format

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatterTest {

    @Test
    fun `formats dates in polish`() {
        assertEquals("14 września 2026", DateFormatter.formatFullDate(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `formats roman month labels for charts`() {
        assertEquals("IX", DateFormatter.formatRomanMonth(YearMonth.of(2026, 9)))
        assertEquals("XII", DateFormatter.formatRomanMonth(YearMonth.of(2026, 12)))
        assertEquals("I", DateFormatter.formatRomanMonth(YearMonth.of(2027, 1)))
    }

    @Test
    fun `formats month and year`() {
        assertEquals("wrzesień 2026", DateFormatter.formatMonthAndYear(YearMonth.of(2026, 9)))
    }
}
