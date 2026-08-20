package com.griff.subscriptions.domain.repository

import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationId
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for obligations. Implemented by the infrastructure layer.
 *
 * Mirrors [SubscriptionRepository]: reads are exposed as [Flow] so every screen observes a single
 * source of truth.
 */
interface ObligationRepository {

    fun observeAll(): Flow<List<Obligation>>

    fun observeById(id: ObligationId): Flow<Obligation?>

    suspend fun findById(id: ObligationId): Obligation?

    suspend fun add(obligation: Obligation)

    suspend fun update(obligation: Obligation)

    suspend fun delete(id: ObligationId)
}
