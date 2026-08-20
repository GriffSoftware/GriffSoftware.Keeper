package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.id.SubscriptionIdGenerator
import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.domain.validation.ValidatedSubscriptionInput
import javax.inject.Inject

/** Creates a new subscription from already validated form input. */
class AddSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val idGenerator: SubscriptionIdGenerator,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(input: ValidatedSubscriptionInput): SubscriptionId {
        val now = clock.now()
        val subscription = Subscription(
            id = idGenerator.next(),
            providerId = input.providerId,
            name = input.name,
            categoryOverride = input.categoryOverride,
            price = input.price,
            currency = input.currency,
            billingPeriod = input.billingPeriod,
            managementUrl = input.managementUrl,
            nextBillingDate = input.nextBillingDate,
            createdAt = now,
            updatedAt = now,
        )
        repository.add(subscription)
        return subscription.id
    }
}
