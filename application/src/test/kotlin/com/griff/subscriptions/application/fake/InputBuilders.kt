package com.griff.subscriptions.application.fake

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.validation.SubscriptionInput
import com.griff.subscriptions.domain.validation.SubscriptionInputValidation
import com.griff.subscriptions.domain.validation.SubscriptionInputValidator
import com.griff.subscriptions.domain.validation.ValidatedSubscriptionInput
import java.time.LocalDate

/** Builds validated input through the real validator, so tests exercise the production path. */
fun validatedInput(
    providerId: String = "spotify",
    name: String = "Spotify",
    price: String = "34,99",
    billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    managementUrl: String = "",
    nextBillingDate: LocalDate? = null,
): ValidatedSubscriptionInput {
    val validation = SubscriptionInputValidator.validate(
        SubscriptionInput(
            providerId = ProviderId(providerId),
            name = name,
            price = price,
            billingPeriod = billingPeriod,
            managementUrl = managementUrl,
            nextBillingDate = nextBillingDate,
        ),
    )
    return (validation as SubscriptionInputValidation.Valid).input
}
