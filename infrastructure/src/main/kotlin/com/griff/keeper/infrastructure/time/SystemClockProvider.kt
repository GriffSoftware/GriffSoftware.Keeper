package com.griff.keeper.infrastructure.time

import com.griff.keeper.domain.time.ClockProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** [ClockProvider] backed by the device clock and time zone. */
@Singleton
class SystemClockProvider @Inject constructor() : ClockProvider {

    override fun zone(): ZoneId = ZoneId.systemDefault()

    override fun now(): Instant = Clock.systemUTC().instant()
}
