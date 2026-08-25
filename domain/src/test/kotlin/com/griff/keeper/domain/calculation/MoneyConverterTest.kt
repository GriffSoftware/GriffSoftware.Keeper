package com.griff.keeper.domain.calculation

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyConverterTest {

    private fun rate(value: String): ExchangeRate =
        ExchangeRate.ofOrNull(BigDecimal(value)) ?: error("test rate must be valid: $value")

    @Test
    fun `converts PLN to EUR by dividing by the rate`() {
        // 1 EUR = 4.25 PLN, 425.00 PLN -> 100.00 EUR
        val converted = MoneyConverter.convert(
            amount = Money.ofUnits(425),
            from = Currency.PLN,
            to = Currency.EUR,
            rate = rate("4.25"),
        )

        assertEquals(Money.ofUnits(100), converted)
    }

    @Test
    fun `converts EUR to PLN by multiplying by the rate`() {
        // 1 EUR = 4.25 PLN, 100.00 EUR -> 425.00 PLN
        val converted = MoneyConverter.convert(
            amount = Money.ofUnits(100),
            from = Currency.EUR,
            to = Currency.PLN,
            rate = rate("4.25"),
        )

        assertEquals(Money.ofUnits(425), converted)
    }

    @Test
    fun `rounds PLN to EUR half up to the minor unit`() {
        // 59.99 / 4.25 = 14.1152941... -> 14.12
        val converted = MoneyConverter.convert(
            amount = Money.ofUnits(59, 99),
            from = Currency.PLN,
            to = Currency.EUR,
            rate = rate("4.25"),
        )

        assertEquals(Money.ofUnits(14, 12), converted)
    }

    @Test
    fun `rounds EUR to PLN half up to the minor unit`() {
        // 0.01 EUR * 4.5 = 0.045 PLN, exactly halfway between 0.04 and 0.05. HALF_UP rounds away from
        // zero at a tie - the case that actually distinguishes it from HALF_EVEN (which would keep the
        // even 0.04).
        val converted = MoneyConverter.convert(
            amount = Money.ofMinorUnits(1),
            from = Currency.EUR,
            to = Currency.PLN,
            rate = rate("4.5"),
        )

        assertEquals(Money.ofMinorUnits(5), converted)
    }

    @Test
    fun `a currency converted to itself is unchanged`() {
        val amount = Money.ofUnits(59, 99)

        assertEquals(amount, MoneyConverter.convert(amount, Currency.PLN, Currency.PLN, rate("4.25")))
        assertEquals(amount, MoneyConverter.convert(amount, Currency.EUR, Currency.EUR, rate("4.25")))
    }

    @Test
    fun `PLN to EUR to PLN can differ by a rounding cent`() {
        val original = Money.ofUnits(59, 99)
        val usedRate = rate("4.25")

        val toEur = MoneyConverter.convert(original, Currency.PLN, Currency.EUR, usedRate)
        val backToPln = MoneyConverter.convert(toEur, Currency.EUR, Currency.PLN, usedRate)

        // 59.99 -> 14.12 -> 60.01, one cent away from the original - exactly the rounding warning
        // shown to the user before a conversion is confirmed.
        assertEquals(Money.ofUnits(60, 1), backToPln)
    }

    @Test
    fun `zero converts to zero in either direction`() {
        assertEquals(Money.ZERO, MoneyConverter.convert(Money.ZERO, Currency.PLN, Currency.EUR, rate("4.25")))
        assertEquals(Money.ZERO, MoneyConverter.convert(Money.ZERO, Currency.EUR, Currency.PLN, rate("4.25")))
    }
}
