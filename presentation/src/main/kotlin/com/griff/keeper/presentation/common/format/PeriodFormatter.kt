package com.griff.keeper.presentation.common.format

import com.griff.keeper.domain.model.ExpensePeriod
import java.util.Locale

/** Formats an [ExpensePeriod] the way the period selector and the summaries name it. */
object PeriodFormatter {

    fun format(period: ExpensePeriod, locale: Locale = Locale.getDefault()): String =
        when (period) {
            is ExpensePeriod.Month -> DateFormatter.formatMonthAndYear(period.yearMonth, locale)
                .replaceFirstChar { it.titlecase(locale) }

            is ExpensePeriod.Year -> period.year.toString()

            is ExpensePeriod.Range -> "${DateFormatter.formatMonthAndYear(period.from, locale)} " +
                "- ${DateFormatter.formatMonthAndYear(period.toInclusive, locale)}"
        }
}
