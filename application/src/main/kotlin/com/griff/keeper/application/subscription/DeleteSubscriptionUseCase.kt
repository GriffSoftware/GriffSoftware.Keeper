package com.griff.keeper.application.subscription

import com.griff.keeper.domain.model.SubscriptionId
import com.griff.keeper.domain.repository.SubscriptionRepository
import javax.inject.Inject

/** Removes a subscription. Deleting an unknown id is a no-op. */
class DeleteSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(id: SubscriptionId) = repository.delete(id)
}
