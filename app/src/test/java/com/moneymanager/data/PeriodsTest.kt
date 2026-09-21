package com.moneymanager.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * Where the month begins decides every headline figure: safe-to-spend, days left, the pace tick
 * on every budget bar, which bills count as committed. A cycle that opens on the wrong day is
 * not a cosmetic bug -- it moves the number the user is deciding against.
 *
 * These cover the pure date maths. The payday case is derived from real income rows and is
 * covered in RepositoryWriteTest, where there is a ledger to derive it from.
 */
class PeriodsTest {

    private fun on(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    @After
    fun reset() {
        Periods.monthStartDay = 1
        Periods.weekStart = DayOfWeek.MONDAY
    }

    @Test
    fun `the default cycle is the calendar month`() {
        Periods.monthStartDay = 1
        assertEquals(on(2026, 9, 1), cycleStartOn(on(2026, 9, 21)))
        assertEquals(on(2026, 9, 1), cycleStartOn(on(2026, 9, 1)))
        assertEquals(on(2026, 9, 1), cycleStartOn(on(2026, 9, 30)))
    }

    @Test
    fun `a day before the opening belongs to the previous cycle`() {
        Periods.monthStartDay = 25
        // The 24th is still last month's cycle: the one that opened on 25 August.
        assertEquals(on(2026, 8, 25), cycleStartOn(on(2026, 9, 24)))
        assertEquals(on(2026, 9, 25), cycleStartOn(on(2026, 9, 25)))
        assertEquals(on(2026, 9, 25), cycleStartOn(on(2026, 10, 3)))
    }

    @Test
    fun `a cycle that wraps into January takes the right year`() {
        Periods.monthStartDay = 25
        assertEquals(on(2025, 12, 25), cycleStartOn(on(2026, 1, 4)))
    }

    @Test
    fun `an opening past the end of February is clamped rather than skipped`() {
        // A derived payday on the 31st has no 31 February to open on. Clamping to the 28th
        // opens the cycle early; skipping the month would leave February with no cycle at all.
        Periods.monthStartDay = Periods.PAYDAY
        Periods.paydayDay = 31
        assertEquals(28, Periods.startDay)
        assertEquals(on(2026, 2, 28), cycleStartOn(on(2026, 3, 1)))
    }

    @Test
    fun `a leap February still opens on its own day`() {
        Periods.monthStartDay = 15
        assertEquals(on(2024, 2, 15), cycleStartOn(on(2024, 2, 29)))
    }

    @Test
    fun `the week starts where the user says it does`() {
        Periods.weekStart = DayOfWeek.MONDAY
        assertEquals(0, weekColumnOf(on(2026, 9, 21), DayOfWeek.MONDAY))
        assertEquals(6, weekColumnOf(on(2026, 9, 27), DayOfWeek.MONDAY))

        // The same Sunday is the last column of a Monday week and the first of a Sunday week.
        assertEquals(0, weekColumnOf(on(2026, 9, 27), DayOfWeek.SUNDAY))
        assertEquals(1, weekColumnOf(on(2026, 9, 21), DayOfWeek.SUNDAY))

        assertEquals(0, weekColumnOf(on(2026, 9, 26), DayOfWeek.SATURDAY))
    }

    @Test
    fun `the calendar header is rotated, not relabelled`() {
        assertEquals(
            listOf("M", "T", "W", "T", "F", "S", "S"),
            weekdayInitials(DayOfWeek.MONDAY),
        )
        assertEquals(
            listOf("S", "M", "T", "W", "T", "F", "S"),
            weekdayInitials(DayOfWeek.SUNDAY),
        )
        assertEquals(
            listOf("S", "S", "M", "T", "W", "T", "F"),
            weekdayInitials(DayOfWeek.SATURDAY),
        )
    }

    @Test
    fun `a cycle is one month long however long that month is`() {
        Periods.monthStartDay = 25
        // 25 January to 24 February inclusive is 31 days; 25 February to 24 March is 28.
        assertEquals(31, daysBetweenCycles(on(2026, 1, 25)))
        assertEquals(28, daysBetweenCycles(on(2026, 2, 25)))
    }

    /** What [daysInMonth] computes, pinned to a date instead of to today. */
    private fun daysBetweenCycles(start: LocalDate): Int =
        java.time.temporal.ChronoUnit.DAYS.between(start, start.plusMonths(1)).toInt()

    @Test
    fun `a date maps to its position in the cycle`() {
        Periods.monthStartDay = 25
        // cycleDayOf is relative to the cycle containing today, so this checks the arithmetic
        // it uses rather than the live clock.
        val start = cycleStartOn(on(2026, 9, 30))
        assertEquals(on(2026, 9, 25), start)
        assertEquals(6, java.time.temporal.ChronoUnit.DAYS.between(start, on(2026, 9, 30)) + 1)
    }
}
