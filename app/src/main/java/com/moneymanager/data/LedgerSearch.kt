package com.moneymanager.data

import java.time.LocalDate
import kotlin.math.absoluteValue

/**
 * What the user is asking the ledger for.
 *
 * Lives here rather than inside the screen because it is a rule about transactions, not about
 * layout: "at least ten pounds" has to mean the same thing whatever is drawing it, and a rule
 * kept in a composable cannot be tested without a device.
 *
 * Every field is inert by default, so an empty query matches everything and each filter narrows
 * from there.
 */
data class LedgerQuery(
    /** Matched against merchant, category label and tags, case-insensitively. */
    val text: String = "",
    /** Null means every direction, including transfers. */
    val flow: Flow? = null,
    /** Inclusive earliest date. Null means as far back as the rows given. */
    val from: LocalDate? = null,
    val accountId: String? = null,
    /** Bounds on the size of the amount, not its sign. Zero means unbounded. */
    val minMinor: Long = 0L,
    val maxMinor: Long = 0L,
    val taggedOnly: Boolean = false,
) {

    /** True when nothing has been narrowed, which is what the ledger opens on. */
    val isWideOpen: Boolean
        get() = text.isBlank() && flow == null && from == null && accountId == null &&
            minMinor == 0L && maxMinor == 0L && !taggedOnly

    fun matches(txn: Txn): Boolean {
        if (text.isNotBlank()) {
            val hit = txn.merchant.contains(text, ignoreCase = true) ||
                Categories[txn.categoryId].label.contains(text, ignoreCase = true) ||
                txn.tags.any { it.contains(text, ignoreCase = true) }
            if (!hit) return false
        }
        if (flow != null && txn.flow != flow) return false
        if (from != null && txn.date < from) return false
        if (accountId != null && txn.accountId != accountId) return false
        if (taggedOnly && txn.tags.isEmpty()) return false

        // On size, never on sign: money out is held negative, and "at least 10" plainly means
        // ten pounds either way rather than "greater than negative ten", which would let every
        // expense through.
        val size = txn.amountMinor.absoluteValue
        if (minMinor > 0L && size < minMinor) return false
        if (maxMinor > 0L && size > maxMinor) return false
        return true
    }

    fun filter(transactions: List<Txn>): List<Txn> = transactions.filter(::matches)
}
