package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/*
 * LedgerState is where every headline figure in the app comes from, and none of it is stored --
 * each total is recomputed from the lists it is handed. That makes it the one place a summary can
 * quietly stop agreeing with the ledger it summarises, and the one place worth testing hardest.
 *
 * All of it is pure, so none of this needs Room, a device, or a clock that can be frozen: the
 * dates are built relative to today, which is the same contract the screens read under.
 */
class LedgerStateTest {

    @Before
    fun seedLookups() {
        Categories.snapshot = listOf(
            Category("food", "Food", "restaurant"),
            Category("transport", "Transport", "transport"),
        )
        Accounts.snapshot = listOf(Account("cash", "Cash", AccountKind.Cash, 0))
    }

    private fun spend(
        minor: Long,
        on: LocalDate = today,
        category: String = "food",
        merchant: String = "Anywhere",
        splits: List<Split> = emptyList(),
    ) = Txn(
        id = "$merchant-$on-$minor",
        merchant = merchant,
        categoryId = category,
        accountId = "cash",
        amountMinor = -minor,
        date = on,
        time = LocalTime.NOON,
        flow = Flow.Out,
        splits = splits,
    )

    private fun income(minor: Long, on: LocalDate = today) = Txn(
        id = "in-$on-$minor",
        merchant = "Work",
        categoryId = "income",
        accountId = "cash",
        amountMinor = minor,
        date = on,
        time = LocalTime.NOON,
        flow = Flow.In,
    )

    private fun account(id: String, kind: AccountKind, balance: Long) =
        Account(id, id, kind, balance)

    // --- Safe to spend ---------------------------------------------------------------------

    @Test
    fun `safe to spend is the budget less what is gone and what is owed`() {
        val state = LedgerState(
            budgets = listOf(Budget("food", 400_00L, 150_00L)),
            bills = listOf(
                Bill("b1", "Rent", 100_00L, today.plusDays(1), Recurrence.Monthly, "cash"),
            ),
        )
        assertEquals(400_00L, state.budgetedMinor)
        assertEquals(150_00L, state.spentMinor)
        assertEquals(100_00L, state.committedMinor)
        assertEquals(150_00L, state.safeToSpendMinor)
    }

    @Test
    fun `safe to spend never goes negative`() {
        val state = LedgerState(budgets = listOf(Budget("food", 100_00L, 500_00L)))
        assertEquals(0L, state.safeToSpendMinor)
        assertEquals(0f, state.safeFraction, 0.0001f)
    }

    @Test
    fun `a bill already paid this month is not still committed`() {
        // Due yesterday means it has been paid and rolled forward, or it is overdue; either way
        // it is not money this month's remaining budget still has to cover.
        val state = LedgerState(
            budgets = listOf(Budget("food", 400_00L, 0L)),
            bills = listOf(
                Bill("b1", "Gone", 100_00L, today.minusDays(1), Recurrence.Monthly, "cash"),
            ),
        )
        assertEquals(0L, state.committedMinor)
        assertEquals(400_00L, state.safeToSpendMinor)
    }

    @Test
    fun `next month's bill is not this month's problem`() {
        val nextMonth = today.withDayOfMonth(1).plusMonths(1).plusDays(3)
        val state = LedgerState(
            budgets = listOf(Budget("food", 400_00L, 0L)),
            bills = listOf(Bill("b1", "Later", 100_00L, nextMonth, Recurrence.Monthly, "cash")),
        )
        assertEquals(0L, state.committedMinor)
    }

    @Test
    fun `no budget means no waterline`() {
        val state = LedgerState(transactions = listOf(spend(50_00L)))
        assertEquals(0L, state.budgetedMinor)
        assertEquals(0L, state.safeToSpendMinor)
        assertEquals(0f, state.safeFraction, 0.0001f)
    }

    // --- Net worth -------------------------------------------------------------------------

    @Test
    fun `net worth counts a card balance and a debt as owed`() {
        val state = LedgerState(
            accounts = listOf(
                account("cash", AccountKind.Cash, 500_00L),
                account("savings", AccountKind.Savings, 2_000_00L),
                account("card", AccountKind.Card, -300_00L),
            ),
            debts = listOf(Debt("d1", "Loan", 1_000_00L, 0, 50_00L)),
        )
        assertEquals(2_500_00L - 300_00L - 1_000_00L, state.netWorthMinor)
    }

