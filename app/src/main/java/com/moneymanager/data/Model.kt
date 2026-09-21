package com.moneymanager.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
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
) {
    /*
     * Derived rather than stored, here and on [Goal] and [Debt].
     *
     * A bill is paid out of an account, and paying one writes a transaction on that account in
     * that account's currency -- so [amountMinor] is already denominated there. A currency column
     * of its own would be a second place for the same fact to live, free to disagree with the
     * first the moment somebody edits the account. Nothing derivable is stored.
     */
    val currency: String get() = Accounts.currencyOf(accountId)
}

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

    /** The account the saving sits in decides what the saving is denominated in. */
    val currency: String get() = Accounts.currencyOf(accountId)
}

data class Debt(
    val id: String,
    val name: String,
    val balanceMinor: Long,
    val aprBasisPoints: Int,
    val minimumMinor: Long,
    /** The account this debt is settled against, when there is one. */
    val accountId: String? = null,
) {
    val currency: String get() = Accounts.currencyOf(accountId)
}

/**
 * Something you own that is worth money. A motorbike, a laptop, a fridge.
 *
 * Bought for a figure and worth less every year, which is the whole reason it is here: spending
 * two thousand on a laptop is not two thousand gone, it is two thousand turned into a thing that
 * gives the money back slowly. An account balance alone cannot say that.
 *
 * Nothing about the value is stored. [costMinor] and [usefulLifeMonths] are what the user typed;
 * what it is worth today is computed from them and the clock, like every other figure in this
 * app -- a stored current value would be a second place for the truth to live and would be wrong
 * the morning after it was written.
 */
data class Asset(
    val id: String,
    val name: String,
    /** What it cost. Positive: this is a price, not a ledger entry. */
    val costMinor: Long,
    val boughtOn: LocalDate,
    /**
     * How long it is expected to be worth anything, in months.
     *
     * Null means it is not expected to lose value -- land, tools, a bike you would sell for what
     * you paid. Those hold [costMinor] until the user says otherwise, which is honest: guessing
     * a decline for something the user did not say declines would invent a number.
     */
    val usefulLifeMonths: Int? = null,
    /** Null when there is no warranty, or the user did not record one. */
    val warrantyUntil: LocalDate? = null,
    /** The account it was bought from, when the user said. Decides what it is denominated in. */
    val accountId: String? = null,
) {
    /** The same rule as [Bill], [Goal] and [Debt]: the account decides the currency. */
    val currency: String get() = Accounts.currencyOf(accountId)

    /**
     * What it was worth on [date]: straight line from [costMinor] at [boughtOn] to nothing at
     * the end of its useful life.
     *
     * Whole months elapsed rather than days, so the figure holds still for a month at a time
     * instead of ticking down every morning -- a net worth that changes while you watch it
     * invites you to distrust it. Zero before it was bought, so a twelve-month history does not
     * credit you with a laptop you had not bought yet.
     */
    fun valueOn(date: LocalDate = today): Long {
        if (date < boughtOn) return 0L
        val life = usefulLifeMonths ?: return costMinor
        if (life <= 0) return 0L
        val elapsed = ChronoUnit.MONTHS.between(boughtOn, date)
        if (elapsed >= life) return 0L
        return costMinor * (life - elapsed) / life
    }

    /** What it is worth today. */
    val valueMinor: Long get() = valueOn()

    /** How much of the price has been used up. Null when it is not set to decline. */
    val depreciatedMinor: Long? get() = usefulLifeMonths?.let { costMinor - valueMinor }

    val underWarranty: Boolean get() = warrantyUntil?.let { it >= today } == true
}

/**
 * A transaction shape the user saves once and logs with one tap.
 *
 * Deliberately not an auto-posting rule. Rent and salary are the examples the spec gives, and
 * both are exactly the kind of row that must not appear in the ledger without the user seeing
 * it: the amount changes, the date slips, and a figure that posted itself is a figure nobody
 * checked. Using a template opens the editor already filled in -- one tap to a correct row,
 * still a confirmation before it is written.
 */
data class Template(
    val id: String,
    val name: String,
    /** Signed, in the account's currency, like [Txn.amountMinor]. Zero means "ask me". */
    val amountMinor: Long,
    val merchant: String,
    val categoryId: String,
    val accountId: String,
    val note: String? = null,
    val sortOrder: Int = 0,
) {
    val flow: Flow get() = if (amountMinor < 0) Flow.Out else Flow.In
    val currency: String get() = Accounts.currencyOf(accountId)
}

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

/*
 * What the app renders money in when a call site does not say.
 *
 * The same snapshot trick as [Categories] and [Accounts], and for the same reason: sixty-seven
 * call sites render a figure, and threading a currency through every one of them to change a
 * setting nobody sets twice is work with no payoff. The repository keeps this current.
 *
 * ponytail: a process-wide display currency. It is only ever read for formatting -- every
 * conversion goes through [Conversion], which is passed explicitly -- so a stale value here can
 * mislabel a figure but can never change one.
 */
