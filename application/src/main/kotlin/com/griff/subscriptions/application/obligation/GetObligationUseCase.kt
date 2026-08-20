package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.domain.model.Obligation
import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.repository.ObligationRepository
import javax.inject.Inject

/** One-shot read, used to pre-fill the edit form. */
class GetObligationUseCase @Inject constructor(
    private val repository: ObligationRepository,
) {
    suspend operator fun invoke(id: ObligationId): Obligation? = repository.findById(id)
}