    @Test
    fun `net worth counts what you own, not just what is in the accounts`() {
        // The point of tracking a possession: two thousand spent on a laptop is not two thousand
        // gone, it is two thousand turned into a thing.
        val state = LedgerState(
            accounts = listOf(account("cash", AccountKind.Cash, 500_00L)),
            assets = listOf(
                Asset("a1", "Laptop", 2_000_00L, today, usefulLifeMonths = null),
            ),
        )
        assertEquals(2_500_00L, state.netWorthMinor)
        assertEquals(2_000_00L, state.assetsMinor)
    }

    @Test
    fun `a possession past its useful life stops propping up net worth`() {
        val state = LedgerState(
            accounts = listOf(account("cash", AccountKind.Cash, 500_00L)),
            assets = listOf(
                Asset("a1", "Old laptop", 2_000_00L, today.minusYears(5), usefulLifeMonths = 36),
            ),
        )
        assertEquals(500_00L, state.netWorthMinor)
    }

    @Test
    fun `net worth history does not credit a possession bought after that month`() {
        // Bought today, so every earlier point on the twelve-month line must exclude it.
        val state = LedgerState(
            accounts = listOf(account("cash", AccountKind.Cash, 500_00L)),
            assets = listOf(Asset("a1", "Laptop", 2_000_00L, today, usefulLifeMonths = null)),
        )
        val line = state.netWorthAssets
        assertEquals(12, line.size)
        assertEquals(2_500_00L, line.last())
        // The month before it was bought knows nothing about it.
        assertEquals(500_00L, line[line.size - 2])
    }

    @Test
    fun `liquid money excludes savings and anything overdrawn`() {
        val state = LedgerState(
            accounts = listOf(
                account("cash", AccountKind.Cash, 500_00L),
                account("savings", AccountKind.Savings, 2_000_00L),
                account("card", AccountKind.Card, -300_00L),
                account("bank", AccountKind.Bank, 250_00L),
            ),
        )
        assertEquals(750_00L, state.liquidMinor)
    }

    // --- Categories and splits --------------------------------------------------------------

    @Test
    fun `a split lands on its own categories not on the parent row`() {
        val row = spend(
            100_00L,
            category = "food",
            splits = listOf(Split("food", -60_00L), Split("transport", -40_00L)),
        )
        val byCategory = LedgerState(transactions = listOf(row)).spendByCategory().toMap()
        assertEquals(60_00L, byCategory[Categories["food"]])
        assertEquals(40_00L, byCategory[Categories["transport"]])
    }

    @Test
    fun `income is never spending`() {
        val state = LedgerState(transactions = listOf(income(500_00L), spend(20_00L)))
        assertEquals(listOf(Categories["food"] to 20_00L), state.spendByCategory())
    }

    @Test
    fun `top merchants are ranked by money out and capped at five`() {
        val rows = listOf(
            spend(10_00L, merchant = "A"), spend(90_00L, merchant = "B"),
            spend(30_00L, merchant = "C"), spend(40_00L, merchant = "D"),
            spend(50_00L, merchant = "E"), spend(60_00L, merchant = "F"),
            spend(5_00L, merchant = "B"),
        )
        val top = LedgerState(transactions = rows).topMerchants
        assertEquals(5, top.size)
        assertEquals("B", top.first().name)
        assertEquals(95_00L, top.first().totalMinor)
        assertEquals(2, top.first().count)
    }

    // --- The streak -------------------------------------------------------------------------

    @Test
    fun `an unlogged today does not break yesterday's streak`() {
        val rows = (1..3).map { spend(10_00L, on = today.minusDays(it.toLong())) }
        assertEquals(3, LedgerState(allTransactions = rows).loggingStreakDays)
    }

    @Test
    fun `a missing day ends the streak there`() {
        val rows = listOf(
            spend(10_00L, on = today),
            spend(10_00L, on = today.minusDays(1)),
            // nothing on day 2
            spend(10_00L, on = today.minusDays(3)),
        )
        assertEquals(2, LedgerState(allTransactions = rows).loggingStreakDays)
    }

