package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/*
 * Challenges are derived on every read, so the thing worth testing is the derivation: that a run
 * breaks where it should, that today cannot break it early, and that the money figure comes from
 * this person's own history rather than from a constant.
 */
class ChallengesTest {

    private val saturday: LocalDate =
        LocalDate.of(2026, 9, 1).with(TemporalAdjusters.next(DayOfWeek.SATURDAY))

    private fun out(date: LocalDate, minor: Long) = Txn(
        id = "$date-$minor",
        merchant = "Anywhere",
        categoryId = "food",
        accountId = "cash",
        amountMinor = -minor,
        date = date,
        time = LocalTime.NOON,
        flow = Flow.Out,
    )

    /** The ledger's first day, which is how far back either challenge is allowed to look. */
    private fun startedAt(vararg rows: Txn) = rows.minOfOrNull { it.date }

    // --- the weekend ------------------------------------------------------------------------

    @Test
    fun `weekend challenge only exists on a weekend`() {
        val spend = spendByDay(emptyList())
        assertNull(weekendChallenge(spend, saturday.minusDays(1), null))   // Friday
        assertNull(weekendChallenge(spend, saturday.plusDays(2), null))    // Monday
        assertEquals(1, weekendChallenge(spend, saturday, null)?.dayOf)
        assertEquals(2, weekendChallenge(spend, saturday.plusDays(1), null)?.dayOf)
    }

    @Test
    fun `saving is the median of past weekends minus this one`() {
        // Four previous weekends costing 40, 60, 60 and 200. The window stops at the first
        // transaction the ledger holds, so the median is of those four and not of eight.
        val history = listOf(40_00L, 60_00L, 60_00L, 200_00L).flatMapIndexed { i, amount ->
            listOf(out(saturday.minusWeeks((i + 1).toLong()), amount))
        }
        val challenge = weekendChallenge(spendByDay(history), saturday, startedAt(*history.toTypedArray()))!!
        assertEquals(60_00L, challenge.savedMinor)
        assertTrue(challenge.blurb.contains("Nothing out"))
    }

    @Test
    fun `spending this weekend eats into the saving and never goes negative`() {
        val history = (1..8).map { out(saturday.minusWeeks(it.toLong()), 50_00L) }
        val began = startedAt(*history.toTypedArray())

        val spent = spendByDay(history + out(saturday, 30_00L))
        assertEquals(20_00L, weekendChallenge(spent, saturday, began)!!.savedMinor)

        val blownOut = spendByDay(history + out(saturday, 90_00L))
        assertEquals(0L, weekendChallenge(blownOut, saturday, began)!!.savedMinor)
    }

    @Test
    fun `a fresh install claims nothing`() {
        assertEquals(0L, weekendChallenge(spendByDay(emptyList()), saturday, null)!!.savedMinor)
    }

    // --- the lean run -----------------------------------------------------------------------

    @Test
    fun `no budget means no pace to beat`() {
        val row = out(saturday, 1_00L)
        assertNull(leanRunChallenge(spendByDay(listOf(row)), saturday, 0L, row.date))
    }

    @Test
    fun `an empty ledger is not a lean week`() {
        // Nothing logged is not the same as nothing spent. Installing the app this morning must
        // not hand out a week of discipline.
        assertNull(leanRunChallenge(spendByDay(emptyList()), saturday, 20_00L, null))
    }

    @Test
    fun `the run stops where the ledger starts`() {
        val first = out(saturday.minusDays(2), 5_00L)
        val challenge = leanRunChallenge(spendByDay(listOf(first)), saturday, 20_00L, first.date)!!
        assertEquals(3, challenge.dayOf)
        // Two silent days at full pace, plus the day that cost 5.
        assertEquals(20_00L + 20_00L + 15_00L, challenge.savedMinor)
    }

    @Test
    fun `the run breaks on the first day over pace`() {
        val pace = 20_00L
        val rows = listOf(
            out(saturday, 10_00L),                 // today, under
            out(saturday.minusDays(1), 19_99L),    // under
            out(saturday.minusDays(2), 20_01L),    // over: the run stops here
            out(saturday.minusDays(3), 1_00L),     // not counted
        )
        val challenge = leanRunChallenge(spendByDay(rows), saturday, pace, startedAt(*rows.toTypedArray()))!!
        assertEquals(2, challenge.dayOf)
        assertEquals(7, challenge.days)
        // (2000 - 1000) + (2000 - 1999)
        assertEquals(10_01L, challenge.savedMinor)
    }

    @Test
    fun `a day exactly at pace still counts`() {
        val rows = listOf(out(saturday, 20_00L))
        assertEquals(1, leanRunChallenge(spendByDay(rows), saturday, 20_00L, saturday)!!.dayOf)
    }

    @Test
    fun `today over pace ends the run immediately`() {
        val rows = listOf(out(saturday, 99_00L), out(saturday.minusDays(1), 1_00L))
        assertNull(leanRunChallenge(spendByDay(rows), saturday, 20_00L, startedAt(*rows.toTypedArray())))
    }

