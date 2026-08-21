package com.griff.subscriptions.domain.validation

import com.griff.subscriptions.domain.model.Currency
import com.griff.subscriptions.domain.model.ManagementUrl
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.model.SubscriptionName

/** Turns raw form values into a [ValidatedSubscriptionInput] or a list of field errors. */
object SubscriptionInputValidator {

    fun validate(input: SubscriptionInput): SubscriptionInputValidation {
        val errors = mutableListOf<SubscriptionInputError>()

        val providerId = input.providerId
        if (providerId == null) errors += SubscriptionInputError.ProviderMissing

        val trimmedName = input.name.trim()
        val name: SubscriptionName? = when {
            trimmedName.isEmpty() -> {
                errors += SubscriptionInputError.NameMissing
                null
            }

            trimmedName.length > SubscriptionName.MAX_LENGTH -> {
                errors += SubscriptionInputError.NameTooLong
                null
            }

            else -> SubscriptionName.of(trimmedName)
        }

        val price = when (val result = PriceParser.parse(input.price)) {
            is PriceParseResult.Success -> result.money
            is PriceParseResult.Failure -> {
                errors += SubscriptionInputError.Price(result.error)
                null
            }
        }

        val rawUrl = input.managementUrl.trim()
        val managementUrl = when {
            rawUrl.isEmpty() -> null
            else -> ManagementUrl.ofOrNull(rawUrl).also {
                if (it == null) errors += SubscriptionInputError.ManagementUrlInvalid
            }
        }

        if (errors.isNotEmpty() || providerId == null || name == null || price == null) {
            return SubscriptionInputValidation.Invalid(errors)
        }

        return SubscriptionInputValidation.Valid(
            ValidatedSubscriptionInput(
                providerId = providerId,
                name = name,
                // Only a custom service carries its own category; a catalog entry would otherwise
                // end up with a stored copy that can drift away from the catalog.
                categoryOverride = when {
                    !providerId.isOther -> null
                    else -> input.category ?: ProviderCategory.OTHER
                },
                price = price,
                currency = Currency.Default,
                billingPeriod = input.billingPeriod,
                managementUrl = managementUrl,
                nextBillingDate = input.nextBillingDate,
                remindersEnabled = input.remindersEnabled,
            ),
        )
    }
}
