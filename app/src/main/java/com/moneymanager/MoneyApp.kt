package com.moneymanager

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import com.moneymanager.data.today
import java.time.LocalDate
import java.time.LocalTime
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.Account
import com.moneymanager.data.AccountKind
import com.moneymanager.data.AppPrefs
import com.moneymanager.data.Bill
import com.moneymanager.data.Flow as MoneyFlow
import com.moneymanager.data.LedgerState
import com.moneymanager.data.MoneyRepository
import com.moneymanager.data.ReceiptGuess
import com.moneymanager.data.Rates
import com.moneymanager.data.RatesStore
import com.moneymanager.data.SecurityStore
import com.moneymanager.data.createReminderChannel
import com.moneymanager.data.scheduleReminders
import com.moneymanager.data.Debt
import com.moneymanager.data.ForeignAmount
import com.moneymanager.data.Goal
import com.moneymanager.data.ImportCandidate
import com.moneymanager.data.Split
import com.moneymanager.data.db.MoneyDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.glance.appwidget.updateAll
import com.moneymanager.widget.MoneyWidget
import kotlinx.coroutines.launch

/**
 * The whole dependency graph.
 *
 * Twenty lines instead of Hilt, because there is one database, one repository and one view model.
 * Annotations and a code-generation round would buy nothing here; if a second data source or a
 * per-feature scope ever appears, swapping this for Hilt is mechanical and nothing above it
 * changes.
 */
class MoneyApp : Application() {
    val repository: MoneyRepository by lazy { MoneyRepository(MoneyDatabase.get(this)) }
    val security: SecurityStore by lazy { SecurityStore(this) }
    val prefs: AppPrefs by lazy { AppPrefs(this) }
    val rates: RatesStore by lazy { RatesStore(this) }

    override fun onCreate() {
        super.onCreate()
        // The channel has to exist before anything posts to it; posting to a missing channel is
        // dropped silently. Re-registering an existing one is a no-op, so this is safe every start.
        createReminderChannel(this)
        // Re-asserted at startup rather than only when the toggle changes, so the daily check
        // survives a reboot, a force-stop, or the user clearing the app's tasks.
        scheduleReminders(this, prefs)
    }
}

class MoneyViewModel(
    private val repo: MoneyRepository,
    private val appContext: Context,
) : ViewModel() {

    val state: StateFlow<LedgerState> = repo.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerState(),
    )

    fun transfer(fromAccountId: String, toAccountId: String, amountMinor: Long, note: String? = null) =
        viewModelScope.launch {
            repo.transfer(fromAccountId, toAccountId, amountMinor, note = note)
            refreshWidget()
        }

    fun logTransaction(
        id: String? = null,
        merchant: String,
        categoryId: String,
        accountId: String,
        amountMinor: Long,
        flow: MoneyFlow,
        note: String? = null,
        tags: List<String> = emptyList(),
        splits: List<Split> = emptyList(),
        foreign: ForeignAmount? = null,
        /** Only passed when restoring or duplicating a row; new rows are stamped with now. */
        date: LocalDate? = null,
        time: LocalTime? = null,
        onSaved: (String) -> Unit = {},
    ) = viewModelScope.launch {
        val saved = repo.saveTransaction(
            id = id,
            merchant = merchant,
            categoryId = categoryId,
            accountId = accountId,
            amountMinor = amountMinor,
            flow = flow,
            date = date ?: today,
            time = time ?: LocalTime.now(),
            note = note,
            tags = tags,
            splits = splits,
            originalMinor = foreign?.minor,
            originalCurrency = foreign?.currency,
            rateMicros = foreign?.rateMicros,
        )
        onSaved(saved)
        refreshWidget()
    }

    /**
     * What a scan read, waiting for the editor to pick it up.
     *
     * Held here rather than passed through the navigation route: a receipt guess is a handful of
     * fields including free text, and encoding that into a URL to decode it back out is a bug
     * waiting for the first merchant with a slash in its name.
     */
    private val _pendingScan = MutableStateFlow<ReceiptGuess?>(null)
    val pendingScan = _pendingScan.asStateFlow()

    fun offerScan(guess: ReceiptGuess) { _pendingScan.value = guess }

    fun consumeScan() { _pendingScan.value = null }

    fun deleteTransaction(id: String) = viewModelScope.launch {
        repo.deleteTransaction(id)
        refreshWidget()
    }

    fun importStatement(
        candidates: List<ImportCandidate>,
        accountId: String,
        fallbackCategoryId: String,
        onDone: (Int) -> Unit = {},
    ) = viewModelScope.launch {
        onDone(repo.importStatement(candidates.map { it.row }, accountId, fallbackCategoryId))
    }

    fun saveGoal(goal: Goal) = viewModelScope.launch { repo.upsertGoal(goal) }

    fun deleteGoal(id: String) = viewModelScope.launch { repo.deleteGoal(id) }

    fun contributeToGoal(goalId: String, fromAccountId: String, amountMinor: Long) =
        viewModelScope.launch { repo.contributeToGoal(goalId, fromAccountId, amountMinor) }

    fun saveDebt(debt: Debt) = viewModelScope.launch { repo.upsertDebt(debt) }

    fun deleteDebt(id: String) = viewModelScope.launch { repo.deleteDebt(id) }

    fun payDebt(debtId: String, fromAccountId: String, amountMinor: Long) =
        viewModelScope.launch { repo.payDebt(debtId, fromAccountId, amountMinor) }

    fun saveBill(bill: Bill) = viewModelScope.launch { repo.upsertBill(bill) }

    fun payBill(id: String) = viewModelScope.launch {
        repo.payBill(id)
        refreshWidget()
    }

    fun deleteBill(id: String) = viewModelScope.launch { repo.deleteBill(id) }

    fun setBudget(categoryId: String, limitMinor: Long, rollsOver: Boolean = false) =
        viewModelScope.launch {
            repo.setBudget(categoryId, limitMinor, rollsOver)
            refreshWidget()
        }

    fun clearBudget(categoryId: String) = viewModelScope.launch { repo.clearBudget(categoryId) }

    fun saveAccount(
        name: String,
        kind: AccountKind,
        openingBalanceMinor: Long,
        limitMinor: Long?,
        id: String = "",
    ) = viewModelScope.launch {
        repo.upsertAccount(
            Account(
                id = id,
                name = name.trim(),
                kind = kind,
                balanceMinor = 0,          // derived on read; the opening balance is what is stored
                limitMinor = limitMinor,
            ),
            openingBalanceMinor,
        )
    }

    /** Populates the ledger with a month of invented activity. Settings only, clearly labelled. */
    fun loadDemoData() = viewModelScope.launch { com.moneymanager.data.DemoData.install(repo) }

    /**
     * Redraws the home-screen widget.
     *
     * Called after every write rather than left to the half-hourly refresh, because a figure on
     * someone's home screen that is thirty minutes behind their ledger is simply a wrong figure.
     */
    private fun refreshWidget() = viewModelScope.launch {
        runCatching { MoneyWidget().updateAll(appContext) }
    }

    class Factory(
        private val repo: MoneyRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MoneyViewModel(repo, appContext) as T
    }
}
