package com.griff.keeper.application.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class PreviewCurrencyConversionUseCaseTest {

    private val rate = ExchangeRate.ofOrNull(BigDecimal("4.25")) ?: error("test rate must be valid")

    @Test
    fun `builds a preview from the stored records without writing anything`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(
                testSubscription(id = "s1", name = "Netflix", priceMinorUnits = 5_999),
            ),
            localObligations = listOf(
                testObligation(id = "o1", name = "OC Ford", amountMinorUnits = 120_000),
            ),
        )

        val preview = fixture.previewCurrencyConversion(Currency.PLN, Currency.EUR, rate)

        assertEquals(1, preview.subscriptionCount)
        assertEquals(1, preview.obligationCount)
        assertEquals(Money.ofMinorUnits(5_999), fixture.subscriptions.stored.single().price)
        assertEquals(Currency.PLN, fixture.subscriptions.stored.single().currency)
        assertEquals(0, fixture.conversionRepository.convertCallCount)
    }
}
