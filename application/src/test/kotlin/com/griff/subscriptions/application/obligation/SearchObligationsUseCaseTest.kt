package com.griff.subscriptions.application.obligation

import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.search.ObligationFilter
import com.griff.subscriptions.domain.testing.FakeObligationRepository
import com.griff.subscriptions.domain.testing.testObligation
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SearchObligationsUseCaseTest {

    private val repository = FakeObligationRepository(
        listOf(
            testObligation(
                id = "1",
                name = "OC Ford",
                amountMinorUnits = 124_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
                validUntil = LocalDate.of(2027, 3, 11),
            ),
            testObligation(
                id = "2",
                name = "OC ZS775VG",
                amountMinorUnits = 98_000,
                payment = PaymentState.Paid(LocalDate.of(2026, 5, 20)),
                validUntil = LocalDate.of(2027, 5, 19),
            ),
            testObligation(
                id = "3",
                name = "Podatek od gruntu",
                category = ObligationCategory.LAND_TAX,
                amountMinorUnits = 32_000,
                payment = PaymentState.Unpaid,
                dueDate = LocalDate.of(2026, 9, 15),
                validUntil = null,
            ),
            testObligation(
                id = "4",
                name = "Ubezpieczenie drona",
                category = ObligationCategory.DRONE_INSURANCE,
                amountMinorUnits = 19_000,
                payment = PaymentState.Paid(LocalDate.of(2025, 8, 2)),
                validUntil = LocalDate.of(2026, 8, 1),
            ),
        ),
    )

    private val useCase = SearchObligationsUseCase(repository)

    private val year2026 = ExpensePeriod.Year(2026)

    private suspend fun result(filter: ObligationFilter) = useCase(MutableStateFlow(filter)).first()

    @Test
    fun `the year window keeps records booked to it`() = runTest {
        val result = result(ObligationFilter(period = year2026))

        // The drone policy was paid in 2025; it only expires in 2026.
        assertEquals(
            listOf("Podatek od gruntu", "OC Ford", "OC ZS775VG"),
            result.matching.map { it.name.value },
        )
        assertEquals(4, result.totalCount)
        assertFalse(result.isNarrowed)
    }

    @Test
    fun `the summary counts only what was paid inside the window`() = runTest {
        val result = result(ObligationFilter(period = year2026))

        // 1 240 + 980; the unpaid tax and the 2025 payment stay out.
        assertEquals(222_000, result.totals.paid.minorUnits)
        assertEquals(2, result.totals.paidCount)
        assertEquals(32_000, result.totals.outstanding.minorUnits)
    }

    @Test
    fun `searching narrows the list and says so`() = runTest {
        val result = result(ObligationFilter(period = year2026, query = "ford"))

        assertEquals(listOf("OC Ford"), result.matching.map { it.name.value })
        assertTrue(result.isNarrowed)
        assertEquals(4, result.totalCount)
    }

    @Test
    fun `a tag filter and a query work together`() = runTest {
        val vehicles = result(ObligationFilter(period = year2026, tag = ObligationTag.VEHICLE))
        assertEquals(listOf("OC Ford", "OC ZS775VG"), vehicles.matching.map { it.name.value })

        val narrowed = result(
            ObligationFilter(period = year2026, tag = ObligationTag.VEHICLE, query = "ZS775"),
        )
        assertEquals(listOf("OC ZS775VG"), narrowed.matching.map { it.name.value })

        val mismatched = result(
            ObligationFilter(period = year2026, tag = ObligationTag.TAX, query = "ford"),
        )
        assertTrue(mismatched.matching.isEmpty())
    }

    @Test
    fun `switching the period on the same stream re-filters`() = runTest {
        val filters = MutableStateFlow(ObligationFilter(period = year2026))
        assertEquals(3, useCase(filters).first().matching.size)

        filters.value = ObligationFilter(period = ExpensePeriod.Month(YearMonth.of(2026, 9)))
        val september = useCase(filters).first()

        assertEquals(listOf("Podatek od gruntu"), september.matching.map { it.name.value })
        assertEquals(0, september.totals.paid.minorUnits)
    }

    @Test
    fun `only tags present in the data are offered`() = runTest {
        val result = result(ObligationFilter(period = year2026))

        assertEquals(
            listOf(ObligationTag.VEHICLE, ObligationTag.DRONE, ObligationTag.TAX),
            result.availableTags,
        )
    }
}
