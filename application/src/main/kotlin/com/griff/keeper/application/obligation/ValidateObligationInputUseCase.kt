package com.griff.keeper.application.obligation

import com.griff.keeper.domain.validation.ObligationInput
import com.griff.keeper.domain.validation.ObligationInputValidation
import com.griff.keeper.domain.validation.ObligationInputValidator
import javax.inject.Inject

/** Exposes domain form validation to the presentation layer. */
class ValidateObligationInputUseCase @Inject constructor() {
    operator fun invoke(input: ObligationInput): ObligationInputValidation =
        ObligationInputValidator.validate(input)
}
