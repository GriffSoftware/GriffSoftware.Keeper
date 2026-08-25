package com.griff.keeper.domain.validation

import com.griff.keeper.domain.model.ExchangeRate
import java.math.BigDecimal

/**
 * Parses a user typed "1 EUR = X PLN" rate such as `4,25`, `4.2537` or `1 299,00` into an
 * [ExchangeRate].
 *
 * Mirrors [PriceParser] deliberately: whitespace and grouping characters are ignored and both `,`
 * and `.` are accepted as the decimal separator, independent of the active *language* - the rate the
 * user types is not price input, but the same "either separator, no ambiguity" rule applies to it.
 */
object ExchangeRateParser {

    const val MAX_FRACTION_DIGITS: Int = ExchangeRate.MAX_SCALE

    private const val MAX_INTEGER_DIGITS = 7

    private val NUMBER = Regex("""^\d+([.,]\d+)?$""")

    private val IGNORED_CHARS = charArrayOf(' ', ' ', ' ')

    fun parse(raw: String): ExchangeRateParseResult {
        val normalized = raw.filterNot { it.isWhitespace() || it in IGNORED_CHARS }
        if (normalized.isEmpty()) return failure(ExchangeRateError.EMPTY)
        if (normalized.startsWith('-')) return failure(ExchangeRateError.NEGATIVE)
        if (!NUMBER.matches(normalized)) return failure(ExchangeRateError.MALFORMED)

        val separatorIndex = normalized.indexOfFirst { it == '.' || it == ',' }
        val integerPart = if (separatorIndex < 0) normalized else normalized.take(separatorIndex)
        val fractionPart = if (separatorIndex < 0) "" else normalized.substring(separatorIndex + 1)

        if (fractionPart.length > MAX_FRACTION_DIGITS) return failure(ExchangeRateError.TOO_MANY_DECIMALS)
        if (integerPart.length > MAX_INTEGER_DIGITS) return failure(ExchangeRateError.TOO_LARGE)

        // No trailing ".0" for a plain integer: the exact scale the user typed is preserved end to
        // end, because it is what the preview and confirmation dialogs render back to them.
        val canonical = if (fractionPart.isEmpty()) integerPart else "$integerPart.$fractionPart"
        val value = BigDecimal(canonical)
        if (value.signum() == 0) return failure(ExchangeRateError.ZERO)

        val rate = ExchangeRate.ofOrNull(value) ?: return failure(ExchangeRateError.TOO_LARGE)
        return ExchangeRateParseResult.Success(rate)
    }

    private fun failure(error: ExchangeRateError) = ExchangeRateParseResult.Failure(error)
}

sealed interface ExchangeRateParseResult {
    data class Success(val rate: ExchangeRate) : ExchangeRateParseResult
    data class Failure(val error: ExchangeRateError) : ExchangeRateParseResult
}

enum class ExchangeRateError {
    EMPTY,
    MALFORMED,
    NEGATIVE,
    ZERO,
    TOO_MANY_DECIMALS,
    TOO_LARGE,
}
