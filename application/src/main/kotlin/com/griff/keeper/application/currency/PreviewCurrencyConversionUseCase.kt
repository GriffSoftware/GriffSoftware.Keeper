package com.griff.keeper.application.currency

import com.griff.keeper.domain.currency.CurrencyConversionPlanner
import com.griff.keeper.domain.currency.CurrencyConversionPreview
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.SubscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Builds the "before you convert" preview for a rate the user has typed but not yet confirmed.
 *
 * Reads the records but writes nothing: this is the step that lets the user see a couple of real
 * before/after amounts and back out before `ChangeAppCurrencyUseCase` touches the database.
 */
class PreviewCurrencyConversionUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
) {
    suspend operator fun invoke(
        from: Currency,
        to: Currency,
        rate: ExchangeRate,
    ): CurrencyConversionPreview = CurrencyConversionPlanner.preview(
        subscriptions = subscriptions.observeAll().first(),
        obligations = obligations.observeAll().first(),
        from = from,
        to = to,
        rate = rate,
    )
}
