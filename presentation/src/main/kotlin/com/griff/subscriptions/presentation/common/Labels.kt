package com.griff.subscriptions.presentation.common

import androidx.annotation.StringRes
import com.griff.subscriptions.domain.model.BillingPeriod
import com.griff.subscriptions.domain.model.ProviderCategory
import com.griff.subscriptions.domain.statistics.StatisticsPeriod
import com.griff.subscriptions.domain.validation.PriceError
import com.griff.subscriptions.domain.validation.SubscriptionInputError
import com.griff.subscriptions.presentation.R

/** Maps domain enums and errors to string resources. Keeps localized copy out of ViewModels. */
internal object Labels {

    @StringRes
    fun billingPeriodShort(period: BillingPeriod): Int = when (period) {
        BillingPeriod.MONTHLY -> R.string.billing_period_monthly
        BillingPeriod.YEARLY -> R.string.billing_period_yearly
    }

    @StringRes
    fun billingPeriodOption(period: BillingPeriod): Int = when (period) {
        BillingPeriod.MONTHLY -> R.string.billing_period_monthly_option
        BillingPeriod.YEARLY -> R.string.billing_period_yearly_option
    }

    @StringRes
    fun billingPeriodRecurrence(period: BillingPeriod): Int = when (period) {
        BillingPeriod.MONTHLY -> R.string.billing_period_monthly_recurrence
        BillingPeriod.YEARLY -> R.string.billing_period_yearly_recurrence
    }

    @StringRes
    fun category(category: ProviderCategory): Int = when (category) {
        ProviderCategory.VIDEO -> R.string.category_video
        ProviderCategory.MUSIC -> R.string.category_music
        ProviderCategory.AI -> R.string.category_ai
        ProviderCategory.CLOUD -> R.string.category_cloud
        ProviderCategory.SOFTWARE -> R.string.category_software
        ProviderCategory.HOSTING -> R.string.category_hosting
        ProviderCategory.SHOPPING -> R.string.category_shopping
        ProviderCategory.GAMING -> R.string.category_gaming
        ProviderCategory.BOOKS -> R.string.category_books
        ProviderCategory.OTHER -> R.string.category_other
    }

    @StringRes
    fun statisticsPeriod(period: StatisticsPeriod): Int = when (period) {
        StatisticsPeriod.MONTH -> R.string.statistics_period_month
        StatisticsPeriod.YEAR -> R.string.statistics_period_year
        StatisticsPeriod.TWELVE_MONTHS -> R.string.statistics_period_twelve_months
    }

    @StringRes
    fun inputError(error: SubscriptionInputError): Int = when (error) {
        SubscriptionInputError.ProviderMissing -> R.string.form_error_provider_missing
        SubscriptionInputError.NameMissing -> R.string.form_error_name_missing
        SubscriptionInputError.NameTooLong -> R.string.form_error_name_too_long
        SubscriptionInputError.ManagementUrlInvalid -> R.string.form_error_url_invalid
        is SubscriptionInputError.Price -> priceError(error.error)
    }

    @StringRes
    private fun priceError(error: PriceError): Int = when (error) {
        PriceError.EMPTY -> R.string.form_error_price_empty
        PriceError.MALFORMED -> R.string.form_error_price_malformed
        PriceError.NEGATIVE -> R.string.form_error_price_negative
        PriceError.ZERO -> R.string.form_error_price_zero
        PriceError.TOO_MANY_DECIMALS -> R.string.form_error_price_decimals
        PriceError.TOO_LARGE -> R.string.form_error_price_too_large
    }
}
