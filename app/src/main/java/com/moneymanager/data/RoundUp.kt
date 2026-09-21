package com.moneymanager.data

import java.time.LocalDate
import kotlin.math.absoluteValue

/*
 * Round-ups: the change from every expense, put aside.
 *
 * Qapital's trick, with this app's rule attached. The change is *derived* from transactions the
 * user already logged -- there is no second ledger of round-ups accruing somewhere -- and it is
 * never moved without a tap. An app that quietly moves money out of a current account because a
 * rule said so is exactly the "posted without the user confirming" failure the product brief
 * refuses, and it is how people end up overdrawn by a savings feature.
 *
 * One thing is stored: the day of the last sweep. That is a fact about something that happened,
 * not a derived total, and without it the same change would be offered again every time.
 */

/** The change from one spend: what it would take to reach the next multiple of [toMinor]. */
fun changeFrom(amountMinor: Long, toMinor: Int): Long {
    if (toMinor <= 1) return 0L
    val size = amountMinor.absoluteValue
    val over = size % toMinor
    // An amount already on the boundary generates nothing: rounding 5.00 up to 6.00 would be
    // inventing a saving rather than collecting change.
    return if (over == 0L) 0L else toMinor - over
}

/**
 * Every unswept expense's change, added up in the base currency.
 *
 * Only money out counts. Rounding up income would take money *out* of savings on payday, and a
 * transfer between the user's own accounts is not a spend at all.
 *
 * Rows in a currency with no rate to the base are skipped rather than added raw, the same rule
 * every other total in this app follows -- so a sweep is never a figure the ledger cannot back.
 */
fun roundUpsSince(
    transactions: List<Txn>,
    since: LocalDate?,
    toMinor: Int,
    conversion: Conversion = Conversion(),
): Long {
    if (toMinor <= 1) return 0L
    return transactions
        .filter { it.flow == Flow.Out }
        .filter { since == null || it.date > since }
        .sumOf { txn ->
            val base = conversion.toBase(txn.amountMinor, txn.currency) ?: return@sumOf 0L
            changeFrom(base, toMinor)
        }
}

/** The choices offered, and what each is called. */
val ROUND_UP_STEPS: List<Pair<Int, String>> = listOf(
    100 to "Nearest 1",
    200 to "Nearest 2",
    500 to "Nearest 5",
    1000 to "Nearest 10",
)
