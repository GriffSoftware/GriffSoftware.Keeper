package com.griff.keeper.domain.validation

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ExchangeRateParserTest {

    @Test
    fun `parses both decimal separators`() {
        assertEquals(BigDecimal("4.25"), parsed("4,25"))
        assertEquals(BigDecimal("4.25"), parsed("4.25"))
        assertEquals(BigDecimal("4.2537"), parsed("4,2537"))
        assertEquals(BigDecimal("4"), parsed("4"))
    }

    @Test
    fun `ignores whitespace used as a grouping separator`() {
        assertEquals(BigDecimal("1299.5"), parsed("1 299,5"))
        assertEquals(BigDecimal("1299.5"), parsed("1 299,5"))
    }

    @Test
    fun `rejects non numeric input`() {
        assertEquals(ExchangeRateError.MALFORMED, error("abc"))
        assertEquals(ExchangeRateError.MALFORMED, error("4,2a"))
        assertEquals(ExchangeRateError.MALFORMED, error("4,2,5"))
        assertEquals(ExchangeRateError.MALFORMED, error("4e3"))
    }

    @Test
    fun `rejects a negative rate`() {
        assertEquals(ExchangeRateError.NEGATIVE, error("-4,25"))
    }

    @Test
    fun `rejects empty and zero`() {
        assertEquals(ExchangeRateError.EMPTY, error(""))
        assertEquals(ExchangeRateError.EMPTY, error("   "))
        assertEquals(ExchangeRateError.ZERO, error("0"))
        assertEquals(ExchangeRateError.ZERO, error("0,00"))
    }

    @Test
    fun `rejects more than six decimal places`() {
        assertEquals(ExchangeRateError.TOO_MANY_DECIMALS, error("4,1234567"))
    }

    @Test
    fun `rejects an oversized rate`() {
        assertEquals(ExchangeRateError.TOO_LARGE, error("12345678"))
    }

    private fun parsed(raw: String): BigDecimal =
        (ExchangeRateParser.parse(raw) as ExchangeRateParseResult.Success).rate.eurToPln

    private fun error(raw: String): ExchangeRateError =
        (ExchangeRateParser.parse(raw) as ExchangeRateParseResult.Failure).error
}