    @Test
    fun `an empty ledger has no streak`() {
        assertEquals(0, LedgerState().loggingStreakDays)
        assertFalse(LedgerState().hasAnything)
    }

    // --- Badges are earned, not granted -------------------------------------------------------

    @Test
    fun `the fifty-transaction badge tracks the real count`() {
        val forty = (1..40).map { spend(1_00L, on = today.minusDays((it % 28).toLong())) }
        val badge = LedgerState(allTransactions = forty).badges.first { it.id == "ba1" }
        assertEquals(0.8f, badge.progress, 0.0001f)
        assertEquals(null, badge.earnedOn)

        val sixty = (1..60).map { spend(1_00L, on = today.minusDays((it % 28).toLong())) }
        val earned = LedgerState(allTransactions = sixty).badges.first { it.id == "ba1" }
        assertEquals(1f, earned.progress, 0.0001f)
        assertTrue(earned.earnedOn != null)
    }

    @Test
    fun `debt-free is not awarded to an empty ledger`() {
        // No debts and no accounts is the state of a fresh install, not an achievement.
        val badge = LedgerState().badges.first { it.id == "ba4" }
        assertEquals(null, badge.earnedOn)
    }

    // --- One currency out ---------------------------------------------------------------------

    /** USD base. 0.92 EUR and 16,000 IDR to the dollar; nothing published for KRW. */
    private fun usdBase() = Conversion(
        base = "USD",
        enabled = true,
        rates = Rates(
            base = "USD",
            date = LocalDate.of(2026, 9, 19),
            perBaseMicros = mapOf("EUR" to 920_000L, "IDR" to 16_000_000_000L),
            fetchedAtEpochSecond = 1_000L,
        ),
    )

    private fun foreign(id: String, kind: AccountKind, balance: Long, currency: String) =
        Account(id, id, kind, balance, currency)

    @Test
    fun `an account in another currency is converted before it is added`() {
        val state = LedgerState(
            accounts = listOf(
                foreign("usd", AccountKind.Bank, 100_00L, "USD"),
                foreign("eur", AccountKind.Bank, 92_00L, "EUR"),
            ),
            conversion = usdBase(),
        )
        // 92.00 EUR at 0.92 to the dollar is 100.00 USD.
        assertEquals(200_00L, state.netWorthMinor)
        assertEquals(200_00L, state.liquidMinor)
        assertTrue(state.totalsAreComplete)
    }

    @Test
    fun `a currency with no rate is left out of the total and named`() {
        val state = LedgerState(
            accounts = listOf(
                foreign("usd", AccountKind.Bank, 100_00L, "USD"),
                foreign("krw", AccountKind.Bank, 500_000_00L, "KRW"),
            ),
            conversion = usdBase(),
        )
        // Half a million won added in raw would read as half a million dollars. Better short.
        assertEquals(100_00L, state.netWorthMinor)
        assertEquals(listOf("KRW"), state.unconvertible)
        assertFalse(state.totalsAreComplete)
    }

    @Test
    fun `spending abroad counts against the budget in the base`() {
        val eurRow = spend(46_00L, category = "food").copy(id = "eur", currency = "EUR")
        val state = LedgerState(
            transactions = listOf(eurRow),
            allTransactions = listOf(eurRow),
            conversion = usdBase(),
        )
        assertEquals(50_00L, state.spendByCategory().single().second)
        assertEquals(50_00L, state.monthlyOut.last())
        assertEquals(50_00L, state.topMerchants.single().totalMinor)
    }

    @Test
    fun `switching the base re-reads the same ledger`() {
        val accounts = listOf(foreign("eur", AccountKind.Bank, 92_00L, "EUR"))
        val inUsd = LedgerState(accounts = accounts, conversion = usdBase())
        assertEquals(100_00L, inUsd.netWorthMinor)

        val eurBase = Conversion(
            base = "EUR",
            enabled = true,
            rates = Rates("EUR", LocalDate.of(2026, 9, 19), mapOf("USD" to 1_086_956L), 1_000L),
        )
        val inEur = LedgerState(accounts = accounts, conversion = eurBase)
        assertEquals(92_00L, inEur.netWorthMinor)
        assertEquals("EUR", inEur.baseCurrency)
    }

