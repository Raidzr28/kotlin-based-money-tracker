package com.moneymanager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Sum of everything ever logged against one account. */
data class AccountSum(val accountId: String, val totalMinor: Long)

/** Spend rolled up per category for a period, splits included. */
data class CategorySum(val categoryId: String, val totalMinor: Long)

@Dao
interface TxnDao {

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, secondOfDay DESC")
    fun observeAll(): Flow<List<TxnWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM transactions
        WHERE epochDay BETWEEN :fromDay AND :toDay
        ORDER BY epochDay DESC, secondOfDay DESC
        """
    )
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<TxnWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<TxnWithDetails?>

    @Transaction
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY epochDay DESC, secondOfDay DESC")
    fun observeByAccount(accountId: String): Flow<List<TxnWithDetails>>

    /**
     * Running totals per account, done in SQL rather than in Kotlin because it is the one
     * aggregate every screen wants and the only one that has to walk the whole ledger.
     */
    @Query("SELECT accountId, SUM(amountMinor) AS totalMinor FROM transactions GROUP BY accountId")
    fun observeAccountSums(): Flow<List<AccountSum>>

    /**
     * Spend per category for a period. A split transaction contributes to each of its split
     * categories instead of to its own, which is what makes "Groceries" honest when half the
     * supermarket run was birthday candles.
     */
    @Query(
        """
        SELECT categoryId, SUM(amount) AS totalMinor FROM (
            SELECT t.categoryId AS categoryId, -t.amountMinor AS amount
            FROM transactions t
            WHERE t.flow = 'Out' AND t.epochDay BETWEEN :fromDay AND :toDay
              AND NOT EXISTS (SELECT 1 FROM splits s WHERE s.txnId = t.id)
            UNION ALL
            SELECT s.categoryId AS categoryId, -s.amountMinor AS amount
            FROM splits s JOIN transactions t ON t.id = s.txnId
            WHERE t.flow = 'Out' AND t.epochDay BETWEEN :fromDay AND :toDay
        )
        GROUP BY categoryId
        ORDER BY totalMinor DESC
        """
    )
    fun observeCategorySpend(fromDay: Long, toDay: Long): Flow<List<CategorySum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: TxnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<SplitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM splits WHERE txnId = :txnId")
    suspend fun clearSplits(txnId: String)

    @Query("DELETE FROM tags WHERE txnId = :txnId")
    suspend fun clearTags(txnId: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE transferPairId = :pairId OR id = :pairId")
    suspend fun deleteTransferPair(pairId: String)

    /** Write a transaction and its children as one unit, so a split can never outlive its parent. */
    @Transaction
    suspend fun save(txn: TxnEntity, splits: List<SplitEntity>, tags: List<TagEntity>) {
        insert(txn)
        clearSplits(txn.id)
        clearTags(txn.id)
        if (splits.isNotEmpty()) insertSplits(splits)
        if (tags.isNotEmpty()) insertTags(tags)
    }

    @Query("SELECT transferPairId FROM transactions WHERE id = :id")
    suspend fun pairIdOf(id: String): String?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT DISTINCT tag FROM tags ORDER BY tag")
    fun observeTags(): Flow<List<String>>
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY sortOrder, label")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE periodMonth = :periodMonth")
    fun observeForPeriod(periodMonth: Int): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND periodMonth = :periodMonth")
    suspend fun delete(categoryId: String, periodMonth: Int)

    /** Last period's limits, so a new month can start from what the user already decided. */
    @Query("SELECT * FROM budgets WHERE periodMonth = :periodMonth")
    suspend fun forPeriodOnce(periodMonth: Int): List<BudgetEntity>
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE archived = 0 ORDER BY dueEpochDay")
    fun observeAll(): Flow<List<BillEntity>>

    @Upsert
    suspend fun upsert(bill: BillEntity)

    @Query("SELECT * FROM bills WHERE archived = 0 AND dueEpochDay BETWEEN :from AND :to ORDER BY dueEpochDay")
    suspend fun dueBetween(from: Long, to: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun byId(id: String): BillEntity?

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun byId(id: String): GoalEntity?

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY name")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun byId(id: String): DebtEntity?

    @Upsert
    suspend fun upsert(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MerchantMemoryDao {
    @Query("SELECT categoryId FROM merchant_memory WHERE merchant = :merchant")
    suspend fun categoryFor(merchant: String): String?

    @Query(
        """
        INSERT INTO merchant_memory (merchant, categoryId, hits) VALUES (:merchant, :categoryId, 1)
        ON CONFLICT(merchant) DO UPDATE SET categoryId = :categoryId, hits = hits + 1
        """
    )
    suspend fun remember(merchant: String, categoryId: String)
}
