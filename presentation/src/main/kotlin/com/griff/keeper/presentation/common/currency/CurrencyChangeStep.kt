package com.griff.keeper.presentation.common.currency

import com.griff.keeper.domain.currency.CurrencyConversionPreview
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.validation.ExchangeRateError

/**
 * Which step of the currency-change flow is currently showing, if any.
 *
 * One state machine rather than a handful of booleans, for the same reason
 * [com.griff.keeper.presentation.datatransfer.DataTransferStage] is one enum: the steps are mutually
 * exclusive, so "which dialog is open" is one question with one answer, and moving to the next step
 * always replaces the whole value instead of leaving a stale one turned on.
 */
internal sealed interface CurrencyChangeStep {

    /** No conversion in progress; the picker itself is a separate, transient dialog flag. */
    data object None : CurrencyChangeStep

    /** Waiting for the user to type and confirm the "1 EUR = X PLN" rate. */
    data class EnteringRate(
        val from: Currency,
        val to: Currency,
        val rateInput: String = "",
        val error: ExchangeRateError? = null,
    ) : CurrencyChangeStep

    /** Showing sample before/after amounts for a rate the user has already typed. */
    data class Previewing(val preview: CurrencyConversionPreview) : CurrencyChangeStep

    /** The last, explicit "are you sure" gate before anything is written. */
    data class Confirming(val preview: CurrencyConversionPreview) : CurrencyChangeStep

    /** The Room transaction is running; every control in the flow is disabled. */
    data class Converting(val from: Currency, val to: Currency) : CurrencyChangeStep
}
