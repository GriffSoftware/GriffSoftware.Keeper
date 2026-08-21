package com.griff.keeper.domain.reminder

import java.time.LocalDate

/**
 * Which module a reminder came from.
 *
 * Subscriptions and obligations stay separate aggregates - they are not merged into one table just
 * because both can produce a reminder - so the engine needs a discriminator to point back at the
 * record it is talking about.
 */
enum class ReminderSourceType {
    SUBSCRIPTION,
    OBLIGATION,
}

/**
 * *Why* a reminder exists, which is not the same question as which record it belongs to.
 *
 * The kind decides the wording of the notification and which date the user is being warned about;
 * an insurance expires, a charge falls due, a subscription renews. Keeping it explicit means the
 * notification layer never has to re-derive the intent from a pile of nullable dates.
 */
enum class ReminderKind {
    /** A subscription renews and will be charged again. */
    SUBSCRIPTION_RENEWAL,

    /** Insurance cover ends, regardless of whether the policy has been paid for. */
    INSURANCE_EXPIRY,

    /** An unpaid charge reaches its payment deadline. */
    PAYMENT_DUE,
}

/**
 * "Remind me [daysBefore] days before the date."
 *
 * A rule is deliberately relative: the target date of a record changes (a policy is renewed, a
 * subscription is billed again) and every reminder has to follow it without being rewritten.
 */
@JvmInline
value class ReminderRule(val daysBefore: Int) : Comparable<ReminderRule> {

    init {
        require(daysBefore >= 0) { "daysBefore cannot be negative, was $daysBefore" }
    }

    /** The day this rule wants the user to hear about [target]. */
    fun fireDate(target: LocalDate): LocalDate = target.minusDays(daysBefore.toLong())

    /** Ordered from the earliest warning to the latest, i.e. by descending [daysBefore]. */
    override fun compareTo(other: ReminderRule): Int = other.daysBefore.compareTo(daysBefore)
}

/**
 * The set of rules applied to one kind of record, e.g. 30 / 7 / 1 days before an insurance expires.
 *
 * Held as a sorted, de-duplicated set so that two rules can never fire on the same day for the same
 * record, and so that "the earliest warning first" is a property of the type rather than of every
 * call site.
 */
@JvmInline
value class ReminderSchedule private constructor(val rules: List<ReminderRule>) {

    val daysBefore: List<Int> get() = rules.map(ReminderRule::daysBefore)

    companion object {
        val Empty: ReminderSchedule = ReminderSchedule(emptyList())

        fun of(daysBefore: Iterable<Int>): ReminderSchedule =
            ReminderSchedule(daysBefore.map(::ReminderRule).distinct().sorted())

        fun of(vararg daysBefore: Int): ReminderSchedule = of(daysBefore.asIterable())
    }
}

/**
 * The schedule used for each kind of record until the user overrides it.
 *
 * The numbers live here, in one value object, instead of being written into a worker or a
 * composable: the engine reads them from the settings it is given, so changing them later - or
 * letting the user change them - touches no logic at all.
 */
data class ReminderDefaults(
    val insurance: ReminderSchedule,
    val payment: ReminderSchedule,
    val subscription: ReminderSchedule,
) {
    fun scheduleFor(kind: ReminderKind): ReminderSchedule = when (kind) {
        ReminderKind.INSURANCE_EXPIRY -> insurance
        ReminderKind.PAYMENT_DUE -> payment
        ReminderKind.SUBSCRIPTION_RENEWAL -> subscription
    }

    companion object {
        /**
         * A policy is worth a month's notice - renewing one takes time and comparison; a payment or
         * a renewal is worth a week, which is enough to move money or cancel.
         */
        val Standard: ReminderDefaults = ReminderDefaults(
            insurance = ReminderSchedule.of(30, 7, 1),
            payment = ReminderSchedule.of(7, 1),
            subscription = ReminderSchedule.of(7, 1),
        )
    }
}

/**
 * Reminder configuration that belongs to the app rather than to a single record.
 *
 * The global switch is a *master* switch: it decides whether anything is delivered at all and never
 * touches the per-record flags, so turning it back on restores exactly what the user had.
 */
data class ReminderSettings(
    val globalEnabled: Boolean,
    val defaults: ReminderDefaults,
) {
    companion object {
        val Default: ReminderSettings = ReminderSettings(
            globalEnabled = true,
            defaults = ReminderDefaults.Standard,
        )
    }
}

/**
 * Whether a reminder can actually reach the user.
 *
 * Three independent switches have to agree, and they mean three different things: the app-wide
 * preference, the record's own flag, and whether Android lets the app post notifications at all.
 * The conjunction lives here so that the worker, the reminders screen and the details screens
 * cannot drift apart on what "enabled" means.
 */
object ReminderAvailability {

    fun isEffective(
        globalEnabled: Boolean,
        itemEnabled: Boolean,
        systemNotificationsEnabled: Boolean,
    ): Boolean = globalEnabled && itemEnabled && systemNotificationsEnabled
}
