package com.griff.keeper.application.reminder

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.reminder.ReminderDefaults
import com.griff.keeper.domain.reminder.ReminderKind
import com.griff.keeper.domain.reminder.ReminderOccurrence
import com.griff.keeper.domain.reminder.ReminderSource
import com.griff.keeper.domain.reminder.ReminderSourceType
import java.time.LocalDate

/**
 * Why a record is, or is not, going to produce a reminder.
 *
 * The reminders screen has to answer "will I be told about this?" honestly, and "no" has several
 * different reasons that call for different words - and, in two of the four cases, for a different
 * action from the user.
 */
enum class ReminderItemStatus {
    /** A reminder is planned and its date is known. */
    SCHEDULED,

    /** The user switched reminders off for this record. */
    DISABLED,

    /** Nothing to count down to: no renewal date, no deadline, or the charge is already settled. */
    NO_DATE,

    /** The date exists but every reminder for it is in the past. */
    PASSED,
}

/**
 * One record as the reminders screen sees it.
 *
 * Deliberately not a second source of truth: it is derived on every emission from the records
 * themselves, so an edited date or a deleted record is reflected without any reminder state to
 * clean up.
 */
data class ReminderItem(
    val source: ReminderSource,
    val title: String,
    val amount: Money,
    val currency: Currency,
    val remindersEnabled: Boolean,
    val kind: ReminderKind?,
    val targetDate: LocalDate?,
    val nextReminder: ReminderOccurrence?,
    val status: ReminderItemStatus,
) {
    val id: String get() = source.id

    val sourceType: ReminderSourceType get() = source.type
}

/**
 * Everything the reminders screen needs in one emission.
 *
 * The system permission is *not* part of it: it is not application state but a property of the
 * device that can change while the screen is open, so the presentation layer observes it separately
 * and combines the two through
 * [com.griff.keeper.domain.reminder.ReminderAvailability].
 */
data class ReminderDashboard(
    val globalEnabled: Boolean,
    val defaults: ReminderDefaults,
    val items: List<ReminderItem>,
) {
    /** Records with a reminder still ahead of them, most urgent first. */
    val upcoming: List<ReminderItem>
        get() = items.filter { it.status == ReminderItemStatus.SCHEDULED }

    /** Everything the engine currently has nothing to say about, and why. */
    val inactive: List<ReminderItem>
        get() = items.filterNot { it.status == ReminderItemStatus.SCHEDULED }

    companion object {
        val Empty: ReminderDashboard = ReminderDashboard(
            globalEnabled = true,
            defaults = ReminderDefaults.Standard,
            items = emptyList(),
        )
    }
}

/** What a details screen shows about the reminders of the single record it is displaying. */
data class ItemReminderState(
    val globalEnabled: Boolean,
    val itemEnabled: Boolean,
    val kind: ReminderKind?,
    val targetDate: LocalDate?,
    val nextReminder: ReminderOccurrence?,
    val status: ReminderItemStatus,
)
