package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.domain.validation.ObligationInput
import com.griff.subscriptions.domain.validation.ObligationInputValidation
import com.griff.subscriptions.domain.validation.ObligationInputValidator
import javax.inject.Inject

/** Exposes domain form validation to the presentation layer. */
class ValidateObligationInputUseCase @Inject constructor() {
    operator fun invoke(input: ObligationInput): ObligationInputValidation =
        ObligationInputValidator.validate(input)
}
