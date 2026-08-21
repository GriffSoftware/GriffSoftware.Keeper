package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.domain.model.ObligationId
import com.griff.subscriptions.domain.repository.ObligationRepository
import com.griff.subscriptions.domain.time.ClockProvider
import com.griff.subscriptions.domain.validation.ValidatedObligationInput
import javax.inject.Inject

/** Applies validated form input to an existing obligation. */
class UpdateObligationUseCase @Inject constructor(
    private val repository: ObligationRepository,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(
        id: ObligationId,
        input: ValidatedObligationInput,
    ): Result<Unit> {
        val existing = repository.findById(id)
            ?: return Result.failure(ObligationNotFoundException(id))

        repository.update(
            existing.copy(
                name = input.name,
                category = input.category,
                amount = input.amount,
                currency = input.currency,
                payment = input.payment,
                dueDate = input.dueDate,
                validUntil = input.validUntil,
                notes = input.notes,
                remindersEnabled = input.remindersEnabled,
                updatedAt = clock.now(),
            ),
        )
        return Result.success(Unit)
    }
}

class ObligationNotFoundException(id: ObligationId) :
    NoSuchElementException("Obligation $id does not exist")
