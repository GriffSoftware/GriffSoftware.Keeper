package com.griff.keeper.domain.currency

import com.griff.keeper.domain.calculation.MoneyConverter
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription

/** One before/after amount shown in the conversion preview, e.g. "Netflix 59,99 zł -> 14,12 €". */
data class CurrencyConversionSample(
    val name: String,
    val before: Money,
    val after: Money,
)

/**
 * What a global currency conversion would do, computed before a single record is written.
 *
 * Shown to the user as the "Zmiana waluty" preview: a handful of real [samples] and the total record
 * count, never every record - the point is to demonstrate the rate is being applied sensibly, not to
 * re-list the user's data.
 */
data class CurrencyConversionPreview(
    val from: Currency,
    val to: Currency,
    val rate: ExchangeRate,
    val samples: List<CurrencyConversionSample>,
    val subscriptionCount: Int,
    val obligationCount: Int,
) {
    val affectedRecordCount: Int get() = subscriptionCount + obligationCount
}

/**
 * Builds a [CurrencyConversionPreview] from the records a conversion would touch.
 *
 * Pure and Room/Android free on purpose: the arithmetic that decides what the user is shown must be
 * the exact same arithmetic [com.griff.keeper.domain.currency.CurrencyConversionRepository] later
 * applies, and keeping it here is what guarantees the preview cannot drift from the real write.
 */
object CurrencyConversionPlanner {

    private const val SAMPLE_SIZE = 3

    fun preview(
        subscriptions: List<Subscription>,
        obligations: List<Obligation>,
        from: Currency,
        to: Currency,
        rate: ExchangeRate,
    ): CurrencyConversionPreview {
        val subscriptionSamples = subscriptions.take(SAMPLE_SIZE).map { subscription ->
            CurrencyConversionSample(
                name = subscription.name.value,
                before = subscription.price,
                after = MoneyConverter.convert(subscription.price, from, to, rate),
            )
        }

        val remaining = SAMPLE_SIZE - subscriptionSamples.size
        val obligationSamples = if (remaining > 0) {
            obligations.take(remaining).map { obligation ->
                CurrencyConversionSample(
                    name = obligation.name.value,
                    before = obligation.amount,
                    after = MoneyConverter.convert(obligation.amount, from, to, rate),
                )
            }
        } else {
            emptyList()
        }

        return CurrencyConversionPreview(
            from = from,
            to = to,
            rate = rate,
            samples = subscriptionSamples + obligationSamples,
            subscriptionCount = subscriptions.size,
            obligationCount = obligations.size,
        )
    }
}
