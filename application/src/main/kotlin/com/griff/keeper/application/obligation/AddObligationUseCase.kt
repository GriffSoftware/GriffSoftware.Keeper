package com.griff.keeper.application.obligation

import com.griff.keeper.domain.id.ObligationIdGenerator
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.ObligationId
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.time.ClockProvider
import com.griff.keeper.domain.validation.ValidatedObligationInput
import javax.inject.Inject

/** Creates a new obligation from already validated form input. */
class AddObligationUseCase @Inject constructor(
    private val repository: ObligationRepository,
    private val idGenerator: ObligationIdGenerator,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(input: ValidatedObligationInput): ObligationId {
        val now = clock.now()
        val obligation = Obligation(
            id = idGenerator.next(),
            name = input.name,
            category = input.category,
            amount = input.amount,
            currency = input.currency,
            payment = input.payment,
            dueDate = input.dueDate,
            validUntil = input.validUntil,
            notes = input.notes,
            remindersEnabled = input.remindersEnabled,
            createdAt = now,
            updatedAt = now,
        )
        repository.add(obligation)
        return obligation.id
    }
}
