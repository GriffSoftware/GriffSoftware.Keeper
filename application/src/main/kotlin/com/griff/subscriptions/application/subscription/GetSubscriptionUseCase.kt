package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import javax.inject.Inject

/** One-shot read, used to pre-fill the edit form. */
class GetSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(id: SubscriptionId): Subscription? = repository.findById(id)
}
