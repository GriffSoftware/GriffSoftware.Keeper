package com.griff.subscriptions.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {

    @Test
    fun `stores amounts in minor units`() {
        val money = Money.ofUnits(34, 99)

        assertEquals(3499, money.minorUnits)
        assertEquals(34, money.wholeUnits)
        assertEquals(99, money.fraction)
    }

    @Test
    fun `adds amounts without precision loss`() {
        val total = listOf(
            Money.ofUnits(34, 99),
            Money.ofUnits(29, 99),
            Money.ofUnits(0, 1),
        ).sum()

        assertEquals(6499, total.minorUnits)
    }

    @Test
    fun `multiplies monthly price into a yearly amount`() {
        assertEquals(41_988, (Money.ofUnits(34, 99) * 12).minorUnits)
    }

    @Test
    fun `divides yearly price into a monthly amount rounding half up`() {
        // 599,00 / 12 = 49,9166... -> 49,92
        assertEquals(4992, Money.ofUnits(599).dividedBy(12).minorUnits)
        // 100,00 / 12 = 8,3333... -> 8,33
        assertEquals(833, Money.ofUnits(100).dividedBy(12).minorUnits)
        // 0,05 / 2 = 0,025 -> 0,03 (half up)
        assertEquals(3, Money.ofMinorUnits(5).dividedBy(2).minorUnits)
    }

    @Test
    fun `rejects negative amounts`() {
        assertFailsWith<IllegalArgumentException> { Money.ofMinorUnits(-1) }
    }

    @Test
    fun `rejects invalid fractions`() {
        assertFailsWith<IllegalArgumentException> { Money.ofUnits(10, 100) }
    }

    @Test
    fun `computes share of a total`() {
        val share = Money.ofUnits(25).shareOf(Money.ofUnits(100))

        assertEquals(0.25f, share)
        assertEquals(0f, Money.ofUnits(25).shareOf(Money.ZERO))
    }

    @Test
    fun `compares amounts`() {
        assertTrue(Money.ofUnits(10) > Money.ofUnits(9, 99))
        assertEquals(Money.ZERO, Money.ofMinorUnits(0))
    }
}
