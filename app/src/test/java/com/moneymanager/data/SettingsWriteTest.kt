package com.moneymanager.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.moneymanager.data.db.MoneyDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek

/*
 * The write paths behind the Settings screen: templates, category edits, and erasing everything.
 *
 * These are the controls that did nothing until now, and two of them are destructive. What is
 * worth pinning is not that a row round-trips but that the destructive ones leave the app in a
 * state it can keep working from -- an erase that takes the categories with it would leave a
 * ledger with nowhere to file a transaction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class SettingsWriteTest {

    private lateinit var db: MoneyDatabase
    private lateinit var repo: MoneyRepository
    private lateinit var rates: RatesStore

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
        Periods.monthStartDay = 1
        Periods.weekStart = DayOfWeek.MONDAY
    }

    @After
    fun close() {
        Periods.monthStartDay = 1
        Periods.paydayDay = 1
        db.close()
    }

    private fun state(): LedgerState = runBlocking { repo.state.first() }

    // --- Templates -----------------------------------------------------------------------------

    @Test
    fun `a saved template comes back with its shape intact`() = runBlocking {
        repo.upsertTemplate(
            Template(
                id = "",
                name = "Rent",
                amountMinor = -1200_00L,
                merchant = "Landlord",
                categoryId = "home",
                accountId = "acc_cash",
            )
        )
        val saved = repo.templates.first().single()
        assertEquals("Rent", saved.name)
        assertEquals(-1200_00L, saved.amountMinor)
        assertEquals(Flow.Out, saved.flow)
        assertTrue(saved.id.isNotEmpty())
    }

    @Test
    fun `an amount of zero means ask me rather than log nothing`() = runBlocking {
        repo.upsertTemplate(
            Template(
                id = "", name = "Weekly shop", amountMinor = 0L, merchant = "Market",
                categoryId = "groceries", accountId = "acc_cash",
            )
        )
        assertEquals(0L, repo.templates.first().single().amountMinor)
    }

    @Test
    fun `deleting a template leaves the transactions it was used for alone`() = runBlocking {
        repo.upsertTemplate(
            Template(
                id = "t1", name = "Rent", amountMinor = -1200_00L, merchant = "Landlord",
                categoryId = "home", accountId = "acc_cash",
            )
        )
        repo.saveTransaction(
            merchant = "Landlord", categoryId = "home", accountId = "acc_cash",
            amountMinor = 1200_00L, flow = Flow.Out,
        )
        repo.deleteTemplate("t1")

        assertTrue(repo.templates.first().isEmpty())
        assertEquals(1, state().allTransactions.size)
    }

    // --- Categories ----------------------------------------------------------------------------

    @Test
    fun `renaming a category keeps the transactions filed under it`() = runBlocking {
        repo.saveTransaction(
            merchant = "Cafe", categoryId = "food", accountId = "acc_cash",
            amountMinor = 4_50L, flow = Flow.Out,
        )
        repo.upsertCategory(Category("food", "Eating out", "restaurant", null), sortOrder = 0)

        val after = state()
        assertEquals("Eating out", after.categories.first { it.id == "food" }.label)
        assertEquals("food", after.allTransactions.single().categoryId)
    }

    @Test
    fun `an archived category leaves the pickers but not the history`() = runBlocking {
        repo.saveTransaction(
            merchant = "Vet", categoryId = "pets", accountId = "acc_cash",
            amountMinor = 60_00L, flow = Flow.Out,
        )
        repo.archiveCategory("pets")

        val after = state()
        // Gone from the list the pickers render from...
        assertFalse(after.categories.any { it.id == "pets" })
        // ...but the transaction still names it, so the past is not relabelled.
        assertEquals("pets", after.allTransactions.single().categoryId)
    }

    // --- Erasing everything ----------------------------------------------------------------------

    @Test
    fun `erasing everything removes the ledger`() = runBlocking {
        repo.saveTransaction(
            merchant = "Shop", categoryId = "food", accountId = "acc_cash",
            amountMinor = 20_00L, flow = Flow.Out,
        )
        repo.upsertBill(
            Bill("b1", "Rent", 1200_00L, today, Recurrence.Monthly, "acc_cash")
        )
        assertEquals(1, state().allTransactions.size)

        repo.deleteEverything()

        val after = state()
        assertTrue(after.allTransactions.isEmpty())
        assertTrue(after.bills.isEmpty())
    }

    @Test
    fun `erasing everything leaves somewhere to log the next transaction`() = runBlocking {
        repo.saveTransaction(
            merchant = "Shop", categoryId = "food", accountId = "acc_cash",
            amountMinor = 20_00L, flow = Flow.Out,
        )
        repo.deleteEverything()

        // clearAllTables empties categories and accounts too, and the Room seed callback only
        // fires on create -- without the re-seed this app would come back unusable.
        val after = state()
        assertEquals(12, after.categories.size)
        assertTrue(after.accounts.any { it.id == "acc_cash" })

        // And it can actually be used again.
        repo.saveTransaction(
            merchant = "Shop", categoryId = "food", accountId = "acc_cash",
            amountMinor = 5_00L, flow = Flow.Out,
        )
        assertEquals(1, state().allTransactions.size)
    }

    // --- Payday, derived from the ledger ----------------------------------------------------------

    /**
     * A repository told the month opens on payday.
     *
     * The default settings flow re-asserts "the 1st" on every emission, which is right for the
     * app and wrong for this test: setting the snapshot by hand would be undone the moment the
     * state flow ran.
     */
    private fun paydayRepo() = MoneyRepository(
        db, rates, kotlinx.coroutines.flow.flowOf(PeriodSettings(Periods.PAYDAY, DayOfWeek.MONDAY)),
    )

    @Test
    fun `payday is the median day income actually arrived`() = runBlocking {
        val payday = paydayRepo()
        listOf(1L, 2L, 3L).forEach { monthsAgo ->
            repo.saveTransaction(
                merchant = "Work", categoryId = "income", accountId = "acc_cash",
                amountMinor = 3000_00L, flow = Flow.In,
                date = today.minusMonths(monthsAgo).withDayOfMonth(25),
            )
        }
        payday.state.first()
        assertEquals(25, Periods.startDay)
    }

    @Test
    fun `too little income to tell falls back to the first rather than guessing`() = runBlocking {
        val payday = paydayRepo()
        repo.saveTransaction(
            merchant = "Refund", categoryId = "income", accountId = "acc_cash",
            amountMinor = 12_00L, flow = Flow.In, date = today.withDayOfMonth(17),
        )
        payday.state.first()
        // One refund is not a pattern. Opening the cycle on the 17th off a single row would
        // move every figure on Home on the strength of one coincidence.
        assertEquals(1, Periods.startDay)
    }
}
