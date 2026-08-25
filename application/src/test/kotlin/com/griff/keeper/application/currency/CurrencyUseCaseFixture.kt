package com.griff.keeper.application.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.Obligation
import com.griff.keeper.domain.model.Subscription
import com.griff.keeper.domain.testing.FakeAppCurrencyRepository
import com.griff.keeper.domain.testing.FakeCurrencyConversionRepository
import com.griff.keeper.domain.testing.FakeObligationRepository
import com.griff.keeper.domain.testing.FakeSubscriptionRepository

/**
 * One wiring of the currency use cases and their doubles, mirroring `BackupUseCaseFixture`: assembled
 * by hand so each test is only about the decision the use case makes.
 */
internal class CurrencyUseCaseFixture(
    localSubscriptions: List<Subscription> = emptyList(),
    localObligations: List<Obligation> = emptyList(),
    localAppCurrency: Currency = Currency.Default,
) {
    val subscriptions = FakeSubscriptionRepository(localSubscriptions)
    val obligations = FakeObligationRepository(localObligations)
    val appCurrency = FakeAppCurrencyRepository(localAppCurrency)
    val conversionRepository = FakeCurrencyConversionRepository(subscriptions, obligations)

    val beginCurrencyChange = BeginCurrencyChangeUseCase(
        subscriptions = subscriptions,
        obligations = obligations,
        appCurrency = appCurrency,
    )

    val previewCurrencyConversion = PreviewCurrencyConversionUseCase(
        subscriptions = subscriptions,
        obligations = obligations,
    )

    val changeAppCurrency = ChangeAppCurrencyUseCase(
        conversionRepository = conversionRepository,
        appCurrency = appCurrency,
    )
}
