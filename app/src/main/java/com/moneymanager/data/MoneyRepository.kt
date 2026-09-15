package com.moneymanager.data

import com.moneymanager.data.db.AccountEntity
import com.moneymanager.data.db.BillEntity
import com.moneymanager.data.db.BudgetEntity
import com.moneymanager.data.db.CategoryEntity
import com.moneymanager.data.db.DebtEntity
import com.moneymanager.data.db.GoalEntity
import com.moneymanager.data.db.MoneyDatabase
import com.moneymanager.data.db.SplitEntity
import com.moneymanager.data.db.TagEntity
import com.moneymanager.data.db.TxnEntity
import com.moneymanager.data.db.TxnWithDetails
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.absoluteValue

/**
 * Everything every screen reads, in one snapshot.
 *
 * Shaped to match what the screens already consumed while this was a scaffold, so wiring the real
 * ledger underneath them did not turn into rewriting them. Every total here is derived at read
 * time from the transactions; none of it is stored, so a summary can never disagree with the
 * ledger it summarises.
 */
data class LedgerState(
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    /** This month, newest first. */
    val transactions: List<Txn> = emptyList(),
    val allTransactions: List<Txn> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val loading: Boolean = true,
) {
    // --- The month -------------------------------------------------------------------------------

    val budgetedMinor = budgets.sumOf { it.limitMinor }
    val spentMinor = budgets.sumOf { it.spentMinor }

    /** Bills still to be paid this month. Committed money is not safe to spend. */
    val committedMinor = bills
        .filter { it.due >= today && YearMonth.from(it.due) == thisMonth }
        .sumOf { it.amountMinor }

    val safeToSpendMinor = (budgetedMinor - spentMinor - committedMinor).coerceAtLeast(0)

    /** Water level: what is left, against what the month started with. */
    val safeFraction: Float =
        if (budgetedMinor == 0L) 0f else safeToSpendMinor.toFloat() / budgetedMinor

    val perDayMinor = if (daysLeft > 0) safeToSpendMinor / daysLeft else safeToSpendMinor

    val liquidMinor = accounts
        .filter { it.kind != AccountKind.Savings && it.balanceMinor > 0 }
        .sumOf { it.balanceMinor }

    val todaysTransactions get() = transactions.filter { it.date == today }
    val loggedToday get() = todaysTransactions.isNotEmpty()

    val hasAnything get() = allTransactions.isNotEmpty()

    // --- Net worth -------------------------------------------------------------------------------

    private val assetsNow = accounts.filter { it.balanceMinor >= 0 }.sumOf { it.balanceMinor }
    private val owedNow = accounts.filter { it.balanceMinor < 0 }.sumOf { -it.balanceMinor } +
        debts.sumOf { it.balanceMinor }

    val netWorthMinor = assetsNow - owedNow

    /**
     * Twelve month-ends of net worth, walked backwards out of the ledger. Each step removes that
     * month's transactions from the running total, which is what makes the line the ledger's own
     * history instead of a decorative curve.
     */
    private val netWorthSeries: Pair<List<Long>, List<Long>> = run {
        val assets = ArrayDeque<Long>()
        val owed = ArrayDeque<Long>()
        var a = assetsNow
        var o = owedNow
        var month = thisMonth
        repeat(12) {
            assets.addFirst(a)
            owed.addFirst(o)
            val inMonth = allTransactions.filter { YearMonth.from(it.date) == month }
            val delta = inMonth.sumOf { it.amountMinor }
            a -= delta
            month = month.minusMonths(1)
        }
        assets.toList() to owed.toList()
    }

    val netWorthAssets: List<Long> get() = netWorthSeries.first
    val netWorthDebts: List<Long> get() = netWorthSeries.second

    // --- Six-month series ------------------------------------------------------------------------

    private val recentMonths: List<YearMonth> = (5 downTo 0).map { thisMonth.minusMonths(it.toLong()) }

    val monthLabels: List<String> get() = recentMonths.map { it.month.short() }

    val monthlyIn: List<Long> get() = recentMonths.map { m ->
        allTransactions.filter { YearMonth.from(it.date) == m && it.flow == Flow.In }
            .sumOf { it.amountMinor }
    }

    val monthlyOut: List<Long> get() = recentMonths.map { m ->
        allTransactions.filter { YearMonth.from(it.date) == m && it.flow == Flow.Out }
            .sumOf { it.amountMinor.absoluteValue }
    }

    /** Spend per day of the current month so far, for the cash-flow heatmap. */
    val dailySpend: List<Long> get() = (1..daysInMonth).map { day ->
        transactions
            .filter { it.date.dayOfMonth == day && it.flow == Flow.Out }
            .sumOf { it.amountMinor.absoluteValue }
    }

    val topMerchants: List<MerchantTotal> get() = transactions
        .filter { it.flow == Flow.Out }
        .groupBy { it.merchant }
        .map { (name, rows) -> MerchantTotal(name, rows.sumOf { it.amountMinor.absoluteValue }, rows.size) }
        .sortedByDescending { it.totalMinor }
        .take(5)

    fun byDay(): List<Pair<LocalDate, List<Txn>>> =
        transactions.groupBy { it.date }.toList().sortedByDescending { it.first }

    fun spendByCategory(): List<Pair<Category, Long>> = transactions
        .filter { it.flow == Flow.Out }
        .flatMap { t ->
            if (t.splits.isEmpty()) listOf(t.categoryId to t.amountMinor)
            else t.splits.map { it.categoryId to it.amountMinor }
        }
        .groupBy({ it.first }, { it.second })
        .map { (id, amounts) -> Categories[id] to amounts.sumOf { it.absoluteValue } }
        .sortedByDescending { it.second }

    // --- Gamification, computed from the ledger and nowhere else ---------------------------------

    /** Consecutive days back from today with at least one transaction logged. */
    val loggingStreakDays: Int get() {
        val days = allTransactions.map { it.date }.toHashSet()
        var streak = 0
        var day = today
        if (day !in days) day = day.minusDays(1) // today is not over yet; it cannot break a streak
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    val badges: List<Badge> get() {
        val n = allTransactions.size
        val goalHit = goals.any { it.savedMinor >= it.targetMinor && it.targetMinor > 0 }
        val debtFree = debts.isEmpty() && accounts.none { it.balanceMinor < 0 }
        return listOf(
            Badge("ba1", "First fifty", "Fifty transactions logged.",
                if (n >= 50) today else null, (n / 50f).coerceAtMost(1f)),
            Badge("ba2", "Two weeks running", "A fourteen-day logging streak.",
                if (loggingStreakDays >= 14) today else null,
                (loggingStreakDays / 14f).coerceAtMost(1f)),
            Badge("ba3", "Under budget", "A whole month inside every envelope.",
                null, if (budgets.isEmpty()) 0f else budgets.count { !it.over } / budgets.size.toFloat()),
            Badge("ba4", "Debt-free", "Every tracked debt cleared.",
                if (debtFree && n > 0) today else null, if (debtFree) 1f else 0f),
            Badge("ba5", "Goal hit", "A savings goal reached in full.",
                if (goalHit) today else null,
                goals.maxOfOrNull { it.fraction.coerceAtMost(1f) } ?: 0f),
        )
    }

    val challenges: List<Challenge> get() = emptyList()
}

class MoneyRepository(private val db: MoneyDatabase) {

    private val txnDao = db.txns()

    private val reference = combine(
        db.categories().observeAll(),
        db.accounts().observeAll(),
        txnDao.observeAccountSums(),
    ) { categories, accounts, sums ->
        val byAccount = sums.associate { it.accountId to it.totalMinor }
        val domainCategories = categories.map {
            Category(it.id, it.label, it.iconKey, it.parentId)
        }
        val domainAccounts = accounts.map {
            Account(
                id = it.id,
                name = it.name,
                kind = AccountKind.of(it.kind),
                balanceMinor = it.openingBalanceMinor + (byAccount[it.id] ?: 0L),
                currency = it.currency,
                limitMinor = it.limitMinor,
            )
        }
        // Refresh the lookup snapshot before anything downstream renders against it.
        Categories.snapshot = domainCategories
        Accounts.snapshot = domainAccounts
        domainCategories to domainAccounts
    }

    private val plans = combine(
        db.budgets().observeAll(),
        db.bills().observeAll(),
        db.goals().observeAll(),
        db.debts().observeAll(),
    ) { budgets, bills, goals, debts -> Plans(budgets, bills, goals, debts) }

    private data class Plans(
        val budgets: List<BudgetEntity>,
        val bills: List<BillEntity>,
        val goals: List<GoalEntity>,
        val debts: List<DebtEntity>,
    )

    val state: kotlinx.coroutines.flow.Flow<LedgerState> = combine(
        reference,
        txnDao.observeAll(),
        plans,
    ) { (categories, accounts), rows, p ->
        val all = rows.map { it.toDomain() }
        val month = thisMonth
        val inMonth = all.filter { YearMonth.from(it.date) == month }
        val period = month.periodKey()

        // What has actually been spent against each envelope, splits attributed to their own
        // categories rather than to the parent transaction's.
        val spend = inMonth
            .filter { it.flow == Flow.Out }
            .flatMap { t ->
                if (t.splits.isEmpty()) listOf(t.categoryId to t.amountMinor.absoluteValue)
                else t.splits.map { it.categoryId to it.amountMinor.absoluteValue }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, v) -> v.sum() }

        LedgerState(
            categories = categories,
            accounts = accounts,
            transactions = inMonth,
            allTransactions = all,
            budgets = p.budgets
                .filter { it.periodMonth == period }
                .map {
                    Budget(
                        categoryId = it.categoryId,
                        limitMinor = it.limitMinor + it.rolloverMinor,
                        spentMinor = spend[it.categoryId] ?: 0L,
                        rollsOver = it.rollsOver,
                    )
                },
            bills = p.bills.map { it.toDomain() },
            goals = p.goals.map {
                Goal(it.id, it.name, it.targetMinor, it.savedMinor,
                    LocalDate.ofEpochDay(it.byEpochDay), it.accountId, it.imagePath)
            },
            debts = p.debts.map { Debt(it.id, it.name, it.balanceMinor, it.aprBasisPoints, it.minimumMinor) },
            loading = false,
        )
    }

    fun observeTransaction(id: String): kotlinx.coroutines.flow.Flow<Txn?> =
        txnDao.observeById(id).map { it?.toDomain() }

    fun observeAccountTransactions(accountId: String): kotlinx.coroutines.flow.Flow<List<Txn>> =
        txnDao.observeByAccount(accountId).map { rows -> rows.map { it.toDomain() } }

    val tags: kotlinx.coroutines.flow.Flow<List<String>> = txnDao.observeTags()

    // --- Writes ------------------------------------------------------------------------------------

    suspend fun saveTransaction(
        id: String? = null,
        merchant: String,
        categoryId: String,
        accountId: String,
        amountMinor: Long,
        flow: Flow,
        date: LocalDate = today,
        time: LocalTime = LocalTime.now(),
        note: String? = null,
        tags: List<String> = emptyList(),
        splits: List<Split> = emptyList(),
    ): String {
        val txnId = id ?: UUID.randomUUID().toString()
        val signed = when (flow) {
            Flow.In -> amountMinor.absoluteValue
            else -> -amountMinor.absoluteValue
        }
        txnDao.save(
            txn = TxnEntity(
                id = txnId,
                merchant = merchant.trim().ifEmpty { Categories[categoryId].label },
                categoryId = categoryId,
                accountId = accountId,
                amountMinor = signed,
                epochDay = date.toEpochDay(),
                secondOfDay = time.toSecondOfDay(),
                flow = flow.name,
                note = note?.trim()?.ifEmpty { null },
                currency = Accounts[accountId].currency,
                createdAtEpochSecond = System.currentTimeMillis() / 1000,
            ),
            splits = splits.map { SplitEntity(txnId = txnId, categoryId = it.categoryId, amountMinor = it.amountMinor) },
            tags = tags.map { TagEntity(txnId, it) },
        )
        if (merchant.isNotBlank()) db.merchantMemory().remember(merchant.trim(), categoryId)
        return txnId
    }

    suspend fun deleteTransaction(id: String) = txnDao.deleteById(id)

    suspend fun suggestCategory(merchant: String): String? =
        if (merchant.isBlank()) null else db.merchantMemory().categoryFor(merchant.trim())

    suspend fun upsertAccount(account: Account, openingBalanceMinor: Long) =
        db.accounts().upsert(
            AccountEntity(
                id = account.id.ifEmpty { UUID.randomUUID().toString() },
                name = account.name,
                kind = account.kind.name,
                openingBalanceMinor = openingBalanceMinor,
                currency = account.currency,
                limitMinor = account.limitMinor,
            )
        )

    suspend fun setBudget(categoryId: String, limitMinor: Long, rollsOver: Boolean = false) =
        db.budgets().upsert(
            BudgetEntity(
                categoryId = categoryId,
                periodMonth = thisMonth.periodKey(),
                limitMinor = limitMinor,
                rollsOver = rollsOver,
            )
        )

    suspend fun clearBudget(categoryId: String) =
        db.budgets().delete(categoryId, thisMonth.periodKey())

    suspend fun upsertBill(bill: Bill) = db.bills().upsert(
        BillEntity(
            id = bill.id.ifEmpty { UUID.randomUUID().toString() },
            name = bill.name,
            amountMinor = bill.amountMinor,
            dueEpochDay = bill.due.toEpochDay(),
            every = bill.every.name,
            accountId = bill.accountId,
            subscription = bill.subscription,
        )
    )

    suspend fun upsertGoal(goal: Goal) = db.goals().upsert(
        GoalEntity(
            id = goal.id.ifEmpty { UUID.randomUUID().toString() },
            name = goal.name,
            targetMinor = goal.targetMinor,
            savedMinor = goal.savedMinor,
            byEpochDay = goal.by.toEpochDay(),
            accountId = goal.accountId,
            imagePath = goal.imagePath,
        )
    )

    suspend fun upsertCategory(category: Category) = db.categories().upsert(
        CategoryEntity(
            id = category.id.ifEmpty { UUID.randomUUID().toString() },
            label = category.label,
            iconKey = category.iconKey,
            parentId = category.parent,
        )
    )

    suspend fun isEmpty(): Boolean = txnDao.count() == 0
}

