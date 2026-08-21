package com.griff.subscriptions.infrastructure.reminder

import android.content.Context
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.reminder.ReminderKind
import com.griff.subscriptions.domain.reminder.ReminderNotification
import com.griff.subscriptions.infrastructure.R
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
 */
internal class ReminderNotificationTextFactory(private val context: Context) {

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
     * infrastructure to UI.
     */
    private fun formatMoney(money: Money, currency: Currency): String {
        val amount = AmountFormat.format(BigDecimal.valueOf(money.minorUnits, MONEY_SCALE))
        val symbol = when (currency) {
            Currency.PLN -> context.getString(R.string.reminder_currency_pln)
        }
        return "$amount $symbol"
    }

    private fun fullDate(date: LocalDate): String = FullDate.format(date)

    private fun dayAndMonth(date: LocalDate): String = DayAndMonth.format(date)

    private companion object {
        const val MONEY_SCALE = 2

        val PolishLocale: Locale = Locale.forLanguageTag("pl-PL")

        val AmountFormat: DecimalFormat =
            DecimalFormat("#,##0.00", DecimalFormatSymbols(PolishLocale))

        /** `20 września 2026` - the genitive form Polish uses after a day number. */
        val FullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", PolishLocale)

        /** `28 sierpnia` - the year is noise when the date is a week away. */
        val DayAndMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", PolishLocale)
    }
}
