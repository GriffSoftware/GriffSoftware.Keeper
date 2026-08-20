package com.griff.subscriptions.domain.search

import com.griff.subscriptions.domain.model.ExpensePeriod
import com.griff.subscriptions.domain.model.ObligationCategory
import com.griff.subscriptions.domain.model.ObligationTag
import com.griff.subscriptions.domain.model.PaymentState
import com.griff.subscriptions.domain.testing.testObligation
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObligationFilterTest {

    private val year2026 = ExpensePeriod.Year(2026)

    private val obligations = listOf(
        testObligation(
            id = "1",
            name = "OC Ford",
            payment = PaymentState.Paid(LocalDate.of(2026, 3, 12)),
            validUntil = LocalDate.of(2027, 3, 11),
        ),
        testObligation(
            id = "2",
            name = "OC ZS775VG",
            payment = PaymentState.Paid(LocalDate.of(2026, 5, 20)),
            validUntil = LocalDate.of(2027, 5, 19),
        ),
        testObligation(
            id = "3",
            name = "Podatek od gruntu",
            category = ObligationCategory.LAND_TAX,
            payment = PaymentState.Unpaid,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        ),
        testObligation(
            id = "4",
            name = "Ubezpieczenie domu",
            category = ObligationCategory.HOME_INSURANCE,
            payment = PaymentState.Paid(LocalDate.of(2026, 6, 3)),
            validUntil = LocalDate.of(2027, 6, 2),
        ),
    )

    private fun names(filter: ObligationFilter) =
        obligations.applyFilter(filter).map { it.name.value }

    @Test
    fun `a plain period filter keeps everything from that year`() {
        assertEquals(4, names(ObligationFilter(period = year2026)).size)
    }

    @Test
    fun `search matches a case insensitive fragment of the name`() {
        assertEquals(
            listOf("OC Ford"),
            names(ObligationFilter(period = year2026, query = "ford")),
        )
        assertEquals(
            listOf("OC Ford"),
            names(ObligationFilter(period = year2026, query = "FORD")),
        )
    }

    @Test
    fun `search matches a registration plate fragment`() {
        assertEquals(
            listOf("OC ZS775VG"),
            names(ObligationFilter(period = year2026, query = "ZS775")),
        )
    }

    @Test
    fun `search trims the query`() {
        assertEquals(
            listOf("OC Ford"),
            names(ObligationFilter(period = year2026, query = "  ford  ")),
        )
    }

    @Test
    fun `the vehicle tag shows both policies and hides the land tax`() {
        val result = names(ObligationFilter(period = year2026, tag = ObligationTag.VEHICLE))

        assertEquals(listOf("OC Ford", "OC ZS775VG"), result.sorted())
        assertFalse("Podatek od gruntu" in result)
    }

    @Test
    fun `the tax tag folds both tax categories together`() {
        val withPropertyTax = obligations + testObligation(
            id = "5",
            name = "Podatek od nieruchomości",
            category = ObligationCategory.PROPERTY_TAX,
            payment = PaymentState.Unpaid,
            dueDate = LocalDate.of(2026, 9, 15),
            validUntil = null,
        )

        val result = withPropertyTax
            .applyFilter(ObligationFilter(period = year2026, tag = ObligationTag.TAX))
            .map { it.name.value }

        assertEquals(listOf("Podatek od gruntu", "Podatek od nieruchomości"), result.sorted())
    }

    @Test
    fun `search and tag apply together`() {
        assertEquals(
            listOf("OC Ford"),
            names(
                ObligationFilter(period = year2026, query = "ford", tag = ObligationTag.VEHICLE),
            ),
        )
        // The text matches, the tag does not.
        assertTrue(
            names(
                ObligationFilter(period = year2026, query = "ford", tag = ObligationTag.TAX),
            ).isEmpty(),
        )
    }

    @Test
    fun `a paid record is filtered by its payment date, not by its expiry`() {
        // Every policy above expires in 2027 but was paid in 2026.
        assertTrue(names(ObligationFilter(period = ExpensePeriod.Year(2027))).none { it == "OC Ford" })
        assertTrue("OC Ford" in names(ObligationFilter(period = year2026)))
    }

    @Test
    fun `an unpaid record is filtered by its deadline`() {
        val september = ExpensePeriod.Month(YearMonth.of(2026, 9))

        assertEquals(listOf("Podatek od gruntu"), names(ObligationFilter(period = september)))
    }

    @Test
    fun `a record without any date is never filtered out by a period`() {
        val undated = testObligation(
            id = "9",
            name = "Opłata bez terminu",
            category = ObligationCategory.OTHER,
            payment = PaymentState.Unpaid,
            dueDate = null,
            validUntil = null,
        )

        assertEquals(
            listOf("Opłata bez terminu"),
            listOf(undated)
                .applyFilter(ObligationFilter(period = ExpensePeriod.Year(2031)))
                .map { it.name.value },
        )
    }

    @Test
    fun `sorting puts the nearest deadline first and undated records last`() {
        val undated = testObligation(
            id = "9",
            name = "Opłata bez terminu",
            payment = PaymentState.Unpaid,
            dueDate = null,
            validUntil = null,
        )

        val sorted = (obligations + undated).sortedByDeadline().map { it.name.value }

        assertEquals(
            listOf(
                "Podatek od gruntu", // due 2026-09-15
                "OC Ford", // expires 2027-03-11
                "OC ZS775VG", // expires 2027-05-19
                "Ubezpieczenie domu", // expires 2027-06-02
                "Opłata bez terminu",
            ),
            sorted,
        )
    }
}
