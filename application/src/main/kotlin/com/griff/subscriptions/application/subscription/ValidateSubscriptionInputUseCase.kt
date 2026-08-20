package com.griff.subscriptions.application.subscription

import com.griff.subscriptions.domain.validation.SubscriptionInput
import com.griff.subscriptions.domain.validation.SubscriptionInputValidation
import com.griff.subscriptions.domain.validation.SubscriptionInputValidator
import javax.inject.Inject

/** Exposes domain form validation to the presentation layer. */
class ValidateSubscriptionInputUseCase @Inject constructor() {
    operator fun invoke(input: SubscriptionInput): SubscriptionInputValidation =
        SubscriptionInputValidator.validate(input)
}
