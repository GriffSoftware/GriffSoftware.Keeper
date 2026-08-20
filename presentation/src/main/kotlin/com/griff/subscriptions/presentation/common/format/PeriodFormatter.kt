package com.griff.subscriptions.presentation.common.format

import com.griff.subscriptions.domain.model.ExpensePeriod
import java.util.Locale

/** Formats an [ExpensePeriod] the way the period selector and the summaries name it. */
object PeriodFormatter {

    fun format(period: ExpensePeriod, locale: Locale = MoneyFormatter.PolishLocale): String =
        when (period) {
            is ExpensePeriod.Month -> DateFormatter.formatMonthAndYear(period.yearMonth, locale)
                .replaceFirstChar { it.titlecase(locale) }

            is ExpensePeriod.Year -> period.year.toString()

            is ExpensePeriod.Range -> "${DateFormatter.formatMonthAndYear(period.from, locale)} " +
                "- ${DateFormatter.formatMonthAndYear(period.toInclusive, locale)}"
        }
}
