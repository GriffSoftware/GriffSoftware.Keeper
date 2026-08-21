package com.griff.keeper.domain.reminder

import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.Subscription
import java.time.LocalDate

/**
 * What a record looks like to the reminder engine.
 *
 * Subscriptions and obligations answer the same three questions - what is it called, which date
 * matters, how much is it - and the engine only ever needs those. Flattening both into one
 * candidate is what makes a single reminder engine possible without merging two domain models that
 * have nothing else in common.
 */
data class ReminderCandidate(
    val source: ReminderSource,
    val title: String,
    val kind: ReminderKind,
    val targetDate: LocalDate,
    val amount: Money,
    val currency: Currency,
    val schedule: ReminderSchedule,
    /** The record's own switch. The global switch and the system permission are applied later. */
    val remindersEnabled: Boolean,
) {
    val sourceType: ReminderSourceType get() = source.type

    val sourceId: String get() = source.id
}

/**
 * The record a reminder points back at, together with what the UI needs to draw it.
 *
 * Modelled as a sealed type rather than a pair of nullable fields: a reminder comes from exactly one
 * of the two modules, and the icon, the tag and the destination it opens all follow from that.
 */
sealed interface ReminderSource {

    val id: String

    val type: ReminderSourceType

    data class Subscription(
        override val id: String,
        val logoKey: String,
        val category: ProviderCategory,
        val billingPeriod: BillingPeriod,
    ) : ReminderSource {
        override val type: ReminderSourceType get() = ReminderSourceType.SUBSCRIPTION
    }

    data class Obligation(
        override val id: String,
        val category: ObligationCategory,
    ) : ReminderSource {
        override val type: ReminderSourceType get() = ReminderSourceType.OBLIGATION
    }
}

/**
 * Turns records into reminder candidates, which is where the rules about *which date matters* live.
 *
 * This is the one place that knows an insurance is about its expiry even after it has been paid,
 * while a tax is about its deadline and stops mattering the moment it is settled. Spread across the
 * worker as a series of `if`s the same knowledge would be impossible to test and easy to contradict.
 */
object ReminderCandidates {

    /**
     * A subscription reminds about its next charge.
     *
     * Without [com.griff.keeper.domain.model.Subscription.nextBillingDate] there is nothing
     * to count down to, so no reminder can be planned - the UI says so instead of pretending.
     */
    fun of(
        subscription: Subscription,
        logoKey: String,
        category: ProviderCategory,
        defaults: ReminderDefaults,
    ): ReminderCandidate? {
        val target = subscription.nextBillingDate ?: return null
        return ReminderCandidate(
            source = ReminderSource.Subscription(
                id = subscription.id.value,
                logoKey = logoKey,
                category = category,
                billingPeriod = subscription.billingPeriod,
            ),
            title = subscription.name.value,
            kind = ReminderKind.SUBSCRIPTION_RENEWAL,
            targetDate = target,
            amount = subscription.price,
            currency = subscription.currency,
            schedule = defaults.subscription,
            remindersEnabled = subscription.remindersEnabled,
        )
    }

    /**
     * An obligation reminds about whichever of its dates is still ahead of the user.
     *
     * Cover that runs out is the stronger signal and survives payment: a policy paid in March still
     * expires in September, and that is the date worth a reminder. A charge that is merely due, on
     * the other hand, is finished business once it has been marked as paid - reminding about it
     * again would be wrong rather than merely noisy.
     */
    fun of(obligation: Obligation, defaults: ReminderDefaults): ReminderCandidate? {
        val expiry = obligation.validUntil?.takeIf { obligation.category.expires }
        if (expiry != null) {
            return obligation.candidate(ReminderKind.INSURANCE_EXPIRY, expiry, defaults.insurance)
        }

        if (obligation.isPaid) return null

        val due = obligation.dueDate ?: obligation.validUntil ?: return null
        return obligation.candidate(ReminderKind.PAYMENT_DUE, due, defaults.payment)
    }

    private fun Obligation.candidate(
        kind: ReminderKind,
        target: LocalDate,
        schedule: ReminderSchedule,
    ) = ReminderCandidate(
        source = ReminderSource.Obligation(id = id.value, category = category),
        title = name.value,
        kind = kind,
        targetDate = target,
        amount = amount,
        currency = currency,
        schedule = schedule,
        remindersEnabled = remindersEnabled,
    )
}
