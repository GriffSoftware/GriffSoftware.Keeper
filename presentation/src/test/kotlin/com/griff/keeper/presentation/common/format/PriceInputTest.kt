package com.griff.keeper.presentation.common.format

import com.griff.keeper.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceInputTest {

    @Test
    fun `keeps digits and a single separator`() {
        assertEquals("34,99", PriceInput.sanitize("34,99"))
        assertEquals("34,99", PriceInput.sanitize("34.99"))
        assertEquals("34,99", PriceInput.sanitize("34,99,5"))
    }

    @Test
    fun `drops characters that cannot be part of a price`() {
        assertEquals("3499", PriceInput.sanitize("34zł99"))
        assertEquals("", PriceInput.sanitize("abc"))
        assertEquals("12", PriceInput.sanitize("-12"))
    }

    @Test
    fun `limits the number of decimals`() {
        assertEquals("34,55", PriceInput.sanitize("34,555"))
    }

    @Test
    fun `ignores a leading separator`() {
        assertEquals("99", PriceInput.sanitize(",99"))
    }

    @Test
    fun `formats stored amounts for editing`() {
        assertEquals("34,99", PriceInput.format(Money.ofUnits(34, 99)))
        assertEquals("1299,00", PriceInput.format(Money.ofUnits(1_299)))
        assertEquals("5,05", PriceInput.format(Money.ofMinorUnits(505)))
    }
}
