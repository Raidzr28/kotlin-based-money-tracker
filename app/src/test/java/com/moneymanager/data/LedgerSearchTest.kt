package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/*
 * Searching the ledger.
 *
 * A search that quietly drops a row is worse than one that finds nothing: the user concludes the
 * transaction was never logged and logs it again. The amount bounds are the sharpest edge here,
 * because money out is held negative and a naive comparison lets every expense through.
 */
class LedgerSearchTest {

    @Before
    fun categories() {
        // matches() reads category labels through the snapshot the repository normally fills.
        Categories.snapshot = listOf(
            Category("food", "Food & drink", "restaurant"),
            Category("home", "Home", "home"),
        )
    }

    private fun txn(
        merchant: String = "Tesco",
        amountMinor: Long = -12_50L,
        date: LocalDate = LocalDate.of(2026, 9, 21),
        categoryId: String = "food",
        accountId: String = "cash",
        tags: List<String> = emptyList(),
        flow: Flow = if (amountMinor < 0) Flow.Out else Flow.In,
    ) = Txn(
        id = merchant + amountMinor + date,
        merchant = merchant,
        categoryId = categoryId,
        accountId = accountId,
        amountMinor = amountMinor,
        date = date,
        time = LocalTime.NOON,
        flow = flow,
        tags = tags,
    )

    // --- Nothing asked for ------------------------------------------------------------------

    @Test
    fun `an empty query matches everything`() {
        val q = LedgerQuery()
        assertTrue(q.isWideOpen)
        assertTrue(q.matches(txn()))
        assertTrue(q.matches(txn(amountMinor = 5_000_00L)))
    }

    // --- Text -------------------------------------------------------------------------------

    @Test
    fun `text matches merchant, category or tag, ignoring case`() {
        assertTrue(LedgerQuery(text = "tes").matches(txn(merchant = "Tesco")))
        assertTrue(LedgerQuery(text = "FOOD").matches(txn(categoryId = "food")))
        assertTrue(LedgerQuery(text = "bali").matches(txn(tags = listOf("Trip to Bali"))))
        assertFalse(LedgerQuery(text = "chemist").matches(txn(merchant = "Tesco")))
    }

    // --- Amount bounds ----------------------------------------------------------------------

    @Test
    fun `amount bounds are on size, not sign`() {
        // The bug this exists to prevent: -12.50 is less than 10.00 as a signed number, so a
        // naive `amountMinor >= minMinor` would drop every expense from an "at least" search.
        val spend = txn(amountMinor = -12_50L)
        assertTrue(LedgerQuery(minMinor = 10_00L).matches(spend))
        assertFalse(LedgerQuery(minMinor = 20_00L).matches(spend))
        assertTrue(LedgerQuery(maxMinor = 20_00L).matches(spend))
        assertFalse(LedgerQuery(maxMinor = 10_00L).matches(spend))
    }

    @Test
    fun `income and spending of the same size both match the same band`() {
        val band = LedgerQuery(minMinor = 10_00L, maxMinor = 20_00L)
        assertTrue(band.matches(txn(amountMinor = -12_50L)))
        assertTrue(band.matches(txn(amountMinor = 12_50L, flow = Flow.In)))
    }

    @Test
    fun `a bound of zero is no bound at all`() {
        // An empty amount field reads as 0, and must not be taken as "at most nothing".
        assertTrue(LedgerQuery(minMinor = 0L, maxMinor = 0L).matches(txn(amountMinor = -9_999_00L)))
    }

    @Test
    fun `the bounds are inclusive at both ends`() {
        val band = LedgerQuery(minMinor = 12_50L, maxMinor = 12_50L)
        assertTrue(band.matches(txn(amountMinor = -12_50L)))
    }

    // --- Date, account, tags ------------------------------------------------------------------

    @Test
    fun `from is inclusive of its own day`() {
        val march = LocalDate.of(2026, 3, 1)
        val q = LedgerQuery(from = march)
        assertTrue(q.matches(txn(date = march)))
        assertTrue(q.matches(txn(date = march.plusDays(1))))
        assertFalse(q.matches(txn(date = march.minusDays(1))))
    }

    @Test
    fun `an account filter keeps only that account`() {
        val q = LedgerQuery(accountId = "cash")
        assertTrue(q.matches(txn(accountId = "cash")))
        assertFalse(q.matches(txn(accountId = "card")))
    }

    @Test
    fun `tagged only drops untagged rows`() {
        val q = LedgerQuery(taggedOnly = true)
        assertTrue(q.matches(txn(tags = listOf("holiday"))))
        assertFalse(q.matches(txn(tags = emptyList())))
    }

    @Test
    fun `a transfer is only found when transfers are asked for`() {
        val transfer = txn(amountMinor = -50_00L, flow = Flow.Transfer)
        assertTrue(LedgerQuery(flow = Flow.Transfer).matches(transfer))
        assertFalse(LedgerQuery(flow = Flow.Out).matches(transfer))
        // No flow asked for means every direction, transfers included.
        assertTrue(LedgerQuery().matches(transfer))
    }

    // --- Together -----------------------------------------------------------------------------

    @Test
    fun `filters narrow together rather than widening each other`() {
        val rows = listOf(
            txn(merchant = "Tesco", amountMinor = -12_50L, accountId = "cash"),
            txn(merchant = "Tesco", amountMinor = -80_00L, accountId = "card"),
            txn(merchant = "Chemist", amountMinor = -12_50L, accountId = "cash"),
        )
        val found = LedgerQuery(text = "tesco", accountId = "cash", maxMinor = 20_00L).filter(rows)
        assertEquals(1, found.size)
        assertEquals(-12_50L, found.single().amountMinor)
    }
}
