package com.griff.subscriptions.domain.repository

import com.griff.subscriptions.domain.model.Subscription
import com.griff.subscriptions.domain.model.SubscriptionId
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for subscriptions. Implemented by the infrastructure layer.
 *
 * Reads are exposed as [Flow] so that every screen observes a single source of truth.
 */
interface SubscriptionRepository {

    fun observeAll(): Flow<List<Subscription>>

    fun observeById(id: SubscriptionId): Flow<Subscription?>

    suspend fun findById(id: SubscriptionId): Subscription?

    suspend fun add(subscription: Subscription)

    suspend fun update(subscription: Subscription)

    suspend fun delete(id: SubscriptionId)
}
