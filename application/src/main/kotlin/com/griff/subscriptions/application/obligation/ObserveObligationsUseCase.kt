package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.repository.ObligationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams all stored obligations. */
class ObserveObligationsUseCase @Inject constructor(
    private val repository: ObligationRepository,
) {
    operator fun invoke(): Flow<List<Obligation>> = repository.observeAll()
}
