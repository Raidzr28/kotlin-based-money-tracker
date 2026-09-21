package com.moneymanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        TemplateEntity::class,
        AssetEntity::class,
    ],
    version = 3,
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
    abstract fun templates(): TemplateDao
    abstract fun assets(): AssetDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(Seed)
                .build()

        /**
         * Adds the assets table. Additive, like the one before it: an installed copy keeps every
         * row it had, and comes back with an empty asset list rather than a migration failure.
         *
         * The DDL is the generated schema's, verbatim (app/schemas/.../3.json). Room compares
         * the migrated table against that file column for column and throws on any difference,
         * including a default this entity does not declare.
         */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assets` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`costMinor` INTEGER NOT NULL, " +
                        "`boughtEpochDay` INTEGER NOT NULL, " +
                        "`usefulLifeMonths` INTEGER, " +
                        "`warrantyUntilEpochDay` INTEGER, " +
                        "`accountId` TEXT, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        /**
         * Adds the recurring-template table. Purely additive: no existing column is touched, so
         * every ledger row, budget and bill on an installed copy survives untouched.
         */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Copied from the generated schema (app/schemas/.../2.json) rather than
                // written by hand. Room compares the migrated table against that file column
                // for column, defaults included -- a stray DEFAULT 0 here reads as a different
                // table and throws on the first launch after an upgrade.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `templates` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`amountMinor` INTEGER NOT NULL, " +
                        "`merchant` TEXT NOT NULL, " +
                        "`categoryId` TEXT NOT NULL, " +
                        "`accountId` TEXT NOT NULL, " +
                        "`note` TEXT, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }
    }

    /**
     * What a brand-new install starts with: the categories most people need on day one, and one
     * cash account so the first transaction has somewhere to go.
     *
     * Deliberately not a demo ledger. A finance app that opens full of invented spending teaches
     * the user to distrust the numbers on it. The empty states are designed for exactly this
     * moment, and Settings carries a separate, clearly-labelled action for loading sample data.
     */
    internal object Seed : RoomDatabase.Callback() {

        /** The day-one categories, in the order they are offered. */
        val categories: List<CategoryEntity> = listOf(
            CategoryEntity("food", "Food & drink", "restaurant", null, 0),
            CategoryEntity("groceries", "Groceries", "groceries", "food", 1),
            CategoryEntity("transport", "Transport", "transport", null, 2),
            CategoryEntity("home", "Home", "home", null, 3),
            CategoryEntity("utilities", "Utilities", "utilities", null, 4),
            CategoryEntity("health", "Health", "health", null, 5),
            CategoryEntity("fun", "Entertainment", "fun", null, 6),
            CategoryEntity("subs", "Subscriptions", "subs", null, 7),
            CategoryEntity("travel", "Travel", "travel", null, 8),
            CategoryEntity("pets", "Pets", "pets", null, 9),
            CategoryEntity("income", "Income", "income", null, 10),
            CategoryEntity("savings", "Savings", "savings", null, 11),
        )

        val firstAccount = AccountEntity(
            id = "acc_cash",
            name = "Cash",
            kind = "Cash",
            openingBalanceMinor = 0,
            currency = "USD",
            limitMinor = null,
        )

        override fun onCreate(db: SupportSQLiteDatabase) {
            categories.forEach {
                db.execSQL(
                    "INSERT INTO categories (id, label, iconKey, parentId, sortOrder, archived) " +
                        "VALUES (?, ?, ?, ?, ?, 0)",
                    arrayOf(it.id, it.label, it.iconKey, it.parentId, it.sortOrder),
                )
            }
            db.execSQL(
                "INSERT INTO accounts (id, name, kind, openingBalanceMinor, currency, " +
                    "limitMinor, archived, sortOrder) VALUES ('acc_cash', 'Cash', 'Cash', 0, " +
                    "'USD', NULL, 0, 0)"
            )
        }

        /**
         * The same starting point, after a wipe rather than on first run.
         *
         * [onCreate] fires only when the file is created, and `clearAllTables` does not recreate
         * it -- so erasing everything would otherwise leave an install with no categories to
         * file a transaction under and no account to put it on.
         */
        suspend fun reseed(db: MoneyDatabase) {
            db.categories().upsertAll(categories)
            db.accounts().upsert(firstAccount)
        }
    }
}
