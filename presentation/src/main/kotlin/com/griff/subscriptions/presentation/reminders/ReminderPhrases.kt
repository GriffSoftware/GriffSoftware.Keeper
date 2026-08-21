package com.griff.subscriptions.presentation.reminders

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.griff.subscriptions.domain.reminder.ReminderKind
import com.griff.subscriptions.domain.reminder.ReminderSchedule
import com.griff.subscriptions.presentation.R

/**
 * The words the reminder screens use for dates and counts.
 *
 * Gathered in one place because the same three phrases - what the date is called, how far away it
 * is, and when the next reminder lands - appear on the dashboard, on both details screens and in
 * the accessibility labels, and they have to agree.
 */
internal object ReminderPhrases {

    /** What the target date is called for this kind of reminder. */
    @StringRes
    fun targetLabel(kind: ReminderKind): Int = when (kind) {
        ReminderKind.INSURANCE_EXPIRY -> R.string.reminders_target_expiry
        ReminderKind.PAYMENT_DUE -> R.string.reminders_target_due
        ReminderKind.SUBSCRIPTION_RENEWAL -> R.string.reminders_target_renewal
    }

    /** "Dzisiaj", "Jutro", "Za 12 dni" - always words, never a bare color or icon. */
    @Composable
    fun relativeDays(days: Long): String = when {
        days <= 0L -> stringResource(R.string.reminders_relative_today)
        days == 1L -> stringResource(R.string.reminders_relative_tomorrow)
        else -> pluralStringResource(R.plurals.reminders_relative_days, days.toInt(), days.toInt())
    }

    /** The lower case form used inside a sentence: "21 sierpnia • dzisiaj". */
    @Composable
    fun relativeDaysInline(days: Long): String = when {
        days <= 0L -> stringResource(R.string.reminders_when_today)
        days == 1L -> stringResource(R.string.reminders_when_tomorrow)
        else -> pluralStringResource(R.plurals.reminders_when_days, days.toInt(), days.toInt())
    }

    /** "7 dni przed odnowieniem" - explains *why* the next reminder falls where it does. */
    @Composable
    fun offsetExplanation(kind: ReminderKind, daysBefore: Int): String {
        val plural = when (kind) {
            ReminderKind.INSURANCE_EXPIRY -> R.plurals.reminder_section_days_before_expiry
            ReminderKind.PAYMENT_DUE -> R.plurals.reminder_section_days_before_due
            ReminderKind.SUBSCRIPTION_RENEWAL -> R.plurals.reminder_section_days_before_renewal
        }
        return pluralStringResource(plural, daysBefore, daysBefore)
    }

    /** "30 dni • 7 dni • 1 dzień" for the defaults section. */
    @Composable
    fun scheduleSummary(schedule: ReminderSchedule): String {
        val separator = stringResource(R.string.reminders_defaults_separator)
        // Resolved eagerly: a composable call cannot happen inside the joinToString lambda.
        val parts = schedule.daysBefore.map { days ->
            pluralStringResource(R.plurals.reminders_days_before, days, days)
        }
        return parts.joinToString(separator = separator)
    }
}
