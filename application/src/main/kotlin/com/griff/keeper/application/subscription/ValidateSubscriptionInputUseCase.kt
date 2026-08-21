package com.griff.keeper.application.subscription

import com.griff.keeper.domain.validation.SubscriptionInput
import com.griff.keeper.domain.validation.SubscriptionInputValidation
import com.griff.keeper.domain.validation.SubscriptionInputValidator
import javax.inject.Inject

/** Exposes domain form validation to the presentation layer. */
class ValidateSubscriptionInputUseCase @Inject constructor() {
    operator fun invoke(input: SubscriptionInput): SubscriptionInputValidation =
        SubscriptionInputValidator.validate(input)
}
