package com.griff.subscriptions.domain.validation

import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.ManagementUrl
import com.griff.subscriptions.domain.model.Money
import com.griff.subscriptions.domain.model.ProviderId
import com.griff.subscriptions.domain.model.SubscriptionName
import java.time.LocalDate

/** Raw form values as typed by the user. */
data class SubscriptionInput(
    val providerId: ProviderId?,
    val name: String,
    val price: String,
    val billingPeriod: BillingPeriod,
    val managementUrl: String,
    val nextBillingDate: LocalDate?,
)

/**
 * Form values that passed domain validation.
 *
 * Use cases only accept this type, so unvalidated input cannot reach the repository.
 */
@ConsistentCopyVisibility
data class ValidatedSubscriptionInput internal constructor(
    val providerId: ProviderId,
    val name: SubscriptionName,
    val price: Money,
    val currency: Currency,
    val billingPeriod: BillingPeriod,
    val managementUrl: ManagementUrl?,
    val nextBillingDate: LocalDate?,
)

enum class SubscriptionField {
    PROVIDER,
    NAME,
    PRICE,
    MANAGEMENT_URL,
}

sealed interface SubscriptionInputError {
    val field: SubscriptionField

    data object ProviderMissing : SubscriptionInputError {
        override val field = SubscriptionField.PROVIDER
    }

    data object NameMissing : SubscriptionInputError {
        override val field = SubscriptionField.NAME
    }

    data object NameTooLong : SubscriptionInputError {
        override val field = SubscriptionField.NAME
    }

    data class Price(val error: PriceError) : SubscriptionInputError {
        override val field = SubscriptionField.PRICE
    }

    data object ManagementUrlInvalid : SubscriptionInputError {
        override val field = SubscriptionField.MANAGEMENT_URL
    }
}

sealed interface SubscriptionInputValidation {
    data class Valid(val input: ValidatedSubscriptionInput) : SubscriptionInputValidation
    data class Invalid(val errors: List<SubscriptionInputError>) : SubscriptionInputValidation
}
