package com.griff.keeper.presentation.common.format

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyFormatterTest {

    private val polish: Locale = Locale.forLanguageTag("pl-PL")
    private val english: Locale = Locale.forLanguageTag("en-US")

    @Test
    fun `formats amounts with polish separators`() {
        assertEquals("34,99", MoneyFormatter.formatAmount(Money.ofUnits(34, 99), polish))
        assertEquals("0,05", MoneyFormatter.formatAmount(Money.ofMinorUnits(5), polish))
        assertEquals("120,00", MoneyFormatter.formatAmount(Money.ofUnits(120), polish))
    }

    @Test
    fun `formats amounts with english separators`() {
        assertEquals("34.99", MoneyFormatter.formatAmount(Money.ofUnits(34, 99), english))
        assertEquals("0.05", MoneyFormatter.formatAmount(Money.ofMinorUnits(5), english))
        assertEquals("120.00", MoneyFormatter.formatAmount(Money.ofUnits(120), english))
    }

    @Test
    fun `groups thousands`() {
        // Polish locale uses a non breaking space as the grouping separator.
        assertEquals("1\u00A0299,00", MoneyFormatter.formatAmount(Money.ofUnits(1_299), polish))
        assertEquals("1,299.00", MoneyFormatter.formatAmount(Money.ofUnits(1_299), english))
    }

    @Test
    fun `appends the currency symbol of the active locale`() {
        assertEquals("34,99 zł", MoneyFormatter.format(Money.ofUnits(34, 99), locale = polish))
        assertEquals("34.99 PLN", MoneyFormatter.format(Money.ofUnits(34, 99), locale = english))
    }

    @Test
    fun `the amount is the same number in both languages`() {
        // Only the presentation changes with the language; the value never does.
        val polishText = MoneyFormatter.formatAmount(Money.ofUnits(1_299, 50), polish)
        val englishText = MoneyFormatter.formatAmount(Money.ofUnits(1_299, 50), english)

        assertEquals("129950", polishText.filter { it.isDigit() })
        assertEquals("129950", englishText.filter { it.isDigit() })
    }

    @Test
    fun `currency stays PLN in both languages`() {
        assertTrue(Currency.Default.symbol(polish).isNotBlank())
        assertEquals("PLN", Currency.PLN.code)
    }
}
