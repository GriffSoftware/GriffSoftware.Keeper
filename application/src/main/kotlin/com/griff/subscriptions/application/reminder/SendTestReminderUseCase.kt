package com.griff.subscriptions.application.reminder

import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.reminder.ReminderKind
import com.griff.subscriptions.domain.reminder.ReminderNotification
import com.griff.subscriptions.domain.reminder.ReminderOccurrence
import com.griff.subscriptions.domain.reminder.ReminderPublisher
import com.griff.subscriptions.domain.reminder.ReminderSourceType
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Publishes one reminder immediately so a build can be checked without waiting days.
 *
 * It goes through the real publisher, so what it proves is the whole chain - channel, icon, copy,
 * permission and the deep link - and not a mock of it. When the user has a record it uses that
 * record, which makes the deep link verifiable too; the debug build is the only caller.
 */
class SendTestReminderUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val publisher: ReminderPublisher,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke() {
        val subscription = subscriptions.observeAll().first().firstOrNull()
        val obligation = obligations.observeAll().first().firstOrNull()
        val today = clock.today()

        val notification = when {
            subscription != null -> ReminderNotification(
                occurrence = ReminderOccurrence(
                    sourceType = ReminderSourceType.SUBSCRIPTION,
                    sourceId = subscription.id.value,
                    kind = ReminderKind.SUBSCRIPTION_RENEWAL,
                    targetDate = subscription.nextBillingDate ?: today.plusDays(TEST_DAYS_BEFORE),
                    daysBefore = TEST_DAYS_BEFORE.toInt(),
                ),
                title = subscription.name.value,
                amount = subscription.price,
                currency = subscription.currency,
                billingPeriod = subscription.billingPeriod,
            )

            obligation != null -> ReminderNotification(
                occurrence = ReminderOccurrence(
                    sourceType = ReminderSourceType.OBLIGATION,
                    sourceId = obligation.id.value,
                    kind = if (obligation.category.expires) {
                        ReminderKind.INSURANCE_EXPIRY
                    } else {
                        ReminderKind.PAYMENT_DUE
                    },
                    targetDate = obligation.deadline ?: today.plusDays(TEST_DAYS_BEFORE),
                    daysBefore = TEST_DAYS_BEFORE.toInt(),
                ),
                title = obligation.name.value,
                amount = obligation.amount,
                currency = obligation.currency,
                billingPeriod = null,
            )

            // No records yet: a placeholder still proves the channel, the icon and the permission.
            else -> ReminderNotification(
                occurrence = ReminderOccurrence(
                    sourceType = ReminderSourceType.SUBSCRIPTION,
                    sourceId = "test",
                    kind = ReminderKind.SUBSCRIPTION_RENEWAL,
                    targetDate = today.plusDays(TEST_DAYS_BEFORE),
                    daysBefore = TEST_DAYS_BEFORE.toInt(),
                ),
                title = "Griff",
                amount = Money.ofUnits(59, 99),
                currency = Currency.Default,
                billingPeriod = null,
            )
        }

        publisher.publish(notification)
    }

    private companion object {
        const val TEST_DAYS_BEFORE = 7L
    }
}
