package com.griff.keeper.presentation.reminders

import com.griff.keeper.application.reminder.ReminderItem
import com.griff.keeper.application.reminder.ReminderItemStatus
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.reminder.ReminderAvailability
import com.griff.keeper.domain.reminder.ReminderDefaults
import com.griff.keeper.domain.reminder.ReminderKind
import com.griff.keeper.domain.reminder.ReminderSource
import com.griff.keeper.domain.reminder.ReminderSourceType
import com.griff.keeper.presentation.common.Tags
import com.griff.keeper.presentation.common.component.TagStyle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Which records the list is showing.
 *
 * Insurances are separated from the other charges even though both are obligations, because they
 * follow different rules - a month's notice against a week's - and that is exactly the distinction a
 * user filtering this screen has in mind.
 */
enum class ReminderFilter {
    ALL,
    SUBSCRIPTIONS,
    INSURANCE,
    FEES,
    ;

    fun matches(item: ReminderItem): Boolean {
        val obligation = item.source as? ReminderSource.Obligation
        return when (this) {
            ALL -> true
            SUBSCRIPTIONS -> item.sourceType == ReminderSourceType.SUBSCRIPTION
            INSURANCE -> obligation?.category?.expires == true
            FEES -> obligation != null && !obligation.category.expires
        }
    }
}

/** One record as a row on the reminders screen. */
data class ReminderRowUi(
    val id: String,
    val sourceType: ReminderSourceType,
    val title: String,
    val tag: TagStyle,
    /** Set for subscriptions, which are drawn with the provider's logo. */
    val logoKey: String?,
    /** Set for obligations, which are drawn with their category glyph. */
    val obligationCategory: ObligationCategory?,
    val kind: ReminderKind?,
    val targetDate: LocalDate?,
    val daysUntilTarget: Long?,
    val nextReminderDate: LocalDate?,
    val daysUntilReminder: Long?,
    val status: ReminderItemStatus,
)

data class RemindersUiState(
    val isLoading: Boolean = true,
    val globalEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    /** True once the user has refused the permission in this session, see [RemindersViewModel]. */
    val permissionDenied: Boolean = false,
    val filter: ReminderFilter = ReminderFilter.ALL,
    val upcoming: List<ReminderRowUi> = emptyList(),
    val inactive: List<ReminderRowUi> = emptyList(),
    val defaults: ReminderDefaults = ReminderDefaults.Standard,
    /** Whether the user has any records at all, as opposed to none matching the current filter. */
    val hasAnyRecords: Boolean = false,
) {
    /**
     * Whether reminders can currently reach the user at all.
     *
     * Derived through the shared domain rule rather than re-implemented here, so the screen cannot
     * claim reminders are working while the worker would disagree.
     */
    val remindersActive: Boolean
        get() = ReminderAvailability.isEffective(
            globalEnabled = globalEnabled,
            itemEnabled = true,
            systemNotificationsEnabled = systemNotificationsEnabled,
        )

    /** The system is blocking reminders the user has asked for - the one case worth a warning. */
    val isBlockedBySystem: Boolean get() = globalEnabled && !systemNotificationsEnabled

    val isEmpty: Boolean get() = upcoming.isEmpty() && inactive.isEmpty()
}

/** Maps an application item to its row, resolving the visual identity of its source. */
internal fun ReminderItem.toRow(today: LocalDate): ReminderRowUi = ReminderRowUi(
    id = id,
    sourceType = sourceType,
    title = title,
    tag = when (val source = source) {
        is ReminderSource.Subscription -> Tags.of(source.category)
        is ReminderSource.Obligation -> Tags.of(source.category)
    },
    logoKey = (source as? ReminderSource.Subscription)?.logoKey,
    obligationCategory = (source as? ReminderSource.Obligation)?.category,
    kind = kind,
    targetDate = targetDate,
    daysUntilTarget = targetDate?.let { ChronoUnit.DAYS.between(today, it) },
    nextReminderDate = nextReminder?.fireDate,
    daysUntilReminder = nextReminder?.daysUntilFireDate(today),
    status = status,
)
