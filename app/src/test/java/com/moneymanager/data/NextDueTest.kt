package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/*
 * Advancing a bill is the one write in the app that changes a date rather than an amount, and a
 * wrong date here is money charged on a day the user was not expecting it.
 */
class NextDueTest {

    private fun on(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    @Test
    fun `paying on the day moves one period`() {
        assertEquals(
            on(2026, 10, 15),
            nextDue(on(2026, 9, 15), Recurrence.Monthly, paidOn = on(2026, 9, 15)),
        )
    }

    @Test
    fun `paying early still moves the bill forward`() {
        // Rent paid three days early is this month's rent. Leaving the due date where it was
        // would keep the bill listed as owed and the reminder armed.
        assertEquals(
            on(2026, 10, 15),
            nextDue(on(2026, 9, 15), Recurrence.Monthly, paidOn = on(2026, 9, 12)),
        )
    }

    @Test
    fun `four months late lands on the next real due date`() {
        assertEquals(
            on(2027, 1, 15),
            nextDue(on(2026, 9, 15), Recurrence.Monthly, paidOn = on(2026, 12, 20)),
        )
    }

    @Test
    fun `a month end does not drift`() {
        // Stepping one month at a time: 31 Jan to 28 Feb, then 28 Feb to 28 Mar, and the bill is
        // stuck on the 28th for good. Counting from the anchor keeps the 31st.
        val anchor = on(2026, 1, 31)
        assertEquals(on(2026, 2, 28), nextDue(anchor, Recurrence.Monthly, on(2026, 1, 31)))
        assertEquals(on(2026, 3, 31), nextDue(anchor, Recurrence.Monthly, on(2026, 3, 1)))
        assertEquals(on(2026, 4, 30), nextDue(anchor, Recurrence.Monthly, on(2026, 4, 1)))
        assertEquals(on(2026, 5, 31), nextDue(anchor, Recurrence.Monthly, on(2026, 5, 1)))
    }

    @Test
    fun `a leap day falls back and comes back`() {
        val anchor = on(2028, 2, 29)
        assertEquals(on(2029, 2, 28), nextDue(anchor, Recurrence.Yearly, on(2028, 2, 29)))
        assertEquals(on(2032, 2, 29), nextDue(anchor, Recurrence.Yearly, on(2031, 6, 1)))
    }

    @Test
    fun `weekly quarterly and yearly each move their own period`() {
        val due = on(2026, 9, 15)
        assertEquals(on(2026, 9, 22), nextDue(due, Recurrence.Weekly, due))
        assertEquals(on(2026, 12, 15), nextDue(due, Recurrence.Quarterly, due))
        assertEquals(on(2027, 9, 15), nextDue(due, Recurrence.Yearly, due))
    }

    @Test
    fun `a weekly bill a year overdue catches up rather than looping forever`() {
        val due = on(2025, 9, 15)
        val next = nextDue(due, Recurrence.Weekly, paidOn = on(2026, 9, 19))
        assertEquals(on(2026, 9, 21), next)
        assertEquals(java.time.DayOfWeek.MONDAY, next.dayOfWeek)
    }
}
