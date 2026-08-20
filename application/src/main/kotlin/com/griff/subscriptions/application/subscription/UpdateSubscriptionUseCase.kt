package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.domain.validation.ValidatedSubscriptionInput
import javax.inject.Inject

/** Applies validated form input to an existing subscription. */
class UpdateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        id: SubscriptionId,
        input: ValidatedSubscriptionInput,
    ): Result<Unit> {
        val existing = repository.findById(id)
            ?: return Result.failure(SubscriptionNotFoundException(id))

        repository.update(
            existing.copy(
                providerId = input.providerId,
                name = input.name,
                price = input.price,
                currency = input.currency,
                billingPeriod = input.billingPeriod,
                managementUrl = input.managementUrl,
                nextBillingDate = input.nextBillingDate,
                updatedAt = clock.now(),
            ),
        )
        return Result.success(Unit)
    }
}

class SubscriptionNotFoundException(id: SubscriptionId) :
    NoSuchElementException("Subscription $id does not exist")
