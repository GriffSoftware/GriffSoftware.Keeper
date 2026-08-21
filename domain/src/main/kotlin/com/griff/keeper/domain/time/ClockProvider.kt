package com.griff.keeper.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Abstraction over the system clock so that date dependent logic stays testable. */
interface ClockProvider {

    fun zone(): ZoneId

    fun now(): Instant

    /**
     * `atZone` rather than `LocalDate.ofInstant`: the latter is a Java 9 addition that Android only
     * ships from API 34, so on everything below it the shorter call would be a `NoSuchMethodError`
     * at runtime rather than a compile error. The result is identical.
     */
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    fun currentMonth(): YearMonth = YearMonth.from(today())
}
