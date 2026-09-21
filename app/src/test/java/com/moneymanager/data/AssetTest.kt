package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/*
 * What a possession is worth, and when.
 *
 * This is the one figure in the app that changes without anybody logging anything, which makes
 * it the easiest to get quietly wrong: a laptop that keeps its full value forever inflates net
 * worth, and one that goes negative after its life is up turns an asset into a debt.
 */
class AssetTest {

    private fun on(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    private fun laptop(
        cost: Long = 2_000_00L,
        bought: LocalDate = on(2024, 1, 15),
        life: Int? = 36,
        warranty: LocalDate? = null,
    ) = Asset("a1", "Laptop", cost, bought, life, warranty)

    @Test
    fun `on the day it is bought it is worth what it cost`() {
        assertEquals(2_000_00L, laptop().valueOn(on(2024, 1, 15)))
    }

    @Test
    fun `it loses value in a straight line`() {
        // Three years, so a third of the price a year.
        assertEquals(2_000_00L * 24 / 36, laptop().valueOn(on(2025, 1, 15)))
        assertEquals(2_000_00L * 12 / 36, laptop().valueOn(on(2026, 1, 15)))
    }

    @Test
    fun `the figure holds still within a month rather than ticking down daily`() {
        // A net worth that moves while you watch it is one you stop trusting.
        val a = laptop()
        assertEquals(a.valueOn(on(2024, 6, 15)), a.valueOn(on(2024, 7, 14)))
    }

    @Test
    fun `past its life it is worth nothing, never less`() {
        assertEquals(0L, laptop().valueOn(on(2027, 1, 15)))
        assertEquals(0L, laptop().valueOn(on(2030, 1, 1)))
    }

    @Test
    fun `before it was bought it was not yours`() {
        // The twelve-month net worth history walks backwards through dates it did not exist on.
        assertEquals(0L, laptop().valueOn(on(2023, 12, 31)))
    }

    @Test
    fun `something that holds its value holds all of it`() {
        val land = laptop(life = null)
        assertEquals(2_000_00L, land.valueOn(on(2024, 1, 15)))
        assertEquals(2_000_00L, land.valueOn(on(2044, 1, 15)))
        assertNull(land.depreciatedMinor)
    }

    @Test
    fun `a life of zero is worthless immediately rather than dividing by zero`() {
        assertEquals(0L, laptop(life = 0).valueOn(on(2024, 1, 15)))
    }

    @Test
    fun `what has been used up and what is left always add back to the price`() {
        val a = laptop()
        val used = a.copy().let { it.costMinor - it.valueOn(on(2025, 1, 15)) }
        assertEquals(2_000_00L, a.valueOn(on(2025, 1, 15)) + used)
    }

    @Test
    fun `a warranty is over the day after it ends`() {
        assertTrue(laptop(warranty = today.plusDays(1)).underWarranty)
        assertTrue(laptop(warranty = today).underWarranty)
        assertFalse(laptop(warranty = today.minusDays(1)).underWarranty)
        assertFalse(laptop(warranty = null).underWarranty)
    }

    @Test
    fun `a large price does not overflow on the way through`() {
        // cost * months has to stay inside a Long: a house over forty years is the worst case
        // this app will plausibly see.
        val house = Asset("a2", "House", 900_000_00L, on(2000, 1, 1), 480, null)
        assertEquals(900_000_00L / 2, house.valueOn(on(2020, 1, 1)))
    }
}
