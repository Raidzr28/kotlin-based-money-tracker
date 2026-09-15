package com.moneymanager

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.Flow as MoneyFlow
import com.moneymanager.data.LedgerState
import com.moneymanager.data.MoneyRepository
import com.moneymanager.data.Split
import com.moneymanager.data.db.MoneyDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
}

class MoneyViewModel(private val repo: MoneyRepository) : ViewModel() {

    val state: StateFlow<LedgerState> = repo.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerState(),
    )

    fun logTransaction(
        merchant: String,
        categoryId: String,
        accountId: String,
        amountMinor: Long,
        flow: MoneyFlow,
        note: String? = null,
        tags: List<String> = emptyList(),
        splits: List<Split> = emptyList(),
        onSaved: (String) -> Unit = {},
    ) = viewModelScope.launch {
        val id = repo.saveTransaction(
            merchant = merchant,
            categoryId = categoryId,
            accountId = accountId,
            amountMinor = amountMinor,
            flow = flow,
            note = note,
            tags = tags,
            splits = splits,
        )
        onSaved(id)
    }

    fun deleteTransaction(id: String) = viewModelScope.launch { repo.deleteTransaction(id) }

    fun setBudget(categoryId: String, limitMinor: Long, rollsOver: Boolean = false) =
        viewModelScope.launch { repo.setBudget(categoryId, limitMinor, rollsOver) }

    fun clearBudget(categoryId: String) = viewModelScope.launch { repo.clearBudget(categoryId) }

    /** Populates the ledger with a month of invented activity. Settings only, clearly labelled. */
    fun loadDemoData() = viewModelScope.launch { com.moneymanager.data.DemoData.install(repo) }

    class Factory(private val repo: MoneyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MoneyViewModel(repo) as T
    }
}
