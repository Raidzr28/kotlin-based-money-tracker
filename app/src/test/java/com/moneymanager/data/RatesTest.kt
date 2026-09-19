package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Conversion turns one real amount of money into another. Every case here is a way that could
 * silently be a cent or two wrong, which is the kind of wrong a user only finds when their
 * reconciliation refuses to balance.
 */
class RatesTest {

    private val rates = Rates(
        base = "USD",
        date = LocalDate.of(2026, 9, 15),
        perBaseMicros = mapOf(
            "EUR" to 921_000L,        // 1 USD = 0.921 EUR
            "GBP" to 784_500L,        // 1 USD = 0.7845 GBP
            "IDR" to 15_842_000_000L, // 1 USD = 15,842 IDR
            "JPY" to 147_310_000L,    // 1 USD = 147.31 JPY
        ),
        fetchedAtEpochSecond = 0L,
    )

    @Test
    fun `the base converts to itself untouched`() {
        assertEquals(1_234_56L, convertMinor(1_234_56L, "USD", "USD", rates))
        assertEquals(1_234_56L, convertMinor(1_234_56L, "EUR", "EUR", rates))
    }

    @Test
    fun `base to foreign`() {
        // 100.00 USD at 0.921 = 92.10 EUR
        assertEquals(92_10L, convertMinor(100_00L, "USD", "EUR", rates))
    }

    @Test
    fun `foreign to base`() {
        // 92.10 EUR / 0.921 = 100.00 USD
        assertEquals(100_00L, convertMinor(92_10L, "EUR", "USD", rates))
    }

    @Test
    fun `foreign to foreign goes through the base`() {
        // 92.10 EUR -> 100.00 USD -> 78.45 GBP
        assertEquals(78_45L, convertMinor(92_10L, "EUR", "GBP", rates))
    }

    @Test
    fun `a round trip comes home`() {
        val there = convertMinor(1_284_12L, "USD", "GBP", rates)!!
        val back = convertMinor(there, "GBP", "USD", rates)!!
        // Two roundings, so a cent of drift is allowed; more would mean the maths is wrong.
        assertEquals(1_284_12L.toDouble(), back.toDouble(), 1.0)
    }

    @Test
    fun `negative amounts round the same way as positive ones`() {
        // Truncating division would shave these toward zero, so spending and income would drift
        // apart over a statement.
        val out = convertMinor(-100_00L, "USD", "EUR", rates)
        val inn = convertMinor(100_00L, "USD", "EUR", rates)
        assertEquals(-(inn!!), out)
    }

    @Test
    fun `currencies with huge and tiny units survive`() {
        // 1.00 USD = 15,842.00 IDR
        assertEquals(15_842_00L, convertMinor(1_00L, "USD", "IDR", rates))
        // and back again
        assertEquals(1_00L, convertMinor(15_842_00L, "IDR", "USD", rates))
        // 1.00 USD = 147.31 JPY
        assertEquals(147_31L, convertMinor(1_00L, "USD", "JPY", rates))
    }

    @Test
    fun `an unknown currency is null, never the raw number`() {
        // Showing 40 euros as 40 dollars because a rate was missing is worse than showing nothing.
        assertNull(convertMinor(40_00L, "XYZ", "USD", rates))
        assertNull(convertMinor(40_00L, "USD", "XYZ", rates))
    }

    @Test
    fun `rounding is half away from zero in both directions`() {
        assertEquals(3L, divRound(5L, 2L))      // 2.5 -> 3
        assertEquals(-3L, divRound(-5L, 2L))    // -2.5 -> -3
        assertEquals(2L, divRound(4L, 2L))
        assertEquals(0L, divRound(1L, 0L))      // no divide-by-zero crash
    }

    @Test
    fun `a Frankfurter response parses`() {
        val body = """
            {"amount":1.0,"base":"USD","date":"2026-09-15",
             "rates":{"EUR":0.921,"GBP":0.7845,"JPY":147.31}}
        """.trimIndent()
        val parsed = parseRatesJson(body, 1_700_000_000L)!!

        assertEquals("USD", parsed.base)
        assertEquals(LocalDate.of(2026, 9, 15), parsed.date)
        assertEquals(921_000L, parsed.perBaseMicros["EUR"])
        assertEquals(784_500L, parsed.perBaseMicros["GBP"])
        assertEquals(147_310_000L, parsed.perBaseMicros["JPY"])
    }

    @Test
    fun `rubbish in gives null, not a half-built rate table`() {
        assertNull(parseRatesJson("", 0L))
        assertNull(parseRatesJson("not json", 0L))
        assertNull(parseRatesJson("""{"base":"USD"}""", 0L))
    }
}
