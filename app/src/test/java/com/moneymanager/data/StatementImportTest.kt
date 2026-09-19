package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The statement importer reads numbers written by banks and turns them into the ledger's own
 * money. A quiet mistake here does not look like a bug -- it looks like a transaction -- so every
 * amount format worth supporting is pinned down here.
 */
class StatementImportTest {

    @Test
    fun `plain decimals`() {
        assertEquals(123_45L, parseAmountToMinor("123.45"))
        assertEquals(123_40L, parseAmountToMinor("123.4"))
        assertEquals(123_00L, parseAmountToMinor("123"))
        assertEquals(5L, parseAmountToMinor("0.05"))
    }

    @Test
    fun `either convention, decided by which separator comes last`() {
        // The same number, written by a British bank and a German one.
        assertEquals(1_234_56L, parseAmountToMinor("1,234.56"))
        assertEquals(1_234_56L, parseAmountToMinor("1.234,56"))
        assertEquals(1_234_567_89L, parseAmountToMinor("1,234,567.89"))
        assertEquals(1_234_567_89L, parseAmountToMinor("1.234.567,89"))
    }

    @Test
    fun `a lone group of three is thousands, not cents`() {
        // "1,234" is one thousand two hundred and thirty-four, not one point two three four.
        assertEquals(1_234_00L, parseAmountToMinor("1,234"))
        assertEquals(1_234_00L, parseAmountToMinor("1.234"))
        // But two digits after a separator are cents.
        assertEquals(1_23L, parseAmountToMinor("1.23"))
        assertEquals(1_23L, parseAmountToMinor("1,23"))
    }

    @Test
    fun `every way a bank writes a negative`() {
        assertEquals(-45_00L, parseAmountToMinor("-45.00"))
        assertEquals(-45_00L, parseAmountToMinor("45.00-"))
        assertEquals(-45_00L, parseAmountToMinor("(45.00)"))
        assertEquals(45_00L, parseAmountToMinor("+45.00"))
    }

    @Test
    fun `currency symbols and spacing are noise`() {
        assertEquals(1_284_12L, parseAmountToMinor("$1,284.12"))
        assertEquals(1_284_12L, parseAmountToMinor("  1 284.12  "))
        assertEquals(-99_99L, parseAmountToMinor("-€99,99"))
    }

    @Test
    fun `unreadable input is null, never zero`() {
        // Zero would be written to the ledger as a real transaction of no value. Null makes the
        // caller stop and ask.
        assertNull(parseAmountToMinor(""))
        assertNull(parseAmountToMinor("   "))
        assertNull(parseAmountToMinor("pending"))
        assertNull(parseAmountToMinor("--"))
    }

    @Test
    fun `quoted fields keep their commas`() {
        val csv = "Date,Description,Amount\n2026-09-01,\"SUPERINDO, JAKARTA\",-48.16\n"
        val rows = parseCsv(csv)
        assertEquals(2, rows.size)
        assertEquals("SUPERINDO, JAKARTA", rows[1][1])
        assertEquals(3, rows[1].size)
    }

    @Test
    fun `doubled quotes are one quote`() {
        val rows = parseCsv("a,\"he said \"\"hi\"\"\",c\n")
        assertEquals("he said \"hi\"", rows[0][1])
    }

    @Test
    fun `semicolon exports are detected`() {
        val csv = "Datum;Beschreibung;Betrag\n01.09.2026;MIETE;-620,00\n"
        assertEquals(';', detectDelimiter(csv))
        val rows = parseCsv(csv)
        assertEquals("-620,00", rows[1][2])
    }

    @Test
    fun `a signed amount column reads end to end`() {
        val csv = "Date,Description,Amount\n2026-09-01,RENT,-620.00\n2026-09-02,SALARY,2450.00\n"
        val rows = parseCsv(csv)
        val map = inferColumns(rows.first(), rows.drop(1))
        val read = readRows(rows.drop(1), map)

        assertEquals(2, read.size)
        assertEquals(LocalDate.of(2026, 9, 1), read[0].date)
        assertEquals(-620_00L, read[0].amountMinor)
        assertEquals(2_450_00L, read[1].amountMinor)
    }

    @Test
    fun `separate debit and credit columns get their signs from the column`() {
        val csv = "Date,Details,Money out,Money in\n2026-09-01,RENT,620.00,\n2026-09-02,SALARY,,2450.00\n"
        val rows = parseCsv(csv)
        val map = inferColumns(rows.first(), rows.drop(1))
        val read = readRows(rows.drop(1), map)

        assertEquals(2, read.size)
        assertEquals(-620_00L, read[0].amountMinor)
        assertEquals(2_450_00L, read[1].amountMinor)
    }

    @Test
    fun `rows that cannot be read are dropped, not guessed at`() {
        val csv = "Date,Description,Amount\n2026-09-01,RENT,-620.00\nnot a date,JUNK,abc\n"
        val rows = parseCsv(csv)
        val map = inferColumns(rows.first(), rows.drop(1))
        assertEquals(1, readRows(rows.drop(1), map).size)
    }

    @Test
    fun `a posting a day late still matches its transaction`() {
        val existing = listOf(
            Txn(
                id = "t1", merchant = "SHELL", categoryId = "transport", accountId = "a",
                amountMinor = -34_00L, date = LocalDate.of(2026, 9, 10),
                time = java.time.LocalTime.NOON,
            )
        )
        val rows = listOf(
            StatementRow(LocalDate.of(2026, 9, 12), "SHELL", -34_00L, emptyList()),
            StatementRow(LocalDate.of(2026, 9, 12), "GRAMEDIA", -18_40L, emptyList()),
        )
        val matched = matchDuplicates(rows, existing)

        // Two days late, same amount: the same purchase.
        assertEquals("t1", matched[0].duplicateOf?.id)
        assertTrue("a duplicate is excluded by default", !matched[0].include)

        // Nothing like it in the ledger: imported.
        assertNull(matched[1].duplicateOf)
        assertTrue("a new row is included by default", matched[1].include)
    }

    @Test
    fun `a matching amount a fortnight away is a different purchase`() {
        val existing = listOf(
            Txn(
                id = "t1", merchant = "SHELL", categoryId = "transport", accountId = "a",
                amountMinor = -34_00L, date = LocalDate.of(2026, 8, 20),
                time = java.time.LocalTime.NOON,
            )
        )
        val rows = listOf(StatementRow(LocalDate.of(2026, 9, 12), "SHELL", -34_00L, emptyList()))
        assertNull(matchDuplicates(rows, existing)[0].duplicateOf)
    }
}
