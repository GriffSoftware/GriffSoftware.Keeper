package com.griff.subscriptions.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class PriceParserTest {

    @Test
    fun `parses integers and both decimal separators`() {
        assertEquals(3400, parsed("34"))
        assertEquals(3450, parsed("34,5"))
        assertEquals(3450, parsed("34,50"))
        assertEquals(3450, parsed("34.50"))
        assertEquals(3499, parsed("34,99"))
        assertEquals(129_900, parsed("1299"))
    }

    @Test
    fun `ignores whitespace used as a grouping separator`() {
        assertEquals(3499, parsed(" 34,99 "))
        assertEquals(129_900, parsed("1 299,00"))
        assertEquals(129_900, parsed("1\u00A0299,00"))
    }

    @Test
    fun `rejects non numeric input`() {
        assertEquals(PriceError.MALFORMED, error("abc"))
        assertEquals(PriceError.MALFORMED, error("34,9a"))
        assertEquals(PriceError.MALFORMED, error("34,9,9"))
        assertEquals(PriceError.MALFORMED, error("12e3"))
    }

    @Test
    fun `rejects negative amounts`() {
        assertEquals(PriceError.NEGATIVE, error("-12"))
        assertEquals(PriceError.NEGATIVE, error("-0,01"))
    }

    @Test
    fun `rejects more than two decimals`() {
        assertEquals(PriceError.TOO_MANY_DECIMALS, error("34,555"))
        assertEquals(PriceError.TOO_MANY_DECIMALS, error("34.1234"))
    }

    @Test
    fun `rejects empty zero and oversized amounts`() {
        assertEquals(PriceError.EMPTY, error(""))
        assertEquals(PriceError.EMPTY, error("   "))
        assertEquals(PriceError.ZERO, error("0"))
        assertEquals(PriceError.ZERO, error("0,00"))
        assertEquals(PriceError.TOO_LARGE, error("1000001"))
    }

    private fun parsed(raw: String): Long =
        (PriceParser.parse(raw) as PriceParseResult.Success).money.minorUnits

    private fun error(raw: String): PriceError =
        (PriceParser.parse(raw) as PriceParseResult.Failure).error
}