    // --- money in does not count as spending ------------------------------------------------

    @Test
    fun `income is not spending`() {
        val paycheck = Txn(
            id = "in",
            merchant = "Work",
            categoryId = "income",
            accountId = "cash",
            amountMinor = 500_00L,
            date = saturday,
            time = LocalTime.NOON,
            flow = Flow.In,
        )
        val challenge = leanRunChallenge(spendByDay(listOf(paycheck)), saturday, 20_00L, paycheck.date)!!
        assertEquals(1, challenge.dayOf)
    }

    // --- the two together --------------------------------------------------------------------

    @Test
    fun `both run on a quiet Saturday with a budget and some history`() {
        val rows = listOf(out(saturday.minusWeeks(3), 10_00L))
        val ids = challengesFor(rows, saturday, 20_00L).map { it.id }
        assertEquals(listOf("ch_weekend", "ch_lean"), ids)
    }

    @Test
    fun `midweek with no budget produces nothing`() {
        assertEquals(emptyList<Challenge>(), challengesFor(emptyList(), saturday.plusDays(3), 0L))
    }

    // --- The 52-week ladder --------------------------------------------------------------------

    /** A transfer landing in a savings account, which is what a goal contribution writes. */
    private fun intoSavings(date: LocalDate, minor: Long) = Txn(
        id = "save-$date-$minor",
        merchant = "Savings",
        categoryId = "savings",
        accountId = "savings",
        amountMinor = minor,
        date = date,
        time = LocalTime.NOON,
        flow = Flow.Transfer,
    )

    private fun withSavingsAccount(block: () -> Unit) {
        val before = Accounts.snapshot
        Accounts.snapshot = listOf(
            Account("savings", "Savings", AccountKind.Savings, 0L),
            Account("cash", "Cash", AccountKind.Cash, 0L),
        )
        try { block() } finally { Accounts.snapshot = before }
    }

    /** The Monday of ISO week [week] in [year], so a date lands in a known week. */
    private fun inWeek(year: Int, week: Int): LocalDate =
        LocalDate.of(year, 1, 4)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks((week - 1).toLong())

    @Test
    fun `without a savings account there is no ladder to climb`() {
        // Nothing to measure against, so the challenge does not appear rather than reporting zero.
        val rows = listOf(out(saturday, 10_00L))
        assertNull(fiftyTwoWeekChallenge(savedByIsoWeek(rows, saturday.year), saturday, 100L))
    }

    @Test
    fun `only money landing in savings counts toward it`() = withSavingsAccount {
        val week = inWeek(2026, 3)
        val rows = listOf(out(week, 40_00L), intoSavings(week, 6_00L))
        assertEquals(mapOf(3 to 6_00L), savedByIsoWeek(rows, 2026))
    }

    @Test
    fun `money taken back out of savings nets off against the week it left in`() = withSavingsAccount {
        val week = inWeek(2026, 3)
        val rows = listOf(intoSavings(week, 50_00L), intoSavings(week, -40_00L))
        assertEquals(mapOf(3 to 10_00L), savedByIsoWeek(rows, 2026))
    }

    @Test
    fun `the ladder asks for one plus two plus three by week three`() = withSavingsAccount {
        val today = inWeek(2026, 3).plusDays(2)
        // 1 + 2 + 3 = 6 units. Saving exactly that is on track.
        val saved = mapOf(1 to 1_00L, 2 to 2_00L, 3 to 3_00L)
        val challenge = fiftyTwoWeekChallenge(saved, today, unitMinor = 100L)!!
        assertEquals(3, challenge.dayOf)
        assertEquals(52, challenge.days)
        assertEquals(6_00L, challenge.savedMinor)
        assertTrue(challenge.blurb.contains("against"))
    }

    @Test
    fun `falling short says how far behind rather than how much was saved`() = withSavingsAccount {
        val today = inWeek(2026, 4).plusDays(1)
        // The year has asked for 1+2+3+4 = 10 units; only 2 went in.
        val challenge = fiftyTwoWeekChallenge(mapOf(1 to 2_00L), today, unitMinor = 100L)!!
        assertTrue(challenge.blurb.contains("behind"))
        assertEquals(2_00L, challenge.savedMinor)
    }

    @Test
    fun `a year with nothing saved claims nothing`() = withSavingsAccount {
        val today = inWeek(2026, 5)
        assertNull(fiftyTwoWeekChallenge(mapOf(1 to 0L), today, unitMinor = 100L))
    }

    @Test
    fun `weeks after today are not counted yet`() = withSavingsAccount {
        val today = inWeek(2026, 2).plusDays(1)
        // Week 40 money exists but the year has not reached it; it must not flatter week 2.
        val challenge = fiftyTwoWeekChallenge(mapOf(1 to 1_00L, 40 to 500_00L), today, 100L)!!
        assertEquals(1_00L, challenge.savedMinor)
    }
}
