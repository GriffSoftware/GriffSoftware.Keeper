package com.griff.subscriptions.domain.calculation

import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.testing.testObligation
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObligationCostCalculatorTest {

    private val ford = testObligation(
        id = "o-1",
        name = "OC Ford",
        amountMinorUnits = 124_000,
        payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
        validUntil = LocalDate.of(2027, 3, 11),
    )

    private val landTax = testObligation(
        id = "o-2",
        name = "Podatek od gruntu",
        category = ObligationCategory.LAND_TAX,
        amountMinorUnits = 32_000,
        payment = PaymentState.Paid(LocalDate.of(2026, 3, 15)),
        validUntil = null,
    )

    private val unpaidPropertyTax = testObligation(
        id = "o-3",
        name = "Podatek od nieruchomości",
        category = ObligationCategory.PROPERTY_TAX,
        amountMinorUnits = 92_000,
        payment = PaymentState.Unpaid,
        dueDate = LocalDate.of(2026, 9, 15),
        validUntil = null,
    )

    private val all = listOf(ford, landTax, unpaidPropertyTax)

    @Test
    fun `only paid records count towards the total`() {
        val totals = ObligationCostCalculator.totals(all, ExpensePeriod.Year(2026))

        assertEquals(156_000, totals.paid.minorUnits)
        assertEquals(2, totals.paidCount)
        // The open charge is reported separately instead of being added to what was paid.
        assertEquals(92_000, totals.outstanding.minorUnits)
        assertEquals(1, totals.outstandingCount)
    }

    @Test
    fun `a payment on the last day of december belongs to that year`() {
        val newYearsEve = ford.copy(payment = PaymentState.Paid(LocalDate.of(2026, 12, 31)))

        assertEquals(
            124_000,
            ObligationCostCalculator.paidTotal(listOf(newYearsEve), ExpensePeriod.Year(2026))
                .minorUnits,
        )
        assertTrue(
            ObligationCostCalculator.paidIn(listOf(newYearsEve), ExpensePeriod.Year(2027)).isEmpty(),
        )
    }

    @Test
    fun `an expiry date never moves an expense into another year`() {
        // Paid in December 2026, cover runs until December 2027: a 2026 expense.
        val policy = testObligation(
            id = "o-9",
            payment = PaymentState.Paid(LocalDate.of(2026, 12, 20)),
            validUntil = LocalDate.of(2027, 12, 19),
            amountMinorUnits = 120_000,
        )

        assertEquals(
            120_000,
            ObligationCostCalculator.paidTotal(listOf(policy), ExpensePeriod.Year(2026)).minorUnits,
        )
        assertEquals(
            0,
            ObligationCostCalculator.paidTotal(listOf(policy), ExpensePeriod.Year(2027)).minorUnits,
        )
    }

    @Test
    fun `a month window only counts payments made in that month`() {
        val march = ExpensePeriod.Month(YearMonth.of(2026, 3))
        val april = ExpensePeriod.Month(YearMonth.of(2026, 4))

        assertEquals(156_000, ObligationCostCalculator.paidTotal(all, march).minorUnits)
        assertEquals(0, ObligationCostCalculator.paidTotal(all, april).minorUnits)
    }

    @Test
    fun `payments per month cover the whole period and place amounts once`() {
        val perMonth = ObligationCostCalculator.paidPerMonth(all, ExpensePeriod.Year(2026))

        assertEquals(12, perMonth.size)
        // A yearly policy is never spread across months.
        assertEquals(156_000, perMonth.getValue(YearMonth.of(2026, 3)).minorUnits)
        assertEquals(0, perMonth.getValue(YearMonth.of(2026, 2)).minorUnits)
        assertEquals(0, perMonth.getValue(YearMonth.of(2026, 9)).minorUnits)
    }

    @Test
    fun `a rolling range window spans the months it was given`() {
        val range = ExpensePeriod.Range(YearMonth.of(2025, 12), YearMonth.of(2026, 3))
        val perMonth = ObligationCostCalculator.paidPerMonth(all, range)

        assertEquals(
            listOf(
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2),
                YearMonth.of(2026, 3),
            ),
            perMonth.keys.toList(),
        )
        assertEquals(156_000, ObligationCostCalculator.paidTotal(all, range).minorUnits)
    }

    @Test
    fun `the largest paid amount ignores unpaid records`() {
        val totals = ObligationCostCalculator.totals(all, ExpensePeriod.Year(2026))

        // The unpaid property tax is the biggest amount, but nothing was paid for it.
        assertEquals(124_000, totals.largestPaid.minorUnits)
    }

    @Test
    fun `an empty collection yields zeroes rather than a crash`() {
        val totals = ObligationCostCalculator.totals(emptyList(), ExpensePeriod.Year(2026))

        assertEquals(0, totals.paid.minorUnits)
        assertEquals(0, totals.largestPaid.minorUnits)
        assertEquals(0, totals.count)
    }
}
