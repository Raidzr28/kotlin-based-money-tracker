package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/*
 * An export is only worth having if it survives being read back. These check the two places the
 * format can silently corrupt a ledger: a cell that contains the delimiter, and the sign on a
 * small negative amount.
 */
class LedgerCsvTest {

    @Before
    fun seedLookups() {
        Categories.snapshot = listOf(Category("food", "Food, drink", "restaurant"))
        Accounts.snapshot = listOf(Account("cash", "Cash", AccountKind.Cash, 0))
    }

    private fun txn(
        merchant: String,
        minor: Long,
        note: String? = null,
        tags: List<String> = emptyList(),
    ) = Txn(
        id = "t1",
        merchant = merchant,
        categoryId = "food",
        accountId = "cash",
        amountMinor = minor,
        date = LocalDate.of(2026, 9, 15),
        time = LocalTime.of(9, 5),
        note = note,
        tags = tags,
    )

    @Test
    fun `quoted cells survive a round trip`() {
        val csv = ledgerCsv(listOf(txn("Bar \"Nine\", Jakarta", -12_50, note = "split with Ana")))
        val rows = parseCsv(csv, delimiter = ',')

        assertEquals(listOf("Date", "Time", "Merchant"), rows[0].take(3))
        assertEquals("Bar \"Nine\", Jakarta", rows[1][2])
        // The category name contains the delimiter too; it must not shift the columns right.
        assertEquals("Food, drink", rows[1][3])
        assertEquals("-12.50", rows[1][5])
        assertEquals("split with Ana", rows[1][7])
    }

    @Test
    fun `a sub-unit negative keeps its sign`() {
        val rows = parseCsv(ledgerCsv(listOf(txn("Parking", -50))), delimiter = ',')
        assertEquals("-0.50", rows[1][5])
        assertEquals(-50L, parseAmountToMinor(rows[1][5]))
    }

    @Test
    fun `amounts read back as the same minor units`() {
        listOf(-1L, 0L, 5L, -99L, 100L, -123_456_78L).forEach { minor ->
            assertEquals(minor, parseAmountToMinor(minorToDecimal(minor)))
        }
    }

    @Test
    fun `newest row comes first`() {
        val older = txn("Older", -100).copy(id = "a", date = LocalDate.of(2026, 9, 1))
        val newer = txn("Newer", -200).copy(id = "b", date = LocalDate.of(2026, 9, 14))
        val rows = parseCsv(ledgerCsv(listOf(older, newer)), delimiter = ',')
        assertEquals("Newer", rows[1][2])
        assertEquals("Older", rows[2][2])
    }
}
