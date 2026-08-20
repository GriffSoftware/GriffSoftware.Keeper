package com.griff.subscriptions.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Abstraction over the system clock so that date dependent logic stays testable. */
interface ClockProvider {

    fun zone(): ZoneId

    fun now(): Instant

    fun today(): LocalDate = LocalDate.ofInstant(now(), zone())

    fun currentMonth(): YearMonth = YearMonth.from(today())
}
