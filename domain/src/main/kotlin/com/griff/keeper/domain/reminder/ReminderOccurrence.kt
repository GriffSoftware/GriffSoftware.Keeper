package com.griff.keeper.domain.reminder

import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One concrete reminder: "on [fireDate], tell the user about [targetDate], which is [daysBefore]
 * days away".
 *
 * The identity of an occurrence is its [key], and the key deliberately includes the target date.
 * That is what makes a renewed subscription or a re-insured car produce *new* reminders instead of
 * being silenced by the ones already delivered for the previous cycle.
 */
data class ReminderOccurrence(
    val sourceType: ReminderSourceType,
    val sourceId: String,
    val kind: ReminderKind,
    val targetDate: LocalDate,
    val daysBefore: Int,
) {
    /** The day this reminder is meant to be delivered on. */
    val fireDate: LocalDate = targetDate.minusDays(daysBefore.toLong())

    /**
     * Deterministic identity, e.g. `OBLIGATION:123:2026-09-20:7`.
     *
     * Deterministic because deduplication has to survive a process restart: the worker can run
     * several times a day and must recognise a reminder it has already delivered without keeping
     * anything in memory.
     */
    val key: String = "${sourceType.name}:$sourceId:$targetDate:$daysBefore"

    /**
     * Notification id derived from the key.
     *
     * Every occurrence gets its own id so that three reminders landing on the same day sit side by
     * side in the drawer instead of overwriting one another, while a redelivery of the *same*
     * reminder would replace itself rather than pile up.
     */
    val notificationId: Int = key.hashCode()

    fun daysUntilFireDate(today: LocalDate): Long = ChronoUnit.DAYS.between(today, fireDate)
}

/**
 * A reminder that is ready to be shown, with everything the notification needs.
 *
 * The domain decides what to say, about which record, and why; how it is rendered - channel, icon,
 * grouping, intent - is left entirely to the platform layer.
 */
data class ReminderNotification(
    val occurrence: ReminderOccurrence,
    val title: String,
    val amount: Money,
    val currency: Currency,
    val billingPeriod: BillingPeriod?,
)
