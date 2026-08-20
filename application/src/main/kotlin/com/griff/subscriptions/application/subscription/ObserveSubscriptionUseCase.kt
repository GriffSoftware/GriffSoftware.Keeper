package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import com.griff.subscriptions.domain.repository.SubscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams a single subscription; emits `null` once the record no longer exists. */
class ObserveSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(id: SubscriptionId): Flow<Subscription?> = repository.observeById(id)
}
