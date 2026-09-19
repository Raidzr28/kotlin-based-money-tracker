package com.moneymanager.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.absoluteValue

/*
 * The shape the screens read. Deliberately not the shape the database stores: a screen wants a
 * transaction with its splits and tags already attached, the database wants them in their own
 * tables. `data/db/` owns storage, this owns meaning, and the repository is the only thing that
 * knows both.
 *
 * Money is a Long of minor units, everywhere, with no exceptions. Never a Double.
 */

enum class Flow { In, Out, Transfer }

data class Category(
    val id: String,
    val label: String,
    /** Resolved to a drawn glyph by the UI. The model never holds a drawable. */
    val iconKey: String,
    val parent: String? = null,
)

enum class AccountKind(val label: String) {
    Cash("Cash"),
    Bank("Bank"),
    Card("Credit card"),
    Wallet("E-wallet"),
    Savings("Savings");

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: Cash
    }
}

data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    /** Opening balance plus everything logged since. Computed, never stored. */
    val balanceMinor: Long,
    val currency: String = "USD",
    val limitMinor: Long? = null,
)

data class Split(val categoryId: String, val amountMinor: Long)

data class Txn(
    val id: String,
    val merchant: String,
    val categoryId: String,
    val accountId: String,
    /** Signed, in the account's currency. Negative is money leaving. */
    val amountMinor: Long,
    val date: LocalDate,
    val time: LocalTime,
    val flow: Flow = if (amountMinor < 0) Flow.Out else Flow.In,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val splits: List<Split> = emptyList(),
    val currency: String = "USD",
    /** Set when the row arrived in another currency; [amountMinor] is already converted. */
    val originalMinor: Long? = null,
    val originalCurrency: String? = null,
    val hasReceipt: Boolean = false,
)

data class Budget(
    val categoryId: String,
    val limitMinor: Long,
    val spentMinor: Long,
    val rollsOver: Boolean = false,
) {
    val remainingMinor get() = limitMinor - spentMinor
    val fraction get() = if (limitMinor == 0L) 0f else (spentMinor.toFloat() / limitMinor)
    val over get() = spentMinor > limitMinor
}

enum class Recurrence(val label: String) {
    Monthly("Monthly"), Weekly("Weekly"), Yearly("Yearly"), Quarterly("Quarterly");

    fun addTo(date: LocalDate, periods: Long): LocalDate = when (this) {
        Monthly -> date.plusMonths(periods)
        Weekly -> date.plusWeeks(periods)
        Yearly -> date.plusYears(periods)
        Quarterly -> date.plusMonths(periods * 3)
    }

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: Monthly
    }
}

/**
 * When a bill next falls due, once the current period has been paid.
 *
 * Two things this gets right that stepping the date forward one period at a time does not.
 *
 * It always moves at least one period. Paying rent three days early is still paying this month's
 * rent, and a due date that stays put would leave the bill listed as owed and the reminder armed.
 *
 * It counts periods from the original due date rather than from the last date it landed on. A
 * bill due on the 31st that goes unpaid through February would otherwise be pushed to the 28th
 * and stay on the 28th forever, because 31 January plus one month is 28 February and 28 February
 * plus one month is 28 March. Adding N months to the anchor gives 31 March.
 */
fun nextDue(due: LocalDate, every: Recurrence, paidOn: LocalDate): LocalDate {
    var periods = 1L
    while (true) {
        val next = every.addTo(due, periods)
        if (next.isAfter(paidOn)) return next
        periods++
    }
}

data class Bill(
    val id: String,
    val name: String,
    val amountMinor: Long,
    val due: LocalDate,
    val every: Recurrence,
    val accountId: String,
    val subscription: Boolean = false,
    /** Months since this bill was last paid. Null when there is nothing to measure from. */
    val idleMonths: Int? = null,
)

data class Goal(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val savedMinor: Long,
    val by: LocalDate,
    val accountId: String,
    /** A picture the user chose. Null until they do. */
    val imagePath: String? = null,
) {
    val fraction get() = if (targetMinor == 0L) 0f else savedMinor.toFloat() / targetMinor
}

data class Debt(
    val id: String,
    val name: String,
    val balanceMinor: Long,
    val aprBasisPoints: Int,
    val minimumMinor: Long,
)

