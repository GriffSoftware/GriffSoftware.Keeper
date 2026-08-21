package com.griff.keeper.application.obligation

import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationId
import com.griff.keeper.domain.repository.ObligationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams a single obligation; emits `null` once the record no longer exists. */
class ObserveObligationUseCase @Inject constructor(
    private val repository: ObligationRepository,
) {
    operator fun invoke(id: ObligationId): Flow<Obligation?> = repository.observeById(id)
}
