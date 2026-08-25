package com.griff.keeper.application.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.repository.AppCurrencyRepository
import com.griff.keeper.domain.repository.ObligationRepository
import com.griff.keeper.domain.repository.SubscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** What picking a new app currency leads to, decided before the user is asked for anything else. */
sealed interface CurrencyChangeOutcome {

    /**
     * The device held no subscription or obligation, so the switch already happened - there was
     * nothing to convert and therefore nothing to ask a rate for.
     */
    data class SwitchedImmediately(val currency: Currency) : CurrencyChangeOutcome

    /**
     * At least one record exists. The caller collects an [com.griff.keeper.domain.model.ExchangeRate]
     * from the user, previews the effect with `PreviewCurrencyConversionUseCase`, and only then
     * commits with `ChangeAppCurrencyUseCase`.
     */
    data class RequiresConversion(val from: Currency, val to: Currency, val recordCount: Int) :
        CurrencyChangeOutcome
}

/**
 * Starts a currency change: applies it immediately when there is nothing to convert, or reports what
 * a conversion would involve so the UI can ask for a rate.
 *
 * Nothing is written when [RequiresConversion] is returned - the actual conversion is a separate,
 * explicitly confirmed step (`ChangeAppCurrencyUseCase`), never a side effect of this check.
 */
class BeginCurrencyChangeUseCase @Inject constructor(
    private val subscriptions: SubscriptionRepository,
    private val obligations: ObligationRepository,
    private val appCurrency: AppCurrencyRepository,
) {
    suspend operator fun invoke(target: Currency): CurrencyChangeOutcome {
        val current = appCurrency.current()
        if (current == target) return CurrencyChangeOutcome.SwitchedImmediately(target)

        val subscriptionCount = subscriptions.observeAll().first().size
        val obligationCount = obligations.observeAll().first().size
        val recordCount = subscriptionCount + obligationCount

        if (recordCount == 0) {
            appCurrency.set(target)
            return CurrencyChangeOutcome.SwitchedImmediately(target)
        }

        return CurrencyChangeOutcome.RequiresConversion(current, target, recordCount)
    }
}
