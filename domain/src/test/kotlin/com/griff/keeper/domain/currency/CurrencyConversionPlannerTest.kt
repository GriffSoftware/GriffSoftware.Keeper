package com.griff.keeper.domain.currency

import com.griff.keeper.domain.model.Currency
import com.griff.keeper.domain.model.ExchangeRate
import com.griff.keeper.domain.model.Money
import com.griff.keeper.domain.testing.testObligation
import com.griff.keeper.domain.testing.testSubscription
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyConversionPlannerTest {

    private val rate = ExchangeRate.ofOrNull(BigDecimal("4.25")) ?: error("test rate must be valid")

    @Test
    fun `previews totals and a sample for each kind of record`() {
        val preview = CurrencyConversionPlanner.preview(
            subscriptions = listOf(testSubscription(id = "s1", name = "Netflix", priceMinorUnits = 5_999)),
            obligations = listOf(testObligation(id = "o1", name = "OC Ford", amountMinorUnits = 120_000)),
            from = Currency.PLN,
            to = Currency.EUR,
            rate = rate,
        )

        assertEquals(Currency.PLN, preview.from)
        assertEquals(Currency.EUR, preview.to)
        assertEquals(1, preview.subscriptionCount)
        assertEquals(1, preview.obligationCount)
        assertEquals(2, preview.affectedRecordCount)
        assertEquals(2, preview.samples.size)

        val netflixSample = preview.samples.first { it.name == "Netflix" }
        assertEquals(Money.ofMinorUnits(5_999), netflixSample.before)
        // 59.99 / 4.25 = 14.1152... -> 14.12
        assertEquals(Money.ofUnits(14, 12), netflixSample.after)
    }

    @Test
    fun `takes at most three samples, subscriptions first`() {
        val subscriptions = List(2) { index ->
            testSubscription(id = "s$index", name = "Sub $index")
        }
        val obligations = List(5) { index ->
            testObligation(id = "o$index", name = "Obligation $index")
        }

        val preview = CurrencyConversionPlanner.preview(
            subscriptions = subscriptions,
            obligations = obligations,
            from = Currency.PLN,
            to = Currency.EUR,
            rate = rate,
        )

        assertEquals(3, preview.samples.size)
        assertEquals(2, preview.samples.count { it.name.startsWith("Sub") })
        assertEquals(1, preview.samples.count { it.name.startsWith("Obligation") })
        assertEquals(2, preview.subscriptionCount)
        assertEquals(5, preview.obligationCount)
    }

    @Test
    fun `counts totals correctly with nothing to sample`() {
        val preview = CurrencyConversionPlanner.preview(
            subscriptions = emptyList(),
            obligations = emptyList(),
            from = Currency.PLN,
            to = Currency.EUR,
            rate = rate,
        )

        assertEquals(0, preview.affectedRecordCount)
        assertEquals(emptyList(), preview.samples)
    }
}
