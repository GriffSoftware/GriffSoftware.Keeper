package com.griff.keeper.presentation.common.format

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats [Money] for display, e.g. `34,99 zł` in Polish or `34.99 PLN` in English.
 *
 * The amount that is stored never changes: the currency stays PLN in every language, and only the
 * separators and the symbol follow the locale, which is what `NumberFormat` and CLDR already know
 * how to do. Amounts are converted through [BigDecimal] so the exact minor units are printed; no
 * floating point arithmetic is involved. Formatters are cached per locale because [DecimalFormat]
 * creation is comparatively expensive and the value is rendered in long lists.
 */
object MoneyFormatter {

    private const val PATTERN = "#,##0.00"

    private val formatters = HashMap<Locale, DecimalFormat>()

    fun format(
        money: Money,
        currency: Currency = Currency.Default,
        locale: Locale = Locale.getDefault(),
    ): String = "${formatAmount(money, locale)} ${currency.symbol(locale)}"

    fun formatAmount(money: Money, locale: Locale = Locale.getDefault()): String =
        formatter(locale).format(money.toBigDecimal())

    private fun formatter(locale: Locale): DecimalFormat = synchronized(formatters) {
        formatters.getOrPut(locale) { DecimalFormat(PATTERN, DecimalFormatSymbols(locale)) }
    }

    private fun Money.toBigDecimal(): BigDecimal =
        BigDecimal.valueOf(minorUnits, MONEY_SCALE)

    private const val MONEY_SCALE = 2
}

/**
 * The symbol a reader of [locale] expects for this currency: `zł` for a Polish reader, `PLN` for an
 * English one.
 *
 * Read from CLDR through [java.util.Currency] rather than written out per language: a currency
 * symbol is locale data, not UI copy, and the ISO code the domain already carries is all the JDK
 * needs to look it up.
 */
internal fun Currency.symbol(locale: Locale): String =
    java.util.Currency.getInstance(code).getSymbol(locale)
