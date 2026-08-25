package com.griff.keeper.application.currency

import com.griff.keeper.domain.currency.CurrencyConversionErrorType
import com.griff.keeper.domain.currency.CurrencyConversionException
import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Commits a previewed conversion: the transactional rewrite, then the preference, in that order. */
class ChangeAppCurrencyUseCaseTest {

    private val rate = ExchangeRate.ofOrNull(BigDecimal("4.25")) ?: error("test rate must be valid")

    @Test
    fun `converts every stored record and persists the new currency`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(
                testSubscription(id = "s1", priceMinorUnits = 5_999, currency = Currency.PLN),
            ),
            localObligations = listOf(
                testObligation(id = "o1", amountMinorUnits = 120_000, currency = Currency.PLN),
            ),
            localAppCurrency = Currency.PLN,
        )

        val result = fixture.changeAppCurrency(Currency.PLN, Currency.EUR, rate)

        assertTrue(result.isSuccess)
        assertEquals(Currency.EUR, fixture.appCurrency.current())

        val subscription = fixture.subscriptions.stored.single()
        assertEquals(Currency.EUR, subscription.currency)
        // 59.99 / 4.25 = 14.1152... -> 14.12
        assertEquals(Money.ofUnits(14, 12), subscription.price)

        val obligation = fixture.obligations.stored.single()
        assertEquals(Currency.EUR, obligation.currency)
        // 1200.00 / 4.25 = 282.3529... -> 282.35
        assertEquals(Money.ofUnits(282, 35), obligation.amount)
    }

    @Test
    fun `every record ends up in the new currency, never a mix`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(
                testSubscription(id = "s1", currency = Currency.PLN),
                testSubscription(id = "s2", currency = Currency.PLN),
            ),
            localObligations = listOf(
                testObligation(id = "o1", currency = Currency.PLN),
                testObligation(id = "o2", currency = Currency.PLN),
                testObligation(id = "o3", currency = Currency.PLN),
            ),
            localAppCurrency = Currency.PLN,
        )

        fixture.changeAppCurrency(Currency.PLN, Currency.EUR, rate)

        assertTrue(fixture.subscriptions.stored.all { it.currency == Currency.EUR })
        assertTrue(fixture.obligations.stored.all { it.currency == Currency.EUR })
    }

    @Test
    fun `a failed conversion leaves every record in its original currency`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "s1", currency = Currency.PLN)),
            localAppCurrency = Currency.PLN,
        )
        fixture.conversionRepository.failOnConvert = true

        val result = fixture.changeAppCurrency(Currency.PLN, Currency.EUR, rate)

        assertTrue(result.isFailure)
        assertEquals(Currency.PLN, fixture.subscriptions.stored.single().currency)
        // The preference is never touched when the conversion itself never committed.
        assertEquals(Currency.PLN, fixture.appCurrency.current())
    }

    @Test
    fun `a preference write failure after a successful conversion is reported distinctly`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(testSubscription(id = "s1", currency = Currency.PLN)),
            localAppCurrency = Currency.PLN,
        )
        fixture.appCurrency.failOnSet = true

        val result = fixture.changeAppCurrency(Currency.PLN, Currency.EUR, rate)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as? CurrencyConversionException
        assertEquals(CurrencyConversionErrorType.PREFERENCE_NOT_PERSISTED, error?.errorType)
        // The data is safe - every record already carries the new currency - only the preference
        // write failed.
        assertEquals(Currency.EUR, fixture.subscriptions.stored.single().currency)
    }

    @Test
    fun `converting a currency to itself is a same-value no-op`() = runTest {
        val fixture = CurrencyUseCaseFixture(
            localSubscriptions = listOf(
                testSubscription(id = "s1", priceMinorUnits = 5_999, currency = Currency.PLN),
            ),
            localAppCurrency = Currency.PLN,
        )

        fixture.changeAppCurrency(Currency.PLN, Currency.PLN, rate)

        assertEquals(Money.ofMinorUnits(5_999), fixture.subscriptions.stored.single().price)
    }
}
