package com.griff.keeper.domain.model

import java.math.BigDecimal

/**
 * How many PLN one EUR is worth, always in this fixed direction - "1 EUR = [eurToPln] PLN" - no
 * matter which way an actual conversion runs (see
 * [com.griff.keeper.domain.calculation.MoneyConverter]). Presenting the rate the same way regardless
 * of direction means the number the user is asked for never needs a sentence explaining which
 * currency it multiplies.
 *
 * User input is validated by [com.griff.keeper.domain.validation.ExchangeRateParser]; the checks in
 * [ofOrNull] and in [init] are the same rule applied twice; a defensive last line, not the primary
 * validation.
 */
@JvmInline
value class ExchangeRate private constructor(val eurToPln: BigDecimal) {

    init {
        require(eurToPln.signum() > 0) { "Exchange rate must be positive, was $eurToPln" }
        require(eurToPln <= MAX_VALUE) { "Exchange rate is unreasonably large, was $eurToPln" }
        require(eurToPln.scale() <= MAX_SCALE) { "Exchange rate has too many decimals, was $eurToPln" }
    }

    companion object {
        /** Generous enough for any real currency pair, tight enough to keep conversions from overflowing. */
        val MAX_VALUE: BigDecimal = BigDecimal("1000000")

        const val MAX_SCALE: Int = 6

        fun ofOrNull(eurToPln: BigDecimal): ExchangeRate? =
            if (eurToPln.signum() > 0 && eurToPln <= MAX_VALUE && eurToPln.scale() <= MAX_SCALE) {
                ExchangeRate(eurToPln)
            } else {
                null
            }
    }
}
