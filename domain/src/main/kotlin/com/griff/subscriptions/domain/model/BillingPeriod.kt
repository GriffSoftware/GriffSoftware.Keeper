package com.griff.subscriptions.domain.model

/** How often a subscription is charged. */
enum class BillingPeriod(val monthsPerPeriod: Int) {
    MONTHLY(1),
    YEARLY(12),
    ;

    val periodsPerYear: Int get() = MONTHS_PER_YEAR / monthsPerPeriod

    companion object {
        const val MONTHS_PER_YEAR: Int = 12
    }
}
