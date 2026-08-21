package com.griff.keeper.infrastructure.reminder

import android.content.Context
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.reminder.ReminderKind
import com.griff.keeper.domain.reminder.ReminderNotification
import com.griff.keeper.infrastructure.R
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The three lines of a reminder notification, already localized. */
internal data class ReminderNotificationCopy(
    val title: String,
    val subText: String,
    val contentText: String,
)

/**
 * Turns a decided reminder into the words the user reads.
 *
 * The domain says *what* and *why*; the wording, the localized date and the currency belong to the
 * platform, so they live here next to the resources they use. The title is always the record's own
 * name - it is the thing the user recognises in a crowded drawer - while the subtext carries the
 * urgency and the body carries the facts.
 *
 * The [context] has to be one that already resolves against the app's own language (see
 * [withAppLocale]); the dates and amounts are formatted with that same context's locale, so a
 * notification cannot end up half translated.
 */
internal class ReminderNotificationTextFactory(private val context: Context) {

    private val locale: Locale = context.resolvedLocale()

    fun copyFor(notification: ReminderNotification): ReminderNotificationCopy {
        val occurrence = notification.occurrence
        val days = occurrence.daysBefore
        val target = occurrence.targetDate
        val amount = formatMoney(notification.amount, notification.currency)

        return when (occurrence.kind) {
            ReminderKind.INSURANCE_EXPIRY -> ReminderNotificationCopy(
                title = notification.title,
                subText = if (days <= 1) {
                    context.getString(R.string.reminder_insurance_subtext_tomorrow)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.reminder_insurance_subtext_in_days,
                        days,
                        days,
                    )
                },
                contentText = if (days <= 1) {
                    context.getString(R.string.reminder_insurance_text_tomorrow, fullDate(target))
                } else {
                    context.getString(R.string.reminder_insurance_text, fullDate(target))
                },
            )

            ReminderKind.PAYMENT_DUE -> ReminderNotificationCopy(
                title = notification.title,
                subText = if (days <= 1) {
                    context.getString(R.string.reminder_payment_subtext_tomorrow)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.reminder_payment_subtext_in_days,
                        days,
                        days,
                    )
                },
                contentText = if (days <= 1) {
                    context.getString(R.string.reminder_payment_text_tomorrow, amount)
                } else {
                    context.getString(R.string.reminder_payment_text, amount, fullDate(target))
                },
            )

            ReminderKind.SUBSCRIPTION_RENEWAL -> ReminderNotificationCopy(
                title = notification.title,
                subText = if (days <= 1) {
                    context.getString(R.string.reminder_renewal_subtext_tomorrow)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.reminder_renewal_subtext_in_days,
                        days,
                        days,
                    )
                },
                contentText = when {
                    days <= 1 -> context.getString(R.string.reminder_renewal_text_tomorrow, amount)
                    notification.billingPeriod == BillingPeriod.YEARLY ->
                        context.getString(R.string.reminder_renewal_text_yearly, amount)

                    else -> context.getString(R.string.reminder_renewal_text, amount, dayAndMonth(target))
                },
            )
        }
    }

    /**
     * Money and dates are formatted here rather than reused from the UI layer.
     *
     * A notification is built by a background worker that must not depend on the presentation
     * module; the few lines of formatting are a smaller price than an upward dependency from
     * infrastructure to UI. Both the separators and the currency symbol come from CLDR through the
     * active locale, so `34,99 zł` in Polish is `34.99 PLN` in English without a symbol being
     * written out anywhere.
     */
    private fun formatMoney(money: Money, currency: Currency): String {
        val format = DecimalFormat(AMOUNT_PATTERN, DecimalFormatSymbols(locale))
        val amount = format.format(BigDecimal.valueOf(money.minorUnits, MONEY_SCALE))
        val symbol = java.util.Currency.getInstance(currency.code).getSymbol(locale)
        return "$amount $symbol"
    }

    /** `20 września 2026` / `September 20, 2026` - CLDR decides the order and the case. */
    private fun fullDate(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)

    /** `28 sierpnia` / `August 28` - the year is noise when the date is a week away. */
    private fun dayAndMonth(date: LocalDate): String =
        DateTimeFormatter
            .ofPattern(context.getString(R.string.reminder_date_day_month_pattern), locale)
            .format(date)

    private companion object {
        const val MONEY_SCALE = 2
        const val AMOUNT_PATTERN = "#,##0.00"
    }
}