// --- Mapping -------------------------------------------------------------------------------------

private fun TxnWithDetails.toDomain() = Txn(
    id = txn.id,
    merchant = txn.merchant,
    categoryId = txn.categoryId,
    accountId = txn.accountId,
    amountMinor = txn.amountMinor,
    date = LocalDate.ofEpochDay(txn.epochDay),
    time = LocalTime.ofSecondOfDay(txn.secondOfDay.toLong()),
    flow = Flow.valueOf(txn.flow),
    note = txn.note,
    tags = tags.map { it.tag },
    splits = splits.map { Split(it.categoryId, it.amountMinor) },
    currency = txn.currency,
    originalMinor = txn.originalMinor,
    originalCurrency = txn.originalCurrency,
    hasReceipt = txn.receiptPath != null,
)

private fun BillEntity.toDomain(): Bill {
    val idle = lastPaidEpochDay?.let {
        ChronoUnit.MONTHS.between(LocalDate.ofEpochDay(it), today).toInt().takeIf { m -> m > 0 }
    }
    return Bill(
        id = id,
        name = name,
        amountMinor = amountMinor,
        due = LocalDate.ofEpochDay(dueEpochDay),
        every = Recurrence.of(every),
        accountId = accountId,
        subscription = subscription,
        idleMonths = idle,
    )
}
