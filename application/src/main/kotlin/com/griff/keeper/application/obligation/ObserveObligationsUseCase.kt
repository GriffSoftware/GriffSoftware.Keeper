package com.griff.keeper.application.obligation

import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.repository.ObligationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams all stored obligations. */
class ObserveObligationsUseCase @Inject constructor(
    private val repository: ObligationRepository,
) {
    operator fun invoke(): Flow<List<Obligation>> = repository.observeAll()
}
