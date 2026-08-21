package com.griff.keeper.application.obligation

import com.griff.keeper.domain.model.ObligationId
import com.griff.keeper.domain.repository.ObligationRepository
import javax.inject.Inject

/** Removes an obligation. Deleting an unknown id is a no-op. */
class DeleteObligationUseCase @Inject constructor(
    private val repository: ObligationRepository,
) {
    suspend operator fun invoke(id: ObligationId) = repository.delete(id)
}
