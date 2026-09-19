package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Real receipts, as a camera reads them: ragged spacing, missing characters, and the figure you
 * actually paid sitting among four other numbers that look just like it.
 *
 * The camera and ML Kit cannot be exercised here. The parsing can, and it is the part that decides
 * whether a scan pre-fills the right number.
 */
class ReceiptParserTest {

    @Test
    fun `the total is found by its label, not by being the biggest number`() {
        // The customer handed over 100.00 and got change. The biggest number is not the spend.
        val receipt = """
            SUPERINDO
            JL. SUDIRMAN 45
            Beras 5kg          75.000
            Minyak             25.000
            SUBTOTAL          100.000
            TOTAL              48.160
            TUNAI             100.000
            KEMBALI            51.840
        """.trimIndent()

        // Indonesian receipt: "48.160" is 48,160 rupiah, the dot being a thousands separator.
        // Stored in minor units like every other amount in the app.
        assertEquals(4_816_000L, parseReceipt(receipt).totalMinor)
    }

    @Test
    fun `subtotal is never mistaken for total`() {
        val receipt = """
            CORNER SHOP
            SUBTOTAL   40.00
            VAT         8.00
            TOTAL      48.00
        """.trimIndent()

        assertEquals(48_00L, parseReceipt(receipt).totalMinor)
    }

    @Test
    fun `cash tendered and change are not the total`() {
        val receipt = """
            WARUNG BU IDA
            TOTAL       27.50
            CASH        50.00
            CHANGE      22.50
        """.trimIndent()

        assertEquals(27_50L, parseReceipt(receipt).totalMinor)
    }

    @Test
    fun `the merchant is the first real line`() {
        val receipt = """
            TAX INVOICE
            KOPI KENANGAN
            0812-3456-7890
            TOTAL 3.20
        """.trimIndent()

        // "TAX INVOICE" is a heading and the phone number is mostly digits.
        assertEquals("KOPI KENANGAN", parseReceipt(receipt).merchant)
    }

    @Test
    fun `a receipt number is not money`() {
        val receipt = """
            GRAMEDIA
            RECEIPT 8842910387
            TOTAL 18.40
        """.trimIndent()

        assertEquals(18_40L, parseReceipt(receipt).totalMinor)
    }

    @Test
    fun `the date is read when it is there`() {
        val receipt = """
            SHELL
            14/09/2026  09:42
            TOTAL 34.00
        """.trimIndent()

        assertEquals(LocalDate.of(2026, 9, 14), parseReceipt(receipt).date)
    }

    @Test
    fun `an unlabelled receipt falls back to the largest plausible amount`() {
        val receipt = """
            PETSHOP CERIA
            Dog food     12.60
            Treats        2.00
            14.60
        """.trimIndent()

        assertEquals(14_60L, parseReceipt(receipt).totalMinor)
    }

    @Test
    fun `nothing readable gives nulls rather than invented numbers`() {
        val guess = parseReceipt("~~~~\n:::\n")
        assertNull(guess.totalMinor)
        assertNull(guess.date)
        assertTrue("a guess with no total is not confident", !guess.confident)
    }

    @Test
    fun `an empty scan does not crash`() {
        val guess = parseReceipt("")
        assertNull(guess.merchant)
        assertNull(guess.totalMinor)
        assertTrue(guess.lines.isEmpty())
    }

    @Test
    fun `every line is kept so the user can check the reading`() {
        val guess = parseReceipt("INDOMARET\nTOTAL 6.35\n")
        assertEquals(listOf("INDOMARET", "TOTAL 6.35"), guess.lines)
        assertTrue(guess.confident)
    }

    @Test
    fun `the rightmost figure on a line is the one that counts`() {
        // Receipts print quantity on the left and money on the right.
        assertEquals(12_50L, lastAmountOn("2 x 6.25        12.50"))
        assertEquals(3_20L, lastAmountOn("Kopi            3.20"))
    }

    @Test
    fun `a European receipt reads the same way`() {
        val receipt = """
            BÄCKEREI MÜLLER
            01.09.2026
            SUMME            12,80
            TOTAL            12,80
        """.trimIndent()

        val guess = parseReceipt(receipt)
        assertEquals(12_80L, guess.totalMinor)
        assertEquals(LocalDate.of(2026, 9, 1), guess.date)
    }
}
