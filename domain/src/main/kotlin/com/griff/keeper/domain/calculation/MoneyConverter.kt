package com.griff.keeper.domain.calculation

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Converts a [Money] amount between [Currency.PLN] and [Currency.EUR] using a user supplied
 * [ExchangeRate] - never a `Double` or `Float`, because a currency switch touches every stored
 * amount at once and a floating point rounding error would be silent and irreversible.
 *
 * The rate is always "1 EUR = X PLN" (see [ExchangeRate]); which way the division goes depends only
 * on [from] and [to]. Every result is rounded [RoundingMode.HALF_UP] to the minor unit, the same
 * rounding [Money.dividedBy] already uses elsewhere in the domain.
 */
object MoneyConverter {

    private const val MINOR_UNIT_SCALE = 2

    fun convert(amount: Money, from: Currency, to: Currency, rate: ExchangeRate): Money {
        if (from == to) return amount

        val units = BigDecimal.valueOf(amount.minorUnits, MINOR_UNIT_SCALE)
        val converted = when {
            from == Currency.PLN && to == Currency.EUR ->
                units.divide(rate.eurToPln, MINOR_UNIT_SCALE, RoundingMode.HALF_UP)

            from == Currency.EUR && to == Currency.PLN ->
                units.multiply(rate.eurToPln).setScale(MINOR_UNIT_SCALE, RoundingMode.HALF_UP)

            else -> error("MoneyConverter only supports PLN <-> EUR conversions, got $from -> $to")
        }

        return Money.ofMinorUnits(converted.movePointRight(MINOR_UNIT_SCALE).longValueExact())
    }
}
