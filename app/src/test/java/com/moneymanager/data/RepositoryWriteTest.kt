package com.moneymanager.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.moneymanager.data.db.MoneyDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/*
 * The write paths, against a real database.
 *
 * Everything else in this suite is pure and needs nothing but the JVM. These do not: they are the
 * paths that move money between accounts and currencies, and what makes them worth testing is
 * exactly what makes them hard to -- they only mean anything once Room has actually written the
 * rows and the state flow has read them back.
 *
 * Robolectric supplies Android's SQLite on the JVM, so this runs under `gradlew test` with no
 * device attached. The database is in-memory and dies with each test.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application, not MoneyApp. The real one schedules reminder work on startup, and
// WorkManager has no initializer under Robolectric -- but nothing here wants the app's object
// graph anyway: each test builds its own database and repository and throws them away.
@Config(application = android.app.Application::class)
class RepositoryWriteTest {

    private lateinit var db: MoneyDatabase
    private lateinit var repo: MoneyRepository
    private lateinit var rates: RatesStore

    /** USD base, 0.92 EUR to the dollar, nothing published for KRW. */
    private fun publishRates() {
        rates.multiCurrencyEnabled = true
        rates.baseCurrency = "USD"
        rates.setManualRate("EUR", 920_000L)
    }

    @Before
    fun open() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("rates", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, MoneyDatabase::class.java)
            .addCallback(MoneyDatabase.Seed)
            .allowMainThreadQueries()
            .build()
        rates = RatesStore(context)
        repo = MoneyRepository(db, rates)
    }

    @After
    fun close() {
        db.close()
    }

    /** Forces the state flow to emit once, which also refreshes the Accounts snapshot. */
    private fun state(): LedgerState = runBlocking { repo.state.first() }

    private fun addAccount(id: String, currency: String, opening: Long) = runBlocking {
        repo.upsertAccount(
            Account(id = id, name = id, kind = AccountKind.Bank, balanceMinor = 0, currency = currency),
            opening,
        )
        state()
    }

    // --- Balances are derived, never stored ----------------------------------------------------

    @Test
    fun `an account balance is its opening plus everything logged on it`() = runBlocking {
        addAccount("cash", "USD", 500_00L)
        repo.saveTransaction(
            merchant = "Shop", categoryId = "food", accountId = "cash",
            amountMinor = 20_00L, flow = Flow.Out,
        )
        repo.saveTransaction(
            merchant = "Work", categoryId = "income", accountId = "cash",
            amountMinor = 100_00L, flow = Flow.In,
        )
        assertEquals(580_00L, state().accounts.first { it.id == "cash" }.balanceMinor)
    }

    // --- Transfers -----------------------------------------------------------------------------

    @Test
    fun `a transfer writes a linked pair and moves the balance`() = runBlocking {
        addAccount("a", "USD", 500_00L)
        addAccount("b", "USD", 0L)

        assertTrue(repo.transfer("a", "b", 100_00L))

        val after = state()
        assertEquals(400_00L, after.accounts.first { it.id == "a" }.balanceMinor)
        assertEquals(100_00L, after.accounts.first { it.id == "b" }.balanceMinor)

        val moved = after.allTransactions.filter { it.flow == Flow.Transfer }
        assertEquals(2, moved.size)
    }

    @Test
    fun `a transfer across currencies credits the converted amount`() = runBlocking {
        publishRates()
        addAccount("usd", "USD", 500_00L)
        addAccount("eur", "EUR", 0L)

        assertTrue(repo.transfer("usd", "eur", 100_00L))

        val after = state()
        // A hundred dollars leaves, and ninety-two euros arrive. Before this, a hundred euros did.
        assertEquals(400_00L, after.accounts.first { it.id == "usd" }.balanceMinor)
        assertEquals(92_00L, after.accounts.first { it.id == "eur" }.balanceMinor)
    }

    @Test
    fun `a transfer with no rate is refused and writes nothing`() = runBlocking {
        publishRates()
        addAccount("usd", "USD", 500_00L)
        addAccount("krw", "KRW", 0L)

        assertFalse(repo.transfer("usd", "krw", 100_00L))

        val after = state()
        assertEquals(500_00L, after.accounts.first { it.id == "usd" }.balanceMinor)
        assertEquals(0L, after.accounts.first { it.id == "krw" }.balanceMinor)
        assertTrue(after.allTransactions.none { it.flow == Flow.Transfer })
    }

    @Test
    fun `deleting one side of a transfer deletes its twin`() = runBlocking {
        addAccount("a", "USD", 500_00L)
        addAccount("b", "USD", 0L)
        repo.transfer("a", "b", 100_00L)

        val oneSide = state().allTransactions.first { it.flow == Flow.Transfer }
        repo.deleteTransaction(oneSide.id)

        val after = state()
        assertTrue(after.allTransactions.none { it.flow == Flow.Transfer })
        assertEquals(500_00L, after.accounts.first { it.id == "a" }.balanceMinor)
        assertEquals(0L, after.accounts.first { it.id == "b" }.balanceMinor)
    }

    // --- Bills ---------------------------------------------------------------------------------

    @Test
    fun `paying a bill logs the money and moves the due date on`() = runBlocking {
        addAccount("cash", "USD", 500_00L)
        val due = today.minusDays(3)
        repo.upsertBill(Bill("b1", "Rent", 100_00L, due, Recurrence.Monthly, "cash"))

        repo.payBill("b1", on = today)

        val after = state()
        assertEquals(400_00L, after.accounts.first { it.id == "cash" }.balanceMinor)
        assertEquals(nextDue(due, Recurrence.Monthly, today), after.bills.first { it.id == "b1" }.due)
        assertTrue(after.allTransactions.any { it.merchant == "Rent" })
    }

    // --- Debts ---------------------------------------------------------------------------------

    @Test
    fun `a debt payment in another currency reduces the balance by the converted amount`() =
        runBlocking {
            publishRates()
            addAccount("usd", "USD", 500_00L)
            addAccount("eur", "EUR", 500_00L)
            // The debt is settled against the euro account, so it is owed in euros.
            repo.upsertDebt(Debt("d1", "Kredit", 500_00L, 0, 10_00L, "eur"))

            // A hundred dollars paid against a euro debt is ninety-two euros off it.
            assertTrue(repo.payDebt("d1", "usd", 100_00L))

            val after = state()
            assertEquals(408_00L, after.debts.first { it.id == "d1" }.balanceMinor)
            assertEquals(400_00L, after.accounts.first { it.id == "usd" }.balanceMinor)
        }

    @Test
    fun `a debt payment with no rate is refused and the balance stands`() = runBlocking {
        publishRates()
        addAccount("usd", "USD", 500_00L)
        addAccount("krw", "KRW", 0L)
        repo.upsertDebt(Debt("d1", "Loan", 500_00L, 0, 10_00L, "krw"))

        assertFalse(repo.payDebt("d1", "usd", 100_00L))

        val after = state()
        assertEquals(500_00L, after.debts.first { it.id == "d1" }.balanceMinor)
        assertEquals(500_00L, after.accounts.first { it.id == "usd" }.balanceMinor)
    }

    // --- Goals ---------------------------------------------------------------------------------

    @Test
    fun `a goal is credited in its own currency`() = runBlocking {
        publishRates()
        addAccount("usd", "USD", 500_00L)
        addAccount("eur", "EUR", 0L)
        repo.upsertGoal(Goal("g1", "Berlin", 1_000_00L, 0L, LocalDate.of(2027, 1, 1), "eur"))

        assertTrue(repo.contributeToGoal("g1", "usd", 100_00L))

        val after = state()
        // The goal holds euros, so a hundred dollars adds ninety-two to it, not a hundred.
        assertEquals(92_00L, after.goals.first { it.id == "g1" }.savedMinor)
        assertEquals(92_00L, after.accounts.first { it.id == "eur" }.balanceMinor)
        assertEquals(400_00L, after.accounts.first { it.id == "usd" }.balanceMinor)
    }

    @Test
    fun `a goal contribution with no rate is refused entirely`() = runBlocking {
        publishRates()
        addAccount("usd", "USD", 500_00L)
        addAccount("krw", "KRW", 0L)
        repo.upsertGoal(Goal("g1", "Seoul", 1_000_00L, 0L, LocalDate.of(2027, 1, 1), "krw"))

        assertFalse(repo.contributeToGoal("g1", "usd", 100_00L))

        val after = state()
        assertEquals(0L, after.goals.first { it.id == "g1" }.savedMinor)
        assertEquals(500_00L, after.accounts.first { it.id == "usd" }.balanceMinor)
    }

    // --- Splits --------------------------------------------------------------------------------

    @Test
    fun `a split is charged to its own categories not to the parent row`() = runBlocking {
        addAccount("cash", "USD", 500_00L)
        repo.setBudget("food", 200_00L)
        repo.setBudget("transport", 200_00L)
        repo.saveTransaction(
            merchant = "Trip", categoryId = "food", accountId = "cash",
            amountMinor = 100_00L, flow = Flow.Out,
            splits = listOf(Split("food", -60_00L), Split("transport", -40_00L)),
        )

        val budgets = state().budgets.associateBy { it.categoryId }
        assertEquals(60_00L, budgets["food"]?.spentMinor)
        assertEquals(40_00L, budgets["transport"]?.spentMinor)
    }

    @Test
    fun `a deleted transaction leaves no trace in the balance`() = runBlocking {
        addAccount("cash", "USD", 500_00L)
        val id = repo.saveTransaction(
            merchant = "Shop", categoryId = "food", accountId = "cash",
            amountMinor = 20_00L, flow = Flow.Out,
        )
        assertEquals(480_00L, state().accounts.first { it.id == "cash" }.balanceMinor)

        repo.deleteTransaction(id)
        assertEquals(500_00L, state().accounts.first { it.id == "cash" }.balanceMinor)
        assertNull(state().allTransactions.firstOrNull { it.id == id })
    }
}
