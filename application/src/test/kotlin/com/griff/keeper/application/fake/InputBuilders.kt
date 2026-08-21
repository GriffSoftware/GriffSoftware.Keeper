package com.griff.keeper.application.fake

import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.model.ProviderId
import com.griff.keeper.domain.validation.ObligationInput
import com.griff.keeper.domain.validation.ObligationInputValidation
import com.griff.keeper.domain.validation.ObligationInputValidator
import com.griff.keeper.domain.validation.SubscriptionInput
import com.griff.keeper.domain.validation.SubscriptionInputValidation
import com.griff.keeper.domain.validation.SubscriptionInputValidator
import com.griff.keeper.domain.validation.ValidatedObligationInput
import com.griff.keeper.domain.validation.ValidatedSubscriptionInput
import java.time.LocalDate

/** Builds validated input through the real validator, so tests exercise the production path. */
fun validatedInput(
    providerId: String = "spotify",
    name: String = "Spotify",
    category: ProviderCategory? = null,
    price: String = "34,99",
    billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    managementUrl: String = "",
    nextBillingDate: LocalDate? = null,
    remindersEnabled: Boolean = true,
): ValidatedSubscriptionInput {
    val validation = SubscriptionInputValidator.validate(
        SubscriptionInput(
            providerId = ProviderId(providerId),
            name = name,
            category = category,
            price = price,
            billingPeriod = billingPeriod,
            managementUrl = managementUrl,
            nextBillingDate = nextBillingDate,
            remindersEnabled = remindersEnabled,
        ),
    )
    return (validation as SubscriptionInputValidation.Valid).input
}

/** Builds validated obligation input through the real validator. */
fun validatedObligationInput(
    name: String = "OC Ford",
    category: ObligationCategory = ObligationCategory.VEHICLE_INSURANCE,
    amount: String = "1240,00",
    paymentStatus: PaymentStatus = PaymentStatus.PAID,
    paymentDate: LocalDate? = LocalDate.of(2026, 3, 12),
    dueDate: LocalDate? = null,
    validUntil: LocalDate? = LocalDate.of(2027, 3, 11),
    notes: String = "",
    remindersEnabled: Boolean = true,
): ValidatedObligationInput {
    val validation = ObligationInputValidator.validate(
        ObligationInput(
            name = name,
            category = category,
            amount = amount,
            paymentStatus = paymentStatus,
            paymentDate = paymentDate,
            dueDate = dueDate,
            validUntil = validUntil,
            notes = notes,
            remindersEnabled = remindersEnabled,
        ),
    )
    return (validation as ObligationInputValidation.Valid).input
}
