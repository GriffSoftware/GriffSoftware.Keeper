package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.application.provider.GetProviderUseCase
import com.griff.subscriptions.application.subscription.GetSubscriptionCategoryUseCase
import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.reminder.ReminderCandidate
import com.griff.subscriptions.domain.reminder.ReminderCandidates
import com.griff.subscriptions.domain.reminder.ReminderDefaults
import com.griff.subscriptions.domain.reminder.ReminderPlanner
import com.griff.subscriptions.domain.reminder.ReminderSource
import java.time.LocalDate
import javax.inject.Inject

/**
 * Builds the reminder view of a record, for the dashboard and for the details screens alike.
 *
 * One factory rather than one per screen: "when will I next hear about this?" has to give the same
 * answer everywhere, and the rules behind it (which date counts, which reminders are already spent)
 * are subtle enough that a second implementation would quietly disagree.
 */
class ReminderItemFactory @Inject constructor(
    private val getProvider: GetProviderUseCase,
    private val getCategory: GetSubscriptionCategoryUseCase,
) {

    fun candidateOf(subscription: Subscription, defaults: ReminderDefaults): ReminderCandidate? {
        val provider = getProvider(subscription.providerId)
        return ReminderCandidates.of(
            subscription = subscription,
            // A custom entry has no catalog artwork, so the name drives the monogram - the same
            // rule the lists and the details screen already use.
            logoKey = if (provider.isOther) subscription.name.value else provider.logoKey,
            category = getCategory(subscription),
            defaults = defaults,
        )
    }

    fun candidateOf(obligation: Obligation, defaults: ReminderDefaults): ReminderCandidate? =
        ReminderCandidates.of(obligation, defaults)

    fun itemOf(
        subscription: Subscription,
        defaults: ReminderDefaults,
        today: LocalDate,
        deliveredKeys: Set<String>,
    ): ReminderItem {
        val candidate = candidateOf(subscription, defaults)
        val provider = getProvider(subscription.providerId)
        return item(
            candidate = candidate,
            fallbackSource = ReminderSource.Subscription(
                id = subscription.id.value,
                logoKey = if (provider.isOther) subscription.name.value else provider.logoKey,
                category = getCategory(subscription),
                billingPeriod = subscription.billingPeriod,
            ),
            title = subscription.name.value,
            amount = subscription.price,
            currency = subscription.currency,
            remindersEnabled = subscription.remindersEnabled,
            today = today,
            deliveredKeys = deliveredKeys,
        )
    }

    fun itemOf(
        obligation: Obligation,
        defaults: ReminderDefaults,
        today: LocalDate,
        deliveredKeys: Set<String>,
    ): ReminderItem = item(
        candidate = candidateOf(obligation, defaults),
        fallbackSource = ReminderSource.Obligation(
            id = obligation.id.value,
            category = obligation.category,
        ),
        title = obligation.name.value,
        amount = obligation.amount,
        currency = obligation.currency,
        remindersEnabled = obligation.remindersEnabled,
        today = today,
        deliveredKeys = deliveredKeys,
    )

    fun stateOf(item: ReminderItem, globalEnabled: Boolean): ItemReminderState = ItemReminderState(
        globalEnabled = globalEnabled,
        itemEnabled = item.remindersEnabled,
        kind = item.kind,
        targetDate = item.targetDate,
        nextReminder = item.nextReminder,
        status = item.status,
    )

    /**
     * A record with no candidate still belongs on the screen.
     *
     * "Netflix has no renewal date" is exactly the kind of thing the user came to the reminders
     * screen to find out; dropping such records would make the list look complete when it is not.
     */
    private fun item(
        candidate: ReminderCandidate?,
        fallbackSource: ReminderSource,
        title: String,
        amount: Money,
        currency: Currency,
        remindersEnabled: Boolean,
        today: LocalDate,
        deliveredKeys: Set<String>,
    ): ReminderItem {
        val next = candidate
            ?.takeIf { remindersEnabled }
            ?.let { ReminderPlanner.nextUpcoming(it, today) { key -> key in deliveredKeys } }

        val status = when {
            candidate == null -> ReminderItemStatus.NO_DATE
            !remindersEnabled -> ReminderItemStatus.DISABLED
            next == null -> ReminderItemStatus.PASSED
            else -> ReminderItemStatus.SCHEDULED
        }

        return ReminderItem(
            source = candidate?.source ?: fallbackSource,
            title = title,
            amount = amount,
            currency = currency,
            remindersEnabled = remindersEnabled,
            kind = candidate?.kind,
            targetDate = candidate?.targetDate,
            nextReminder = next,
            status = status,
        )
    }
}