    @Test
    fun `single currency needs no rates at all`() {
        // The ordinary case: multi-currency off, nothing fetched, everything still totals.
        val state = LedgerState(
            accounts = listOf(foreign("usd", AccountKind.Bank, 100_00L, "USD")),
            transactions = listOf(spend(20_00L)),
        )
        assertEquals(100_00L, state.netWorthMinor)
        assertEquals(20_00L, state.spendByCategory().single().second)
        assertTrue(state.totalsAreComplete)
    }

    // --- Bills, goals and debts borrow their account's currency --------------------------------

    @Test
    fun `a bill takes the currency of the account it is paid from`() {
        Accounts.snapshot = listOf(
            Account("usd", "Everyday", AccountKind.Bank, 0, "USD"),
            Account("eur", "Berlin", AccountKind.Bank, 0, "EUR"),
        )
        val here = Bill("b1", "Rent", 100_00L, today, Recurrence.Monthly, "usd")
        val there = Bill("b2", "Miete", 100_00L, today, Recurrence.Monthly, "eur")
        assertEquals("USD", here.currency)
        assertEquals("EUR", there.currency)
    }

    @Test
    fun `a bill whose account is gone falls back to the display base rather than to dollars`() {
        Accounts.snapshot = emptyList()
        Money.base = "GBP"
        try {
            assertEquals("GBP", Bill("b", "Orphan", 1L, today, Recurrence.Monthly, "missing").currency)
        } finally {
            Money.base = "USD"
        }
    }

    @Test
    fun `a foreign bill is converted before it is committed against the month`() {
        Accounts.snapshot = listOf(Account("eur", "Berlin", AccountKind.Bank, 0, "EUR"))
        val state = LedgerState(
            budgets = listOf(Budget("food", 400_00L, 0L)),
            bills = listOf(
                Bill("b1", "Miete", 92_00L, today.plusDays(1), Recurrence.Monthly, "eur"),
            ),
            conversion = usdBase(),
        )
        assertEquals(100_00L, state.committedMinor)
        assertEquals(300_00L, state.safeToSpendMinor)
    }

    @Test
    fun `a foreign debt is converted before it is owed`() {
        Accounts.snapshot = listOf(Account("eur", "Berlin", AccountKind.Bank, 0, "EUR"))
        val state = LedgerState(
            debts = listOf(Debt("d1", "Kredit", 92_00L, 0, 10_00L, "eur")),
            conversion = usdBase(),
        )
        assertEquals(-100_00L, state.netWorthMinor)
    }

    @Test
    fun `a bill in a currency with no rate is named rather than dropped in silence`() {
        Accounts.snapshot = listOf(Account("krw", "Seoul", AccountKind.Bank, 0, "KRW"))
        val state = LedgerState(
            bills = listOf(Bill("b1", "Rent", 500_000_00L, today, Recurrence.Monthly, "krw")),
            conversion = usdBase(),
        )
        assertEquals(0L, state.committedMinor)
        assertEquals(listOf("KRW"), state.unconvertible)
    }

    // --- Series -------------------------------------------------------------------------------

    @Test
    fun `the six-month series is six months ending with this one`() {
        val state = LedgerState(allTransactions = listOf(spend(30_00L), income(70_00L)))
        assertEquals(6, state.monthLabels.size)
        assertEquals(6, state.monthlyIn.size)
        assertEquals(6, state.monthlyOut.size)
        assertEquals(70_00L, state.monthlyIn.last())
        // Money out is reported positive, whatever sign it carries in the ledger.
        assertEquals(30_00L, state.monthlyOut.last())
    }

    @Test
    fun `the net worth line is twelve points ending at today`() {
        val state = LedgerState(
            accounts = listOf(account("cash", AccountKind.Cash, 500_00L)),
            allTransactions = listOf(spend(100_00L)),
        )
        assertEquals(12, state.netWorthAssets.size)
        assertEquals(500_00L, state.netWorthAssets.last())
        // Walking this month back out restores the hundred that was spent in it.
        assertEquals(600_00L, state.netWorthAssets[10])
    }
}
