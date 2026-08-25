package com.griff.keeper.domain.model

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExchangeRateTest {

    @Test
    fun `accepts a plausible rate`() {
        val rate = ExchangeRate.ofOrNull(BigDecimal("4.2537"))

        assertNotNull(rate)
        assertEquals(BigDecimal("4.2537"), rate.eurToPln)
    }

    @Test
    fun `rejects zero`() {
        assertNull(ExchangeRate.ofOrNull(BigDecimal.ZERO))
    }

    @Test
    fun `rejects a negative rate`() {
        assertNull(ExchangeRate.ofOrNull(BigDecimal("-1")))
    }

    @Test
    fun `rejects a rate above the maximum`() {
        assertNull(ExchangeRate.ofOrNull(ExchangeRate.MAX_VALUE.add(BigDecimal.ONE)))
    }

    @Test
    fun `accepts the maximum rate itself`() {
        assertNotNull(ExchangeRate.ofOrNull(ExchangeRate.MAX_VALUE))
    }

    @Test
    fun `rejects a rate with too many decimal places`() {
        assertNull(ExchangeRate.ofOrNull(BigDecimal("4.1234567")))
    }

    @Test
    fun `accepts a rate at the maximum scale`() {
        assertNotNull(ExchangeRate.ofOrNull(BigDecimal("4.123456")))
    }
}