object Money {
    @Volatile
    var base: String = "USD"
}

object Accounts {
    private val unknown = Account("unknown", "Unknown account", AccountKind.Cash, 0)

    @Volatile
    internal var snapshot: List<Account> = emptyList()

    val all: List<Account> get() = snapshot
    operator fun get(id: String): Account = snapshot.firstOrNull { it.id == id } ?: unknown

    /**
     * The currency an account holds, or the display base when there is no such account.
     *
     * Not [get]'s fallback, which reports "USD" for a missing account and would quietly relabel
     * a bill whose account was deleted.
     */
    fun currencyOf(id: String?): String =
        snapshot.firstOrNull { it.id == id }?.currency ?: Money.base
}

// --- The clock -----------------------------------------------------------------------------------

/*
 * Read on every recomposition rather than captured once, so the app does not think it is still
 * yesterday after midnight.
 */

/**
 * Where the user's month and week begin.
 *
 * The same process-wide snapshot as [Money] and [Categories], kept current by the repository and
 * seeded from [AppPrefs] at startup. It lives here rather than being threaded through because
 * every figure below is derived from it, and "the month" is read on nearly every screen.
 *
 * ponytail: a process-wide period setting. Fine while one repository owns it; move to a
 * CompositionLocal if a screen ever needs to render one cycle while another renders a different
 * one (a history browser would).
 */
object Periods {

    /** [monthStartDay] value meaning "read the day off the ledger's income rows". */
    const val PAYDAY = 0

    /** Day of the calendar month a cycle opens on, or [PAYDAY]. */
    @Volatile
    var monthStartDay: Int = 1

    @Volatile
    var weekStart: DayOfWeek = DayOfWeek.MONDAY

    /** Derived from real income when [monthStartDay] is [PAYDAY]. Set by the repository. */
    @Volatile
    internal var paydayDay: Int = 1

    /**
     * The effective opening day, clamped to 1..28.
     *
     * Past the 28th there is no such day in February, and a cycle that silently skips a month is
     * worse than one that opens a few days early. The offered choices are all inside the clamp;
     * it exists for a derived payday that lands on the 31st.
     */
    val startDay: Int get() = (if (monthStartDay == PAYDAY) paydayDay else monthStartDay).coerceIn(1, 28)
}

/** The two period settings as one value, so a change to either re-emits the ledger once. */
data class PeriodSettings(
    val monthStartDay: Int = 1,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
)

val today: LocalDate get() = LocalDate.now()

/** The first day of the budget cycle that contains [date]. */
fun cycleStartOn(date: LocalDate): LocalDate {
    val opening = date.withDayOfMonth(minOf(Periods.startDay, date.lengthOfMonth()))
    if (!date.isBefore(opening)) return opening
    val previous = date.minusMonths(1)
    return previous.withDayOfMonth(minOf(Periods.startDay, previous.lengthOfMonth()))
}

/** First day of the cycle the user is currently in. The 1st, unless they moved it. */
val cycleStart: LocalDate get() = cycleStartOn(today)

/** Last day of the current cycle, inclusive. */
val cycleEnd: LocalDate get() = cycleStart.plusMonths(1).minusDays(1)

/**
 * The calendar month a cycle is filed under, and so the key budgets are stored against.
 *
 * A cycle opening on 25 January is "January", which keeps [periodKey] one row per cycle and
 * means moving the start day never orphans a budget that was already set.
 */
val thisMonth: YearMonth get() = YearMonth.from(cycleStart)

/** Days in the current cycle. 28 to 31, like a month, because a cycle is a month long. */
val daysInMonth: Int get() = ChronoUnit.DAYS.between(cycleStart, cycleStart.plusMonths(1)).toInt()

/** How many days into the cycle today is, counting from 1. */
val dayOfMonth: Int get() = (ChronoUnit.DAYS.between(cycleStart, today) + 1).toInt()

val daysLeft: Int get() = daysInMonth - dayOfMonth

/** How far through the month the clock is. The pace tick on every budget bar. */
val monthPace: Float get() = dayOfMonth.toFloat() / daysInMonth

/** Where [date] sits in the current cycle, counting from 1. Outside it, this is out of range. */
fun cycleDayOf(date: LocalDate): Int = (ChronoUnit.DAYS.between(cycleStart, date) + 1).toInt()

/** Which column [date] falls in on a calendar that starts its week on [start]. */
fun weekColumnOf(date: LocalDate, start: DayOfWeek = Periods.weekStart): Int =
    Math.floorMod(date.dayOfWeek.value - start.value, 7)

/** Weekday initials in the user's own order, for a calendar header. */
fun weekdayInitials(start: DayOfWeek = Periods.weekStart): List<String> =
    (0..6).map { DayOfWeek.of((start.value - 1 + it) % 7 + 1).short().take(1) }

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

fun moneyParts(minor: Long, currency: String = Money.base, showSign: Boolean = false): MoneyParts {
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

fun money(minor: Long, currency: String = Money.base, showSign: Boolean = false): String =
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
