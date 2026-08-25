package com.griff.keeper.presentation.common.currency

import com.griff.keeper.domain.validation.ExchangeRateParser

/**
 * Keyboard level guard for the exchange rate field, mirroring
 * [com.griff.keeper.presentation.common.format.PriceInput] at the rate's own precision.
 *
 * It only prevents obviously impossible keystrokes; the authoritative check stays in
 * [ExchangeRateParser].
 */
internal object ExchangeRateInput {

    private const val MAX_INTEGER_DIGITS = 7

    fun sanitize(raw: String): String {
        val builder = StringBuilder()
        var separatorSeen = false
        var decimals = 0

        for (character in raw) {
            when {
                character.isDigit() -> {
                    if (separatorSeen) {
                        if (decimals == ExchangeRateParser.MAX_FRACTION_DIGITS) continue
                        decimals++
                        builder.append(character)
                    } else {
                        if (builder.length == MAX_INTEGER_DIGITS) continue
                        builder.append(character)
                    }
                }

                character == ',' || character == '.' -> {
                    if (separatorSeen || builder.isEmpty()) continue
                    separatorSeen = true
                    builder.append(',')
                }
            }
        }
        return builder.toString()
    }
}
