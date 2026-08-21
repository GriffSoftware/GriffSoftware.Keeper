package com.griff.keeper.presentation.common.format

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats [Money] for display, e.g. `34,99 zł` or `1 299,00 zł`.
 *
 * Amounts are converted through [BigDecimal] so the exact minor units are printed; no floating
 * point arithmetic is involved. Formatters are cached per locale because [DecimalFormat] creation is
 * comparatively expensive and the value is rendered in long lists.
 */
object MoneyFormatter {

    private const val PATTERN = "#,##0.00"

    private val formatters = HashMap<Locale, DecimalFormat>()

    fun format(money: Money, currency: Currency = Currency.Default, locale: Locale = PolishLocale): String =
        "${formatAmount(money, locale)} ${currency.symbol()}"

    fun formatAmount(money: Money, locale: Locale = PolishLocale): String =
        formatter(locale).format(money.toBigDecimal())

    private fun formatter(locale: Locale): DecimalFormat = synchronized(formatters) {
        formatters.getOrPut(locale) { DecimalFormat(PATTERN, DecimalFormatSymbols(locale)) }
    }

    private fun Money.toBigDecimal(): BigDecimal =
        BigDecimal.valueOf(minorUnits, MONEY_SCALE)

    private const val MONEY_SCALE = 2

    val PolishLocale: Locale = Locale.forLanguageTag("pl-PL")
}

/** Currency symbol is brand/locale data rather than translatable UI copy. */
internal fun Currency.symbol(): String = when (this) {
    Currency.PLN -> "zł"
}
