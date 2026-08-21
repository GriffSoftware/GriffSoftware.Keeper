package com.griff.subscriptions.domain.reminder

import java.time.LocalDate

/**
 * Turns candidates into the concrete reminders that a given day implies.
 *
 * Pure functions over dates: no storage, no Android, no scheduling. The engine can therefore be
 * re-evaluated from scratch whenever it likes, which is what makes edited, renewed and deleted
 * records behave correctly without any reminder state that could go stale.
 */
object ReminderPlanner {

    /** Every reminder this candidate would ever produce for its current target date. */
    fun occurrences(candidate: ReminderCandidate): List<ReminderOccurrence> =
        candidate.schedule.rules.map { rule ->
            ReminderOccurrence(
                sourceType = candidate.sourceType,
                sourceId = candidate.sourceId,
                kind = candidate.kind,
                targetDate = candidate.targetDate,
                daysBefore = rule.daysBefore,
            )
        }

    /**
     * The reminders that fall on [today] - never the ones that fell earlier.
     *
     * A record entered after its own deadline, or an app installed after a reminder was due, must
     * not produce a burst of historical notifications; the user is told about the state of the
     * record in the UI instead.
     */
    fun dueOn(candidate: ReminderCandidate, today: LocalDate): List<ReminderOccurrence> =
        occurrences(candidate).filter { it.fireDate == today }

    /**
     * The next reminder the user can still expect, or `null` when there is none left.
     *
     * Occurrences that have already been delivered are skipped rather than shown again: after the
     * "30 days" notice has gone out, the honest answer to "when will I hear next?" is the 7 day one.
     */
    fun nextUpcoming(
        candidate: ReminderCandidate,
        today: LocalDate,
        isDelivered: (String) -> Boolean = { false },
    ): ReminderOccurrence? = occurrences(candidate)
        .filter { it.fireDate >= today && !isDelivered(it.key) }
        .minByOrNull { it.fireDate }
}
