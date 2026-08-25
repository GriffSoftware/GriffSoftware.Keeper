package com.griff.keeper.application.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class BeginCurrencyChangeUseCaseTest {

    @Test
    fun `switches immediately when the device holds no financial data`() = runTest {
        val fixture = CurrencyUseCaseFixture(localAppCurrency = Currency.PLN)

        val outcome = fixture.beginCurrencyChange(Currency.EUR)

        assertIs<CurrencyChangeOutcome.SwitchedImmediately>(outcome)
        assertEquals(Currency.EUR, outcome.currency)
        assertEquals(Currency.EUR, fixture.appCurrency.current())
    }

    @Test
    fun `requires a rate when at least one subscription exists`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(testSubscription()),
            localAppCurrency = Currency.PLN,
        )

        val outcome = fixture.beginCurrencyChange(Currency.EUR)

        assertIs<CurrencyChangeOutcome.RequiresConversion>(outcome)
        assertEquals(Currency.PLN, outcome.from)
        assertEquals(Currency.EUR, outcome.to)
        assertEquals(1, outcome.recordCount)
        // Nothing has been written; only a Room transaction, confirmed separately, may do that.
        assertEquals(Currency.PLN, fixture.appCurrency.current())
    }

    @Test
    fun `requires a rate when at least one obligation exists`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localObligations = listOf(testObligation()),
            localAppCurrency = Currency.PLN,
        )

        val outcome = fixture.beginCurrencyChange(Currency.EUR)

        assertIs<CurrencyChangeOutcome.RequiresConversion>(outcome)
        assertEquals(1, outcome.recordCount)
    }

    @Test
    fun `picking the currency already active is a no-op`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(testSubscription()),
            localAppCurrency = Currency.PLN,
        )

        val outcome = fixture.beginCurrencyChange(Currency.PLN)

        assertIs<CurrencyChangeOutcome.SwitchedImmediately>(outcome)
        assertEquals(0, fixture.conversionRepository.convertCallCount)
    }
}
