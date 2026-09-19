package com.moneymanager.data

import kotlin.math.absoluteValue

/*
 * Writing the ledger out, which is the other half of reading one in.
 *
 * The app already imports CSV; a money tracker that can only be written to is a trap, and "your
 * data is yours" is a claim you have to be able to act on. The columns are deliberately the ones
 * [inferColumns] knows how to read back, so an export from this app imports into it cleanly, and
 * into a spreadsheet without an import wizard.
 *
 * Amounts are written as a plain signed decimal with a dot and no thousands separator. That is
 * not a locale preference, it is the one form every spreadsheet and every parser agrees on.
 */

private val HEADER = listOf(
    "Date", "Time", "Merchant", "Category", "Account", "Amount", "Currency", "Note", "Tags",
)

/** RFC 4180: quote only when the cell would otherwise break the row, and double inner quotes. */
internal fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

/** Minor units to a signed decimal. Kept off Double: 0.1 + 0.2 has no place in a ledger. */
internal fun minorToDecimal(minor: Long): String {
    val sign = if (minor < 0) "-" else ""
    val whole = (minor / 100).absoluteValue
    val cents = (minor % 100).absoluteValue
    return "%s%d.%02d".format(sign, whole, cents)
}

fun ledgerCsv(transactions: List<Txn>): String {
    val out = StringBuilder()
    out.append(HEADER.joinToString(",")).append("\r\n")
    transactions
        .sortedWith(compareByDescending<Txn> { it.date }.thenByDescending { it.time })
        .forEach { txn ->
            val cells = listOf(
                txn.date.toString(),
                "%02d:%02d".format(txn.time.hour, txn.time.minute),
                txn.merchant,
                Categories[txn.categoryId].label,
                Accounts[txn.accountId].name,
                minorToDecimal(txn.amountMinor),
                txn.currency,
                txn.note.orEmpty(),
                txn.tags.joinToString(" "),
            )
            out.append(cells.joinToString(",") { csvCell(it) }).append("\r\n")
        }
    return out.toString()
}
