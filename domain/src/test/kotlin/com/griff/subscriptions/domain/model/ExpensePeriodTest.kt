package com.griff.subscriptions.domain.model

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpensePeriodTest {

    private val today = LocalDate.of(2026, 8, 20)

    @Test
    fun `a year covers every day from january to december`() {
        val year = ExpensePeriod.Year(2026)

        assertEquals(LocalDate.of(2026, 1, 1), year.start)
        assertEquals(LocalDate.of(2026, 12, 31), year.endInclusive)
        assertEquals(12, year.months.size)
        assertTrue(year.contains(LocalDate.of(2026, 12, 31)))
        assertFalse(year.contains(LocalDate.of(2027, 1, 1)))
        assertFalse(year.contains(LocalDate.of(2025, 12, 31)))
    }

    @Test
    fun `a month covers its own days only, including a short february`() {
        val february = ExpensePeriod.Month(YearMonth.of(2026, 2))

        assertEquals(LocalDate.of(2026, 2, 1), february.start)
        assertEquals(LocalDate.of(2026, 2, 28), february.endInclusive)
        assertTrue(february.contains(LocalDate.of(2026, 2, 28)))
        assertFalse(february.contains(LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun `a range spans whole months from end to end`() {
        val range = ExpensePeriod.Range(YearMonth.of(2025, 9), YearMonth.of(2026, 8))

        assertEquals(12, range.months.size)
        assertEquals(LocalDate.of(2025, 9, 1), range.start)
        assertEquals(LocalDate.of(2026, 8, 31), range.endInclusive)
        assertFalse(range.contains(LocalDate.of(2025, 8, 31)))
    }

    @Test
    fun `shifting keeps the kind of period`() {
        assertEquals(ExpensePeriod.Year(2027), ExpensePeriod.Year(2026).shifted(1))
        assertEquals(
            ExpensePeriod.Month(YearMonth.of(2026, 7)),
            ExpensePeriod.Month(YearMonth.of(2026, 8)).shifted(-1),
        )
        assertEquals(
            ExpensePeriod.Range(YearMonth.of(2025, 10), YearMonth.of(2026, 9)),
            ExpensePeriod.Range(YearMonth.of(2025, 9), YearMonth.of(2026, 8)).shifted(1),
        )
    }

    @Test
    fun `shifting a month rolls over the year boundary`() {
        assertEquals(
            ExpensePeriod.Month(YearMonth.of(2027, 1)),
            ExpensePeriod.Month(YearMonth.of(2026, 12)).shifted(1),
        )
    }

    @Test
    fun `switching to a month keeps the user in the current month of the current year`() {
        assertEquals(
            ExpensePeriod.Month(YearMonth.of(2026, 8)),
            ExpensePeriod.Year(2026).asMonth(today),
        )
        assertEquals(
            ExpensePeriod.Month(YearMonth.of(2026, 8)),
            ExpensePeriod.trailingYear(today).asMonth(today),
        )
    }

    @Test
    fun `switching to a month of another year falls back to its first month`() {
        assertEquals(
            ExpensePeriod.Month(YearMonth.of(2031, 1)),
            ExpensePeriod.Year(2031).asMonth(today),
        )
    }

    @Test
    fun `switching to a year keeps the year that is being looked at`() {
        assertEquals(
            ExpensePeriod.Year(2026),
            ExpensePeriod.Month(YearMonth.of(2026, 3)).asYear(today),
        )
        assertEquals(
            ExpensePeriod.Year(2031),
            ExpensePeriod.Month(YearMonth.of(2031, 3)).asYear(today),
        )
    }

    @Test
    fun `switching a period to its own kind changes nothing`() {
        val month = ExpensePeriod.Month(YearMonth.of(2026, 3))
        val year = ExpensePeriod.Year(2031)

        assertEquals(month, month.asMonth(today))
        assertEquals(year, year.asYear(today))
    }

    @Test
    fun `the defaults are derived from the current date`() {
        assertEquals(ExpensePeriod.Year(2026), ExpensePeriod.currentYear(today))
        assertEquals(ExpensePeriod.Month(YearMonth.of(2026, 8)), ExpensePeriod.currentMonth(today))
        assertEquals(
            ExpensePeriod.Range(YearMonth.of(2025, 9), YearMonth.of(2026, 8)),
            ExpensePeriod.trailingYear(today),
        )
    }
}
