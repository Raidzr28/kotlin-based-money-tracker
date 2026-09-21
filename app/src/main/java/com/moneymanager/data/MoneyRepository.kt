package com.moneymanager.data

import com.moneymanager.data.db.AccountEntity
import com.moneymanager.data.db.AssetEntity
import com.moneymanager.data.db.BillEntity
import com.moneymanager.data.db.BudgetEntity
import com.moneymanager.data.db.CategoryEntity
import com.moneymanager.data.db.DebtEntity
import com.moneymanager.data.db.GoalEntity
import com.moneymanager.data.db.MoneyDatabase
import com.moneymanager.data.db.SplitEntity
import com.moneymanager.data.db.TagEntity
import com.moneymanager.data.db.TemplateEntity
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
    val assets: List<Asset> = emptyList(),
    val conversion: Conversion = Conversion(),
    val loading: Boolean = true,
) {

    // --- One currency out ------------------------------------------------------------------------

    /*
     * Accounts and the rows on them are kept in their own currencies; every total below is in the
     * base. Anything with no rate to the base is left out of the sum rather than added in raw,
     * and named in [unconvertible] so a screen can say the total is short rather than quietly
     * presenting a wrong one.
     */

    private fun Txn.baseMinor(): Long? = conversion.toBase(amountMinor, currency)
    private fun Account.baseMinor(): Long? = conversion.toBase(balanceMinor, currency)

    val baseCurrency: String get() = conversion.base

    /** Currencies on the books that no rate reaches. Empty in the ordinary single-currency case. */
    val unconvertible: List<String> =
        (accounts.map { it.currency } + allTransactions.map { it.currency } +
            bills.map { it.currency } + debts.map { it.currency } + assets.map { it.currency })
            .distinct()
            .filterNot { conversion.canConvert(it) }
            .sorted()

    val totalsAreComplete: Boolean get() = unconvertible.isEmpty()
    // --- The month -------------------------------------------------------------------------------

    val budgetedMinor = budgets.sumOf { it.limitMinor }
    val spentMinor = budgets.sumOf { it.spentMinor }

    /** Bills still to be paid this month. Committed money is not safe to spend. */
    val committedMinor = bills
        .filter { it.due >= today && it.due <= cycleEnd }
        .sumOf { conversion.toBase(it.amountMinor, it.currency) ?: 0L }

    val safeToSpendMinor = (budgetedMinor - spentMinor - committedMinor).coerceAtLeast(0)

    /** Water level: what is left, against what the month started with. */
    val safeFraction: Float =
        if (budgetedMinor == 0L) 0f else safeToSpendMinor.toFloat() / budgetedMinor

    val perDayMinor = if (daysLeft > 0) safeToSpendMinor / daysLeft else safeToSpendMinor

    val liquidMinor = accounts
        .filter { it.kind != AccountKind.Savings && it.balanceMinor > 0 }
        .sumOf { it.baseMinor() ?: 0L }

    val todaysTransactions get() = transactions.filter { it.date == today }
    val loggedToday get() = todaysTransactions.isNotEmpty()

    val hasAnything get() = allTransactions.isNotEmpty()

    // --- Net worth -------------------------------------------------------------------------------

    private val inAccountsNow = accounts.filter { it.balanceMinor >= 0 }.sumOf { it.baseMinor() ?: 0L }
    private val owedNow = accounts.filter { it.balanceMinor < 0 }.sumOf { -(it.baseMinor() ?: 0L) } +
        debts.sumOf { conversion.toBase(it.balanceMinor, it.currency) ?: 0L }

    /** What the tracked possessions are worth on [date], in the base currency. */
    private fun ownedOn(date: LocalDate): Long =
        assets.sumOf { conversion.toBase(it.valueOn(date), it.currency) ?: 0L }

    /** What every tracked possession is worth today. Shown beside the accounts. */
    val assetsMinor: Long get() = ownedOn(today)

    private val assetsNow = inAccountsNow + assetsMinor

    val netWorthMinor = assetsNow - owedNow

    /**
     * Twelve month-ends of net worth, walked backwards out of the ledger. Each step removes that
     * month's transactions from the running total, which is what makes the line the ledger's own
     * history instead of a decorative curve.
     */
    private val netWorthSeries: Pair<List<Long>, List<Long>> = run {
        val owned = ArrayDeque<Long>()
        val owed = ArrayDeque<Long>()
        var inAccounts = inAccountsNow
        val o = owedNow
        var month = thisMonth
        repeat(12) {
            // Possessions are valued at that month's end rather than carried back at today's
            // figure. A laptop bought in March was not on the books in January, and in April it
            // was worth more than it is now -- carrying one number backwards would credit the
            // user with both.
            owned.addFirst(inAccounts + ownedOn(month.atEndOfMonth()))
            owed.addFirst(o)
            val inMonth = allTransactions.filter { YearMonth.from(it.date) == month }
            inAccounts -= inMonth.sumOf { it.baseMinor() ?: 0L }
            month = month.minusMonths(1)
        }
        owned.toList() to owed.toList()
    }

    val netWorthAssets: List<Long> get() = netWorthSeries.first
    val netWorthDebts: List<Long> get() = netWorthSeries.second

    // --- Six-month series ------------------------------------------------------------------------

    private val recentMonths: List<YearMonth> = (5 downTo 0).map { thisMonth.minusMonths(it.toLong()) }

    val monthLabels: List<String> get() = recentMonths.map { it.month.short() }

    val monthlyIn: List<Long> get() = recentMonths.map { m ->
        allTransactions.filter { YearMonth.from(it.date) == m && it.flow == Flow.In }
            .sumOf { it.baseMinor() ?: 0L }
    }

    val monthlyOut: List<Long> get() = recentMonths.map { m ->
        allTransactions.filter { YearMonth.from(it.date) == m && it.flow == Flow.Out }
            .sumOf { it.baseMinor()?.absoluteValue ?: 0L }
    }

    /**
     * Spend per day of the current cycle so far, for the cash-flow heatmap.
     *
     * Indexed from the cycle's opening day rather than the calendar's, so a month that starts on
     * the 25th fills its grid from the 25th instead of leaving three weeks blank.
     */
    val dailySpend: List<Long> get() {
        val start = cycleStart
        return (0 until daysInMonth).map { offset ->
            val day = start.plusDays(offset.toLong())
            transactions
                .filter { it.date == day && it.flow == Flow.Out }
                .sumOf { it.baseMinor()?.absoluteValue ?: 0L }
        }
    }

    val topMerchants: List<MerchantTotal> get() = transactions
        .filter { it.flow == Flow.Out }
        .groupBy { it.merchant }
        .map { (name, rows) ->
            MerchantTotal(name, rows.sumOf { it.baseMinor()?.absoluteValue ?: 0L }, rows.size)
        }
        .sortedByDescending { it.totalMinor }
        .take(5)

    fun byDay(): List<Pair<LocalDate, List<Txn>>> =
        transactions.groupBy { it.date }.toList().sortedByDescending { it.first }

    fun spendByCategory(): List<Pair<Category, Long>> = transactions
        .filter { it.flow == Flow.Out }
        .flatMap { t ->
            val parts = if (t.splits.isEmpty()) listOf(t.categoryId to t.amountMinor)
            else t.splits.map { it.categoryId to it.amountMinor }
            parts.mapNotNull { (id, minor) ->
                conversion.toBase(minor, t.currency)?.let { id to it }
            }
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

    /**
     * The pace a challenge is scored against: the month's budget spread evenly.
     *
     * Not [perDayMinor], which is what is left divided by the days remaining. That figure climbs
     * every time you underspend, so yesterday would be judged against a bar that did not exist
     * yesterday. An even pace is the same bar all month.
     */
    val evenPaceMinor = if (daysInMonth == 0) 0L else budgetedMinor / daysInMonth

    val challenges: List<Challenge> get() =
        challengesFor(allTransactions, today, evenPaceMinor, conversion)
}

class MoneyRepository(
    private val db: MoneyDatabase,
    private val rates: RatesStore,
    /**
     * Where the user's month begins. Defaults to the calendar, which is what a test that does
     * not care about cycles wants, and what an install that has never opened Settings gets.
     */
    private val periodSettings: kotlinx.coroutines.flow.Flow<PeriodSettings> =
        kotlinx.coroutines.flow.flowOf(PeriodSettings()),
) {

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
        db.assets().observeAll(),
    ) { budgets, bills, goals, debts, assets -> Plans(budgets, bills, goals, debts, assets) }

    private data class Plans(
        val budgets: List<BudgetEntity>,
        val bills: List<BillEntity>,
        val goals: List<GoalEntity>,
        val debts: List<DebtEntity>,
        val assets: List<AssetEntity>,
    )

    val state: kotlinx.coroutines.flow.Flow<LedgerState> = combine(
        reference,
        txnDao.observeAll(),
        plans,
        // A fourth source, and the reason the base currency is no longer a setting that changes
        // nothing: picking one re-emits the whole ledger converted into it.
        rates.conversion,
        // A fifth, for the same reason: moving the start of the month has to move every figure
        // derived from it, not just the one stored preference.
        periodSettings,
    ) { (categories, accounts), rows, p, conversion, periods ->
        Periods.monthStartDay = periods.monthStartDay
        Periods.weekStart = periods.weekStart
        val all = rows.map { it.toDomain() }
        // A payday cycle has to know payday before the cycle bounds mean anything, and payday is
        // read off the ledger -- so it is resolved here, on the same emission, before any of the
        // derived figures below are computed.
        Periods.paydayDay = paydayFrom(all)
        val start = cycleStart
        val end = cycleEnd
        val inMonth = all.filter { it.date >= start && it.date <= end }
        val period = thisMonth.periodKey()

        // What has actually been spent against each envelope, splits attributed to their own
        // categories rather than to the parent transaction's.
        // Budgets are set in the base currency, so spend against them has to arrive in it.
        val spend = inMonth
            .filter { it.flow == Flow.Out }
            .flatMap { t ->
                val parts = if (t.splits.isEmpty()) listOf(t.categoryId to t.amountMinor)
                else t.splits.map { it.categoryId to it.amountMinor }
                parts.mapNotNull { (id, minor) ->
                    conversion.toBase(minor, t.currency)?.let { id to it.absoluteValue }
                }
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
            debts = p.debts.map {
                Debt(it.id, it.name, it.balanceMinor, it.aprBasisPoints, it.minimumMinor, it.accountId)
            },
            assets = p.assets.map {
                Asset(
                    id = it.id,
                    name = it.name,
                    costMinor = it.costMinor,
                    boughtOn = LocalDate.ofEpochDay(it.boughtEpochDay),
                    usefulLifeMonths = it.usefulLifeMonths,
                    warrantyUntil = it.warrantyUntilEpochDay?.let(LocalDate::ofEpochDay),
                    accountId = it.accountId,
                )
            },
            conversion = conversion,
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
        learn: Boolean = true,
        /** What the user actually paid, when that was in another currency. */
        originalMinor: Long? = null,
        originalCurrency: String? = null,
        rateMicros: Long? = null,
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
                originalMinor = originalMinor,
                originalCurrency = originalCurrency,
                // Stored with the row, not looked up later: a transaction is a record of what
                // happened at a rate that applied then, and tomorrow's rate would rewrite history.
                rateBasisPoints = rateMicros?.let { (it / 100).toInt() },
                createdAtEpochSecond = System.currentTimeMillis() / 1000,
            ),
            splits = splits.map { SplitEntity(txnId = txnId, categoryId = it.categoryId, amountMinor = it.amountMinor) },
            tags = tags.map { TagEntity(txnId, it) },
        )
        if (learn && merchant.isNotBlank()) db.merchantMemory().remember(merchant.trim(), categoryId)
        return txnId
    }

    /**
     * Money between two of your own accounts.
     *
     * Written as two linked rows rather than one, because a single row cannot be true of both
     * sides: the money genuinely left one account and genuinely arrived in another, and every
     * balance in the app is the sum of that account's rows. Both are flagged Transfer, which is
     * what keeps them out of income and expense totals while still moving the balances.
     */
    /**
     * The same amount of money, said in another account's currency. Null when no rate says it.
     *
     * Every write below that moves money out of one currency context and into another goes
     * through this. Before it existed, a transfer wrote the same number on both sides and tagged
     * each with its own account's currency, which turned a hundred dollars into a hundred euros
     * on the way across.
     */
    private fun crossCurrency(amountMinor: Long, from: String, to: String): Long? {
        if (from == to) return amountMinor
        val table = rates.conversion.value.rates ?: return null
        return convertMinor(amountMinor, from, to, table)
    }

    /**
     * Moves money between two accounts as a linked pair of rows.
     *
     * Returns false without writing anything when the two accounts are in different currencies
     * and no rate connects them. Refusing is the only honest option: a half-written transfer, or
     * one written at a guessed rate, is worse than one the user is told did not happen.
     */
    suspend fun transfer(
        fromAccountId: String,
        toAccountId: String,
        amountMinor: Long,
        date: LocalDate = today,
        time: LocalTime = LocalTime.now(),
        note: String? = null,
    ): Boolean {
        if (fromAccountId == toAccountId || amountMinor <= 0L) return false
        val amount = amountMinor.absoluteValue
        val from = Accounts[fromAccountId]
        val to = Accounts[toAccountId]
        val credited = crossCurrency(amount, from.currency, to.currency) ?: return false
        val outId = UUID.randomUUID().toString()
        val inId = UUID.randomUUID().toString()
        val stamp = System.currentTimeMillis() / 1000

        fun row(id: String, pair: String, accountId: String, signed: Long, label: String) = TxnEntity(
            id = id,
            merchant = label,
            categoryId = "savings",
            accountId = accountId,
            amountMinor = signed,
            epochDay = date.toEpochDay(),
            secondOfDay = time.toSecondOfDay(),
            flow = Flow.Transfer.name,
            note = note?.trim()?.ifEmpty { null },
            currency = Accounts[accountId].currency,
            transferPairId = pair,
            createdAtEpochSecond = stamp,
        )

        txnDao.save(row(outId, inId, fromAccountId, -amount, "To ${to.name}"), emptyList(), emptyList())
        txnDao.save(row(inId, outId, toAccountId, credited, "From ${from.name}"), emptyList(), emptyList())
        return true
    }

    /**
     * Deleting one half of a transfer deletes the other. Leaving the twin behind would invent
     * money in one account and destroy it in another.
     */
    /**
     * Writes the rows the user ticked on the import screen.
     *
     * A merchant the user has categorised before keeps that category; everything else takes the
     * fallback they chose. Importing deliberately does not teach the merchant memory, because the
     * fallback is a bulk guess rather than a decision about this merchant -- letting it write back
     * would turn one careless import into a wrong default forever.
     */
    suspend fun importStatement(
        rows: List<StatementRow>,
        accountId: String,
        fallbackCategoryId: String,
    ): Int {
        rows.forEach { row ->
            val learned = suggestCategory(row.description)
            saveTransaction(
                merchant = row.description,
                categoryId = learned ?: fallbackCategoryId,
                accountId = accountId,
                amountMinor = row.amountMinor,
                flow = if (row.amountMinor >= 0) Flow.In else Flow.Out,
                date = row.date,
                // A statement says which day, never which minute. Noon is a neutral stand-in
                // rather than a pretence that the time is known.
                time = LocalTime.NOON,
                learn = false,
            )
        }
        return rows.size
    }

    suspend fun deleteTransaction(id: String) {
        val pair = txnDao.pairIdOf(id)
        txnDao.deleteById(id)
        if (pair != null) txnDao.deleteById(pair)
    }

    /**
     * The day of the month money tends to arrive, for a cycle pinned to payday.
     *
     * The median day-of-month of income logged in the last six months. A median rather than the
     * latest, because a salary that lands on the Friday before a weekend moves by a day or two
     * and the cycle should not follow it; and rather than the mean, because one refund on the
     * 2nd should not drag a 25th payday down to the 14th.
     *
     * ponytail: one payday a month. Someone paid fortnightly gets whichever half the median
     * lands in; a real biweekly cycle would need its own recurrence rather than a day number.
     */
    private fun paydayFrom(all: List<Txn>): Int {
        if (Periods.monthStartDay != Periods.PAYDAY) return Periods.paydayDay
        val since = today.minusMonths(6)
        val days = all
            .filter { it.flow == Flow.In && it.date >= since }
            .map { it.date.dayOfMonth }
            .sorted()
        // Two rows is the least that can show a pattern rather than a one-off.
        return if (days.size < 2) 1 else days[days.size / 2]
    }

    // --- Assets ------------------------------------------------------------------------------

    suspend fun upsertAsset(asset: Asset) = db.assets().upsert(
        AssetEntity(
            id = asset.id.ifEmpty { UUID.randomUUID().toString() },
            name = asset.name,
            costMinor = asset.costMinor,
            boughtEpochDay = asset.boughtOn.toEpochDay(),
            usefulLifeMonths = asset.usefulLifeMonths,
            warrantyUntilEpochDay = asset.warrantyUntil?.toEpochDay(),
            accountId = asset.accountId,
        )
    )

    /**
     * Stops tracking a possession.
     *
     * Only the record goes. Whatever transaction paid for it stays exactly where it was: the
     * money did leave the account, and deleting the note that it became a thing must not rewrite
     * that. Net worth drops by what it was worth, which is what selling or losing it means.
     */
    suspend fun deleteAsset(id: String) = db.assets().deleteById(id)

    // --- Templates ---------------------------------------------------------------------------

    val templates: kotlinx.coroutines.flow.Flow<List<Template>> =
        db.templates().observeAll().map { rows ->
            rows.map {
                Template(
                    id = it.id,
                    name = it.name,
                    amountMinor = it.amountMinor,
                    merchant = it.merchant,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    note = it.note,
                    sortOrder = it.sortOrder,
                )
            }
        }

    suspend fun upsertTemplate(template: Template) = db.templates().upsert(
        TemplateEntity(
            id = template.id.ifEmpty { UUID.randomUUID().toString() },
            name = template.name,
            amountMinor = template.amountMinor,
            merchant = template.merchant,
            categoryId = template.categoryId,
            accountId = template.accountId,
            note = template.note,
            sortOrder = template.sortOrder,
        )
    )

    suspend fun deleteTemplate(id: String) = db.templates().deleteById(id)

    // --- Categories --------------------------------------------------------------------------

    suspend fun upsertCategory(category: Category, sortOrder: Int) = db.categories().upsert(
        CategoryEntity(
            id = category.id.ifEmpty { UUID.randomUUID().toString() },
            label = category.label,
            iconKey = category.iconKey,
            parentId = category.parent,
            sortOrder = sortOrder,
            archived = false,
        )
    )

    /**
     * Hides a category without deleting it.
     *
     * Every transaction ever filed under it still names it, so removing the row would leave
     * those rows pointing at nothing and silently relabel history as "Uncategorised". Archiving
     * takes it out of every picker and leaves the past intact.
     */
    suspend fun archiveCategory(id: String) {
        val existing = db.categories().byId(id) ?: return
        db.categories().upsert(existing.copy(archived = true))
    }

    // --- Everything --------------------------------------------------------------------------

    /**
     * Erases the user's whole ledger, then puts back the day-one categories and cash account.
     *
     * `clearAllTables` empties every table including `categories` and `accounts`, and the Room
     * seed callback only fires on create -- so without re-seeding, the app would come back with
     * no categories to file anything under and nowhere to put it.
     */
    suspend fun deleteEverything() {
        db.clearAllTables()
        MoneyDatabase.Seed.reseed(db)
    }

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

    /**
     * Records a bill as paid: logs the money going out, then moves the bill to its next date.
     *
     * The next date is advanced repeatedly rather than once, because a bill three months overdue
     * would otherwise land on a date that is still in the past and reappear as overdue the moment
     * the user finished paying it.
     */
    suspend fun payBill(billId: String, on: LocalDate = today) {
        val bill = db.bills().byId(billId) ?: return
        saveTransaction(
            merchant = bill.name,
            categoryId = bill.categoryId ?: "home",
            accountId = bill.accountId,
            amountMinor = bill.amountMinor,
            flow = Flow.Out,
            date = on,
        )
        val next = nextDue(LocalDate.ofEpochDay(bill.dueEpochDay), Recurrence.of(bill.every), on)
        db.bills().upsert(bill.copy(dueEpochDay = next.toEpochDay(), lastPaidEpochDay = on.toEpochDay()))
    }

    suspend fun deleteBill(id: String) = db.bills().deleteById(id)

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

    suspend fun upsertDebt(debt: Debt) = db.debts().upsert(
        DebtEntity(
            id = debt.id.ifEmpty { UUID.randomUUID().toString() },
            name = debt.name,
            balanceMinor = debt.balanceMinor,
            aprBasisPoints = debt.aprBasisPoints,
            minimumMinor = debt.minimumMinor,
            accountId = debt.accountId,
        )
    )

    suspend fun deleteGoal(id: String) = db.goals().deleteById(id)

    suspend fun deleteDebt(id: String) = db.debts().deleteById(id)

    /**
     * Puts money into a savings goal.
     *
     * When the money comes from a different account this is a real transfer, because it is: the
     * cash physically moves. When it is already sitting in the goal's own account nothing moves
     * and this only earmarks it, so no transaction is invented to describe something that did not
     * happen.
     */
    /**
     * Puts money into a goal, and returns false without writing when it cannot be converted.
     *
     * [GoalEntity.savedMinor] is held in the goal's own account's currency, so an amount sent
     * from elsewhere has to arrive in that currency before it is added. Adding the source figure
     * would credit the goal with whatever number the other account happened to use.
     */
    suspend fun contributeToGoal(goalId: String, fromAccountId: String, amountMinor: Long): Boolean {
        if (amountMinor <= 0L) return false
        val goal = db.goals().byId(goalId) ?: return false
        val goalCurrency = Accounts.currencyOf(goal.accountId)
        val fromCurrency =
            if (fromAccountId.isEmpty()) goalCurrency else Accounts.currencyOf(fromAccountId)
        val credited = crossCurrency(amountMinor, fromCurrency, goalCurrency) ?: return false

        if (fromAccountId.isNotEmpty() && fromAccountId != goal.accountId) {
            if (!transfer(fromAccountId, goal.accountId, amountMinor)) return false
        }
        db.goals().upsert(goal.copy(savedMinor = goal.savedMinor + credited))
        return true
    }

    /**
     * Records a payment against a debt: the money leaves an account, and the balance owed drops.
     *
     * Filed under savings rather than an expense category, because clearing debt raises net worth
     * instead of consuming it -- counting it as spending would make every month you pay down a
     * loan look like a month you overspent.
     */
    /**
     * Records a payment against a debt, and returns false without writing when it cannot convert.
     *
     * The money leaves [fromAccountId] in that account's currency, but the balance owed is in the
     * debt's own. Subtracting the raw figure would clear a debt with the wrong amount of money --
     * paying 100 EUR off a 100 USD debt would settle it exactly, having handed over more.
     */
    suspend fun payDebt(debtId: String, fromAccountId: String, amountMinor: Long): Boolean {
        if (amountMinor <= 0L) return false
        val debt = db.debts().byId(debtId) ?: return false
        val paid = crossCurrency(
            amountMinor,
            Accounts.currencyOf(fromAccountId),
            Accounts.currencyOf(debt.accountId),
        ) ?: return false

        saveTransaction(
            merchant = debt.name,
            categoryId = "savings",
            accountId = fromAccountId,
            amountMinor = amountMinor,
            flow = Flow.Out,
            learn = false,
        )
        db.debts().upsert(
            debt.copy(balanceMinor = (debt.balanceMinor - paid).coerceAtLeast(0L))
        )
        return true
    }

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
