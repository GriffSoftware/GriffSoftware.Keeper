package com.griff.keeper.presentation.common

import androidx.annotation.StringRes
import com.griff.keeper.domain.model.BillingPeriod
import com.griff.keeper.domain.model.ObligationCategory
import com.griff.keeper.domain.model.PaymentStatus
import com.griff.keeper.domain.model.ProviderCategory
import com.griff.keeper.domain.statistics.StatisticsPeriod
import com.griff.keeper.domain.statistics.StatisticsScope
import com.griff.keeper.domain.validation.ExchangeRateError
import com.griff.keeper.domain.validation.ObligationInputError
import com.griff.keeper.domain.validation.PriceError
import com.griff.keeper.domain.validation.SubscriptionInputError
import com.griff.keeper.presentation.R

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
    fun obligationCategory(category: ObligationCategory): Int = when (category) {
        ObligationCategory.VEHICLE_INSURANCE -> R.string.obligation_category_vehicle_insurance
        ObligationCategory.HOME_INSURANCE -> R.string.obligation_category_home_insurance
        ObligationCategory.LAND_INSURANCE -> R.string.obligation_category_land_insurance
        ObligationCategory.DRONE_INSURANCE -> R.string.obligation_category_drone_insurance
        ObligationCategory.PROPERTY_TAX -> R.string.obligation_category_property_tax
        ObligationCategory.LAND_TAX -> R.string.obligation_category_land_tax
        ObligationCategory.OTHER -> R.string.obligation_category_other
    }

    @StringRes
    fun paymentStatus(status: PaymentStatus): Int = when (status) {
        PaymentStatus.PAID -> R.string.payment_status_paid
        PaymentStatus.UNPAID -> R.string.payment_status_unpaid
    }

    @StringRes
    fun statisticsScope(scope: StatisticsScope): Int = when (scope) {
        StatisticsScope.ALL -> R.string.statistics_scope_all
        StatisticsScope.SUBSCRIPTIONS -> R.string.statistics_scope_subscriptions
        StatisticsScope.OBLIGATIONS -> R.string.statistics_scope_obligations
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
    fun obligationInputError(error: ObligationInputError): Int = when (error) {
        ObligationInputError.NameMissing -> R.string.obligation_form_error_name_missing
        ObligationInputError.NameTooLong -> R.string.obligation_form_error_name_too_long
        ObligationInputError.CategoryMissing -> R.string.obligation_form_error_category_missing
        ObligationInputError.PaymentDateMissing ->
            R.string.obligation_form_error_payment_date_missing

        ObligationInputError.NotesTooLong -> R.string.obligation_form_error_notes_too_long
        is ObligationInputError.Amount -> priceError(error.error)
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

    @StringRes
    fun exchangeRateError(error: ExchangeRateError): Int = when (error) {
        ExchangeRateError.EMPTY -> R.string.currency_rate_error_empty
        ExchangeRateError.MALFORMED -> R.string.currency_rate_error_malformed
        ExchangeRateError.NEGATIVE -> R.string.currency_rate_error_negative
        ExchangeRateError.ZERO -> R.string.currency_rate_error_zero
        ExchangeRateError.TOO_MANY_DECIMALS -> R.string.currency_rate_error_too_many_decimals
        ExchangeRateError.TOO_LARGE -> R.string.currency_rate_error_too_large
    }
}
