package com.griff.keeper.application.subscription

import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.repository.SubscriptionRepository
import com.griff.keeper.domain.time.ClockProvider
import com.griff.keeper.domain.validation.ValidatedSubscriptionInput
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
                categoryOverride = input.categoryOverride,
                price = input.price,
                currency = input.currency,
                billingPeriod = input.billingPeriod,
                managementUrl = input.managementUrl,
                nextBillingDate = input.nextBillingDate,
                remindersEnabled = input.remindersEnabled,
                updatedAt = clock.now(),
            ),
        )
        return Result.success(Unit)
    }
}

class SubscriptionNotFoundException(id: SubscriptionId) :
    NoSuchElementException("Subscription $id does not exist")