data class Badge(
    val id: String,
    val name: String,
    val blurb: String,
    val earnedOn: LocalDate?,
    val progress: Float,
)

data class Challenge(
    val id: String,
    val name: String,
    val blurb: String,
    val dayOf: Int,
    val days: Int,
    val savedMinor: Long,
)

data class MerchantTotal(val name: String, val totalMinor: Long, val count: Int)

// --- Reference data ------------------------------------------------------------------------------

/*
 * Categories and accounts are lookup tables: small, read on nearly every row of every screen, and
 * changed only when the user edits them. Rather than thread them through every composable, the
 * repository keeps this snapshot current and the screens read it by id.
 *
 * ponytail: a process-wide snapshot of reference data. Fine while it is refreshed from a single
 * repository on a single database; move to a CompositionLocal if a second writer ever appears, or
 * if a screen needs to render against a version other than the current one.
 */

object Categories {
    private val unknown = Category("unknown", "Uncategorised", "unknown")

    @Volatile
    internal var snapshot: List<Category> = emptyList()

    val all: List<Category> get() = snapshot
    operator fun get(id: String): Category = snapshot.firstOrNull { it.id == id } ?: unknown
}

object Accounts {
    private val unknown = Account("unknown", "Unknown account", AccountKind.Cash, 0)

    @Volatile
    internal var snapshot: List<Account> = emptyList()

    val all: List<Account> get() = snapshot
    operator fun get(id: String): Account = snapshot.firstOrNull { it.id == id } ?: unknown
}

// --- The clock -----------------------------------------------------------------------------------

/*
 * Read on every recomposition rather than captured once, so the app does not think it is still
 * yesterday after midnight.
 */

val today: LocalDate get() = LocalDate.now()
val thisMonth: YearMonth get() = YearMonth.from(today)
val daysInMonth: Int get() = thisMonth.lengthOfMonth()
val dayOfMonth: Int get() = today.dayOfMonth
val daysLeft: Int get() = daysInMonth - dayOfMonth

/** How far through the month the clock is. The pace tick on every budget bar. */
val monthPace: Float get() = dayOfMonth.toFloat() / daysInMonth

/** Year * 100 + month. The database's period key. */
fun YearMonth.periodKey(): Int = year * 100 + monthValue

// --- Formatting ----------------------------------------------------------------------------------

private val symbols = mapOf("USD" to "$", "EUR" to "€", "GBP" to "£", "IDR" to "Rp", "JPY" to "¥")

fun symbolOf(currency: String): String = symbols[currency] ?: "$currency "

/**
 * Splits a minor-unit amount into the parts the money styles render separately.
 *
 * The fraction comes back apart from the rest because it is set dimmer everywhere in this app.
 * Cents are real and must be shown -- rounding money away is the one thing a tracker may never
 * do -- but they are not what the eye should land on first.
 */
data class MoneyParts(val sign: String, val symbol: String, val whole: String, val fraction: String)

fun moneyParts(minor: Long, currency: String = "USD", showSign: Boolean = false): MoneyParts {
    val negative = minor < 0
    val abs = minor.absoluteValue
    val whole = abs / 100
    val cents = (abs % 100).toInt()
    val grouped = buildString {
        val digits = whole.toString()
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
    val sign = when {
        negative -> "−"
        showSign -> "+"
        else -> ""
    }
    return MoneyParts(sign, symbolOf(currency), grouped, ".%02d".format(cents))
}

fun money(minor: Long, currency: String = "USD", showSign: Boolean = false): String =
    with(moneyParts(minor, currency, showSign)) { "$sign$symbol$whole$fraction" }

/** "Today", "Yesterday", or "Fri 5 Sep". Never a bare ISO date in front of a person. */
fun dayLabel(date: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> "${date.dayOfWeek.short()} ${date.dayOfMonth} ${date.month.short()}"
}

fun dueLabel(date: LocalDate): String {
    val days = (date.toEpochDay() - today.toEpochDay()).toInt()
    return when {
        days < -1 -> "${-days} days overdue"
        days == -1 -> "Overdue since yesterday"
        days == 0 -> "Due today"
        days == 1 -> "Due tomorrow"
        days <= 30 -> "In $days days"
        else -> "${date.dayOfMonth} ${date.month.short()}"
    }
}

fun DayOfWeek.short(): String = name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

fun java.time.Month.short(): String = name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
