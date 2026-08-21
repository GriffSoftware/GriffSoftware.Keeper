package com.griff.keeper.domain.validation

import com.griff.keeper.domain.model.Money

/**
 * Parses user typed prices such as `34`, `34,9`, `34,99` or `34.99` into [Money].
 *
 * The parser is the single place where free text becomes a domain amount; UI level input filters
 * are only a convenience on top of it. Whitespace is ignored so that grouped values pasted from
 * other apps (`1 299,00`) are accepted as well.
 */
object PriceParser {

    /** Upper bound that keeps totals far away from [Long] overflow: 1 000 000,00. */
    const val MAX_UNITS: Long = 1_000_000

    const val MAX_FRACTION_DIGITS: Int = 2

    private const val MAX_UNIT_DIGITS = 9

    private val NUMBER = Regex("""^\d+([.,]\d+)?$""")

    private val IGNORED_CHARS = charArrayOf('\u00A0', '\u202F', '\u2007')

    fun parse(raw: String): PriceParseResult {
        val normalized = raw.filterNot { it.isWhitespace() || it in IGNORED_CHARS }
        if (normalized.isEmpty()) return failure(PriceError.EMPTY)
        if (normalized.startsWith('-')) return failure(PriceError.NEGATIVE)
        if (!NUMBER.matches(normalized)) return failure(PriceError.MALFORMED)

        val separatorIndex = normalized.indexOfFirst { it == '.' || it == ',' }
        val unitsPart = if (separatorIndex < 0) normalized else normalized.take(separatorIndex)
        val fractionPart = if (separatorIndex < 0) "" else normalized.substring(separatorIndex + 1)

        if (fractionPart.length > MAX_FRACTION_DIGITS) return failure(PriceError.TOO_MANY_DECIMALS)
        if (unitsPart.length > MAX_UNIT_DIGITS) return failure(PriceError.TOO_LARGE)

        val units = unitsPart.toLong()
        if (units > MAX_UNITS) return failure(PriceError.TOO_LARGE)

        val money = Money.ofUnits(units, fractionPart.padEnd(MAX_FRACTION_DIGITS, '0').toInt())
        if (money.isZero) return failure(PriceError.ZERO)
        return PriceParseResult.Success(money)
    }

    private fun failure(error: PriceError) = PriceParseResult.Failure(error)
}

sealed interface PriceParseResult {
    data class Success(val money: Money) : PriceParseResult
    data class Failure(val error: PriceError) : PriceParseResult
}

enum class PriceError {
    EMPTY,
    MALFORMED,
    NEGATIVE,
    ZERO,
    TOO_MANY_DECIMALS,
    TOO_LARGE,
}
