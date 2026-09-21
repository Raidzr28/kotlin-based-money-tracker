package com.moneymanager

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.moneymanager.data.Money
import com.moneymanager.data.MoneyRepository
import com.moneymanager.data.ReceiptGuess
import com.moneymanager.data.Rates
import com.moneymanager.data.RatesStore
import com.moneymanager.data.SecurityStore
import com.moneymanager.data.createReminderChannel
import com.moneymanager.data.scheduleReminders
import com.moneymanager.data.scheduleStreakWarnings
import com.moneymanager.data.Debt
import com.moneymanager.data.ForeignAmount
import com.moneymanager.data.Goal
import com.moneymanager.data.ImportCandidate
import com.moneymanager.data.Split
import com.moneymanager.data.Template
import com.moneymanager.data.ThemeMode
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
    val repository: MoneyRepository by lazy {
        MoneyRepository(MoneyDatabase.get(this), rates, prefs.periods)
    }
    val security: SecurityStore by lazy { SecurityStore(this) }
    val prefs: AppPrefs by lazy { AppPrefs(this) }
    val rates: RatesStore by lazy { RatesStore(this) }

    /** Compose-observable appearance, so Settings repaints the app on the tap rather than next launch. */
    val appearance: AppearanceState by lazy { AppearanceState(prefs) }

    override fun onCreate() {
        super.onCreate()
        // Before the first frame: every figure on the first screen is derived from the cycle,
        // and reading them against the calendar and then correcting would be a visible flicker.
        prefs.publishPeriods()
        // The channel has to exist before anything posts to it; posting to a missing channel is
        // dropped silently. Re-registering an existing one is a no-op, so this is safe every start.
        createReminderChannel(this)
        // Re-asserted at startup rather than only when the toggle changes, so the daily check
        // survives a reboot, a force-stop, or the user clearing the app's tasks.
        scheduleReminders(this, prefs)
        scheduleStreakWarnings(this, prefs)
    }
}

/**
 * The appearance settings, mirrored into Compose state.
 *
 * [AppPrefs] is SharedPreferences, which Compose cannot observe. Rather than move the whole
 * settings store to DataStore for two booleans read once a session, the two values that have to
 * repaint the app live here and are written through to prefs on every change.
 */
class AppearanceState(private val prefs: AppPrefs) {

    var themeMode by mutableStateOf(prefs.themeMode)
        private set

    var materialYou by mutableStateOf(prefs.materialYou)
        private set

    /*
     * `use*` rather than `set*`: a Compose `var` with a private setter already generates
     * setThemeMode/setMaterialYou on the JVM, and an explicit setMaterialYou would collide
     * with it. Naming both the same way keeps the pair readable at the call site.
     */

    fun useTheme(mode: ThemeMode) {
        prefs.themeMode = mode
        themeMode = mode
    }

    fun useMaterialYou(on: Boolean) {
        prefs.materialYou = on
        materialYou = on
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

    /*
     * These three move money across a currency boundary and can refuse. [onDone] carries that
     * decision back so a screen can say so: a transfer that silently does not happen is the worst
     * outcome available, because the user believes it did.
     */

    fun transfer(
        fromAccountId: String,
        toAccountId: String,
        amountMinor: Long,
        note: String? = null,
        onDone: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        val ok = repo.transfer(fromAccountId, toAccountId, amountMinor, note = note)
        refreshWidget()
        onDone(ok)
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

    /** The same hand-off as a receipt scan, for a template the user tapped. */
    private val _pendingTemplate = MutableStateFlow<Template?>(null)
    val pendingTemplate = _pendingTemplate.asStateFlow()

    fun offerTemplate(template: Template) { _pendingTemplate.value = template }

    fun consumeTemplate() { _pendingTemplate.value = null }

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

    fun contributeToGoal(
        goalId: String,
        fromAccountId: String,
        amountMinor: Long,
        onDone: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        val ok = repo.contributeToGoal(goalId, fromAccountId, amountMinor)
        refreshWidget()
        onDone(ok)
    }

    fun saveAsset(asset: com.moneymanager.data.Asset) =
        viewModelScope.launch { repo.upsertAsset(asset) }

    fun deleteAsset(id: String) = viewModelScope.launch { repo.deleteAsset(id) }

    fun saveDebt(debt: Debt) = viewModelScope.launch { repo.upsertDebt(debt) }

    fun deleteDebt(id: String) = viewModelScope.launch { repo.deleteDebt(id) }

    fun payDebt(
        debtId: String,
        fromAccountId: String,
        amountMinor: Long,
        onDone: (Boolean) -> Unit = {},
    ) = viewModelScope.launch {
        val ok = repo.payDebt(debtId, fromAccountId, amountMinor)
        refreshWidget()
        onDone(ok)
    }

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
        currency: String = Money.base,
    ) = viewModelScope.launch {
        repo.upsertAccount(
            Account(
                id = id,
                name = name.trim(),
                kind = kind,
                balanceMinor = 0,          // derived on read; the opening balance is what is stored
                currency = currency,
                limitMinor = limitMinor,
            ),
            openingBalanceMinor,
        )
    }

    /** Populates the ledger with a month of invented activity. Settings only, clearly labelled. */
    fun loadDemoData() = viewModelScope.launch { com.moneymanager.data.DemoData.install(repo) }

    val templates: StateFlow<List<Template>> = repo.templates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun saveTemplate(template: Template) = viewModelScope.launch { repo.upsertTemplate(template) }

    fun deleteTemplate(id: String) = viewModelScope.launch { repo.deleteTemplate(id) }

    fun saveCategory(category: com.moneymanager.data.Category, sortOrder: Int) =
        viewModelScope.launch { repo.upsertCategory(category, sortOrder) }

    fun archiveCategory(id: String) = viewModelScope.launch { repo.archiveCategory(id) }

    /** Erases the ledger and puts the day-one categories and cash account back. */
    fun deleteEverything(onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.deleteEverything()
        onDone()
    }

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
