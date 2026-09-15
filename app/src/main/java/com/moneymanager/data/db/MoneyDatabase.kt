package com.moneymanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        AccountEntity::class,
        TxnEntity::class,
        SplitEntity::class,
        TagEntity::class,
        BudgetEntity::class,
        BillEntity::class,
        GoalEntity::class,
        DebtEntity::class,
        MerchantMemoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MoneyDatabase : RoomDatabase() {

    abstract fun txns(): TxnDao
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun budgets(): BudgetDao
    abstract fun bills(): BillDao
    abstract fun goals(): GoalDao
    abstract fun debts(): DebtDao
    abstract fun merchantMemory(): MerchantMemoryDao

    companion object {
        @Volatile
        private var instance: MoneyDatabase? = null

        fun get(context: Context): MoneyDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context) =
            Room.databaseBuilder(context, MoneyDatabase::class.java, "money.db")
                // No fallbackToDestructiveMigration. This database holds the user's own financial
                // record; wiping it to dodge writing a migration is not a trade that is ours to
                // make. A missing migration should fail loudly in development instead.
                .addCallback(Seed)
                .build()
    }

    /**
     * What a brand-new install starts with: the categories most people need on day one, and one
     * cash account so the first transaction has somewhere to go.
     *
     * Deliberately not a demo ledger. A finance app that opens full of invented spending teaches
     * the user to distrust the numbers on it. The empty states are designed for exactly this
     * moment, and Settings carries a separate, clearly-labelled action for loading sample data.
     */
    private object Seed : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            fun category(id: String, label: String, icon: String, order: Int, parent: String? = null) =
                db.execSQL(
                    "INSERT INTO categories (id, label, iconKey, parentId, sortOrder, archived) " +
                        "VALUES (?, ?, ?, ?, ?, 0)",
                    arrayOf(id, label, icon, parent, order),
                )

            category("food", "Food & drink", "restaurant", 0)
            category("groceries", "Groceries", "groceries", 1, parent = "food")
            category("transport", "Transport", "transport", 2)
            category("home", "Home", "home", 3)
            category("utilities", "Utilities", "utilities", 4)
            category("health", "Health", "health", 5)
            category("fun", "Entertainment", "fun", 6)
            category("subs", "Subscriptions", "subs", 7)
            category("travel", "Travel", "travel", 8)
            category("pets", "Pets", "pets", 9)
            category("income", "Income", "income", 10)
            category("savings", "Savings", "savings", 11)

            db.execSQL(
                "INSERT INTO accounts (id, name, kind, openingBalanceMinor, currency, " +
                    "limitMinor, archived, sortOrder) VALUES ('acc_cash', 'Cash', 'Cash', 0, " +
                    "'USD', NULL, 0, 0)"
            )
        }
    }
}
