package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/*
 * Round-ups.
 *
 * Every figure here is money the app is about to move into savings on the user's say-so, so the
 * two ways to get it wrong both matter: offering change that was already swept takes the money
 * twice, and rounding something that is already round invents a saving out of nothing.
 */
class RoundUpTest {

    private fun spend(
        amountMinor: Long,
        date: LocalDate = LocalDate.of(2026, 9, 21),
        currency: String = "USD",
    ) = Txn(
        id = "$amountMinor-$date",
        merchant = "Shop",
        categoryId = "food",
        accountId = "cash",
        amountMinor = amountMinor,
        date = date,
        time = LocalTime.NOON,
        flow = if (amountMinor < 0) Flow.Out else Flow.In,
        currency = currency,
    )

    // --- One spend ------------------------------------------------------------------------------

    @Test
    fun `the change is what it takes to reach the next whole unit`() {
        assertEquals(50L, changeFrom(-12_50L, 100))
        assertEquals(1L, changeFrom(-9_99L, 100))
        assertEquals(99L, changeFrom(-9_01L, 100))
    }

    @Test
    fun `an amount already on the boundary gives nothing`() {
        // Rounding 5.00 up to 6.00 would be inventing a saving rather than collecting change.
        assertEquals(0L, changeFrom(-5_00L, 100))
        assertEquals(0L, changeFrom(-10_00L, 500))
    }

    @Test
    fun `a bigger step collects more`() {
        assertEquals(2_50L, changeFrom(-12_50L, 500))
        assertEquals(7_50L, changeFrom(-12_50L, 1000))
    }

    @Test
    fun `the sign does not matter to the arithmetic`() {
        assertEquals(changeFrom(-12_50L, 100), changeFrom(12_50L, 100))
    }

    @Test
    fun `a step of nothing rounds nothing`() {
        assertEquals(0L, changeFrom(-12_50L, 1))
        assertEquals(0L, changeFrom(-12_50L, 0))
    }

    // --- Across the ledger -----------------------------------------------------------------------

    @Test
    fun `only money out is rounded up`() {
        // Rounding income up would take money out of savings on payday.
        val rows = listOf(spend(-12_50L), spend(3_000_33L))
        assertEquals(50L, roundUpsSince(rows, since = null, toMinor = 100))
    }

    @Test
    fun `everything after the last sweep counts, and nothing before it`() {
        val swept = LocalDate.of(2026, 9, 10)
        val rows = listOf(
            spend(-12_50L, date = swept.minusDays(1)),
            spend(-12_50L, date = swept),
            spend(-12_50L, date = swept.plusDays(1)),
            spend(-12_50L, date = swept.plusDays(2)),
        )
        // The sweep day itself is already collected: `since` is exclusive.
        assertEquals(100L, roundUpsSince(rows, since = swept, toMinor = 100))
    }

    @Test
    fun `with no sweep yet the whole ledger counts`() {
        val rows = listOf(spend(-12_50L), spend(-9_99L))
        assertEquals(51L, roundUpsSince(rows, since = null, toMinor = 100))
    }

    @Test
    fun `a row in a currency with no rate is skipped rather than counted raw`() {
        // The same refusal every other total makes: a sweep must be a figure the ledger backs.
        // A rate table that knows USD and nothing else, so the KRW row has no way home.
        val conversion = Conversion(
            base = "USD",
            enabled = true,
            rates = Rates("USD", LocalDate.of(2026, 9, 21), mapOf("USD" to 1_000_000L), 0L),
        )
        val rows = listOf(spend(-12_50L, currency = "USD"), spend(-12_50L, currency = "KRW"))
        assertEquals(50L, roundUpsSince(rows, since = null, toMinor = 100, conversion = conversion))
    }

    @Test
    fun `an empty ledger offers nothing`() {
        assertEquals(0L, roundUpsSince(emptyList(), since = null, toMinor = 100))
    }
}
