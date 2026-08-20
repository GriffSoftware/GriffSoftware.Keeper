package com.griff.subscriptions.presentation.common.format

import com.griff.subscriptions.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatterTest {

    @Test
    fun `formats amounts with polish separators`() {
        assertEquals("34,99", MoneyFormatter.formatAmount(Money.ofUnits(34, 99)))
        assertEquals("0,05", MoneyFormatter.formatAmount(Money.ofMinorUnits(5)))
        assertEquals("120,00", MoneyFormatter.formatAmount(Money.ofUnits(120)))
    }

    @Test
    fun `groups thousands`() {
        val formatted = MoneyFormatter.formatAmount(Money.ofUnits(1_299))

        // Polish locale uses a non breaking space as the grouping separator.
        assertEquals("1\u00A0299,00", formatted)
    }

    @Test
    fun `appends the currency symbol`() {
        assertEquals("34,99 zł", MoneyFormatter.format(Money.ofUnits(34, 99)))
    }
}
