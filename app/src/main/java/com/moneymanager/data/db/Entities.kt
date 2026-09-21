package com.moneymanager.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * The stored shape. It is not the shape the screens read -- `data/Model.kt` owns that -- because
 * the two have different jobs: the screens want a transaction with its splits and tags already
 * attached, and the database wants them in their own tables so a tag can be searched for.
 *
 * Two rules run through every table here.
 *
 * Money is a Long of minor units. Never a Double, never a REAL column: SQLite's REAL is an IEEE
 * double and 0.1 + 0.2 is not 0.3 in it either.
 *
 * Nothing derivable is stored. An account has an opening balance and its transactions; its
 * current balance is the sum of those, computed on read. A budget has a limit; what has been
 * spent against it is a query. Storing a derived total is how a ledger and its summary drift
 * apart, and the first product principle is that the number is right.
 */

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val label: String,
    /** Key into the UI's icon table. The drawable itself never goes near the database. */
    val iconKey: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** [com.moneymanager.data.AccountKind] name. */
    val kind: String,
    /**
     * What the account held before the first logged transaction. The balance you see is this plus
     * everything since, so the ledger is always the authority.
     */
    val openingBalanceMinor: Long,
    val currency: String = "USD",
    val limitMinor: Long? = null,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("epochDay"), Index("accountId"), Index("categoryId"), Index("merchant")],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class TxnEntity(
    @PrimaryKey val id: String,
    val merchant: String,
    val categoryId: String,
    val accountId: String,
    /** Signed minor units in the account's currency. Negative is money leaving. */
    val amountMinor: Long,
    val epochDay: Long,
    val secondOfDay: Int,
    /** [com.moneymanager.data.Flow] name. */
    val flow: String,
    val note: String? = null,
    val currency: String = "USD",
    /** Set when the row arrived in another currency; the amount above is already converted. */
    val originalMinor: Long? = null,
    val originalCurrency: String? = null,
    val rateBasisPoints: Int? = null,
    val receiptPath: String? = null,
    /** The other side of a transfer, so the pair can be shown and deleted together. */
    val transferPairId: String? = null,
    val createdAtEpochSecond: Long = 0,
)

@Entity(
    tableName = "splits",
    indices = [Index("txnId")],
    foreignKeys = [
        ForeignKey(
            entity = TxnEntity::class,
            parentColumns = ["id"],
            childColumns = ["txnId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SplitEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val txnId: String,
    val categoryId: String,
    val amountMinor: Long,
)

/**
 * Tags get their own table rather than a delimited column, because the README asks for
 * cross-category grouping and "every transaction tagged trip:bali" has to be a query, not a
 * string scan that breaks the first time someone types the delimiter.
 */
@Entity(
    tableName = "tags",
    primaryKeys = ["txnId", "tag"],
    indices = [Index("tag")],
    foreignKeys = [
        ForeignKey(
            entity = TxnEntity::class,
            parentColumns = ["id"],
            childColumns = ["txnId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TagEntity(
    val txnId: String,
    val tag: String,
)

/** A transaction with everything hanging off it, in one read. */
data class TxnWithDetails(
    @Embedded val txn: TxnEntity,
    @Relation(parentColumn = "id", entityColumn = "txnId")
    val splits: List<SplitEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "txnId")
    val tags: List<TagEntity> = emptyList(),
)

@Entity(tableName = "budgets", primaryKeys = ["categoryId", "periodMonth"])
data class BudgetEntity(
    val categoryId: String,
    /** Year * 100 + month, so a period sorts and compares as an integer. */
    val periodMonth: Int,
    val limitMinor: Long,
    val rollsOver: Boolean = false,
    /** Carried in from an under-spent previous period when [rollsOver] is set. */
    @ColumnInfo(defaultValue = "0") val rolloverMinor: Long = 0,
)

@Entity(tableName = "bills", indices = [Index("dueEpochDay")])
data class BillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountMinor: Long,
    val dueEpochDay: Long,
    /** [com.moneymanager.data.Recurrence] name. */
    val every: String,
    val accountId: String,
    val categoryId: String? = null,
    val subscription: Boolean = false,
    /** When this bill was last actually paid, so "unused" can be measured rather than guessed. */
    val lastPaidEpochDay: Long? = null,
    val remindDaysBefore: Int = 2,
    val archived: Boolean = false,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetMinor: Long,
    val savedMinor: Long,
    val byEpochDay: Long,
    val accountId: String,
    /** A picture the user chose. Null until they do; never a stock photograph. */
    val imagePath: String? = null,
    val sortOrder: Int = 0,
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val name: String,
    val balanceMinor: Long,
    val aprBasisPoints: Int,
    val minimumMinor: Long,
    val accountId: String? = null,
)

/**
 * Something the user owns that is worth money.
 *
 * Stores the price and the expected life, never the current value: what it is worth today is a
 * function of those two and the clock, and a stored figure would be stale by morning.
 */
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val costMinor: Long,
    val boughtEpochDay: Long,
    val usefulLifeMonths: Int? = null,
    val warrantyUntilEpochDay: Long? = null,
    val accountId: String? = null,
    val sortOrder: Int = 0,
)

/**
 * Merchant to category, learned from the user's own corrections. Local, and only ever used to
 * pre-fill a form the user still confirms.
 */
@Entity(tableName = "merchant_memory")
data class MerchantMemoryEntity(
    @PrimaryKey val merchant: String,
    val categoryId: String,
    val hits: Int = 1,
)

/**
 * A saved transaction shape. Rent, salary, the weekly shop.
 *
 * Holds no date and no history: it is a stencil, not a ledger row. Applying one opens the
 * editor pre-filled, and the transaction it eventually writes is an ordinary [TxnEntity] with
 * no link back here -- deleting a template must never disturb what was already logged.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountMinor: Long,
    val merchant: String,
    val categoryId: String,
    val accountId: String,
    val note: String? = null,
    val sortOrder: Int = 0,
)
