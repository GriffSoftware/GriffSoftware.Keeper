package com.griff.keeper.application.subscription

import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.repository.SubscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams all stored subscriptions, ordered by name. */
class ObserveSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<List<Subscription>> = repository.observeAll()
}
