package com.griff.keeper.presentation.common.format

import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.validation.PriceParser

/**
 * Keyboard level guard for the price field.
 *
 * It only prevents obviously impossible keystrokes; the authoritative check stays in
 * [com.griff.keeper.domain.validation.PriceParser].
 */
object PriceInput {

    private const val MAX_UNIT_DIGITS = 7

    /** Drops characters that can never be part of a price and trims extra decimals. */
    fun sanitize(raw: String): String {
        val builder = StringBuilder()
        var separatorSeen = false
        var decimals = 0

        for (character in raw) {
            when {
                character.isDigit() -> {
                    if (separatorSeen) {
                        if (decimals == PriceParser.MAX_FRACTION_DIGITS) continue
                        decimals++
                        builder.append(character)
                    } else {
                        if (builder.length == MAX_UNIT_DIGITS) continue
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

    /** Renders a stored amount for editing, e.g. `1299,00`. */
    fun format(money: Money): String =
        "${money.wholeUnits},${money.fraction.toString().padStart(2, '0')}"
}
