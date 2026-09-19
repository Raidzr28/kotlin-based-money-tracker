package com.moneymanager.data

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.absoluteValue

/*
 * Challenges, derived and never stored.
 *
 * The README's one hard rule for this whole layer is that nothing in it may drift from the real
 * ledger, so none of this is persisted: there is no "joined" flag, no start date, no progress
 * counter. A challenge is a question asked of the transactions, and when the transactions change
 * so does the answer. The cost is that a user cannot opt in to one; the benefit is that the app
 * can never claim a run the ledger does not support, and there is no second source of truth to
 * repair when an import or an undo rewrites history underneath it.
 *
 * Both of these are worth money, and both work out how much from what this person actually
 * spends rather than from a number invented here.
 */

/** Money out per day. Splits do not matter at this resolution: a day's total is a day's total. */
internal fun spendByDay(transactions: List<Txn>): Map<LocalDate, Long> =
    transactions
        .filter { it.flow == Flow.Out }
        .groupBy { it.date }
        .mapValues { (_, rows) -> rows.sumOf { it.amountMinor.absoluteValue } }

/** Median, not mean: one holiday weekend should not become the bar every other weekend is held to. */
internal fun medianOf(values: List<Long>): Long {
    if (values.isEmpty()) return 0L
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

/**
 * A weekend spent on nothing, scored against what this person's weekends usually cost.
 *
 * Only exists on a Saturday or a Sunday. A challenge you can no longer affect is a result, and
 * results belong in Reports. The saving is the median of the last eight complete weekends, which
 * is why a fresh install shows nothing saved rather than a flattering guess.
 */
internal fun weekendChallenge(
    spend: Map<LocalDate, Long>,
    today: LocalDate,
    earliest: LocalDate?,
    history: Int = 8,
): Challenge? {
    val weekday = today.dayOfWeek
    if (weekday != DayOfWeek.SATURDAY && weekday != DayOfWeek.SUNDAY) return null

    val saturday = if (weekday == DayOfWeek.SATURDAY) today else today.minusDays(1)
    fun weekendFrom(sat: LocalDate) = (spend[sat] ?: 0L) + (spend[sat.plusDays(1)] ?: 0L)

    val spentNow = weekendFrom(saturday)

    // Only weekends the ledger was actually keeping. Padding the window with weekends from
    // before the first transaction would quietly halve the median for anyone who installed the
    // app three weeks ago, and understating what a weekend usually costs understates the saving.
    val usual = medianOf(
        (1..history)
            .map { saturday.minusWeeks(it.toLong()) }
            .filter { earliest != null && !it.plusDays(1).isBefore(earliest) }
            .map { weekendFrom(it) }
    )

    return Challenge(
        id = "ch_weekend",
        name = "No-spend weekend",
        blurb = if (spentNow == 0L) {
            "Nothing out since Friday."
        } else {
            "${money(spentNow)} out so far. Your weekends usually cost ${money(usual)}."
        },
        dayOf = if (weekday == DayOfWeek.SATURDAY) 1 else 2,
        days = 2,
        savedMinor = (usual - spentNow).coerceAtLeast(0L),
    )
}

/**
 * Consecutive days, counting back from today, that came in at or under the month's daily pace.
 *
 * Seven, because a week is the shortest span over which a spending habit is visible at all. Today
 * counts while it is still running -- it can break the run later but never retroactively -- which
 * is the rule [LedgerState.loggingStreakDays] already uses for logging.
 *
 * Needs a budget and a ledger. Without a budget there is no pace to come in under, and without
 * transactions there is no evidence of anything -- an empty week is not a lean week.
 */
internal fun leanRunChallenge(
    spend: Map<LocalDate, Long>,
    today: LocalDate,
    paceMinor: Long,
    earliest: LocalDate?,
    days: Int = 7,
): Challenge? {
    if (paceMinor <= 0L || earliest == null) return null

    var run = 0
    var saved = 0L
    var day = today
    while (run < days) {
        // A day before the ledger began is not a lean day, it is a day with no record. Counting
        // it would hand someone who installed the app this morning a week of discipline.
        if (day.isBefore(earliest)) break
        val spent = spend[day] ?: 0L
        if (spent > paceMinor) break
        run++
        saved += paceMinor - spent
        day = day.minusDays(1)
    }
    if (run == 0) return null

    return Challenge(
        id = "ch_lean",
        name = "Seven lean days",
        blurb = "Days at or under ${money(paceMinor)}, which is this month's even pace.",
        dayOf = run,
        days = days,
        savedMinor = saved,
    )
}

fun challengesFor(
    transactions: List<Txn>,
    today: LocalDate,
    paceMinor: Long,
): List<Challenge> {
    val spend = spendByDay(transactions)
    val earliest = transactions.minOfOrNull { it.date }
    return listOfNotNull(
        weekendChallenge(spend, today, earliest),
        leanRunChallenge(spend, today, paceMinor, earliest),
    )
}
