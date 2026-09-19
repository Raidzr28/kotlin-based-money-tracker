package com.moneymanager.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.absoluteValue

/*
 * Reading a bank's CSV export.
 *
 * There is no standard here. Every bank picks its own column order, date format, decimal
 * separator and way of writing a negative number, so nothing in this file assumes a shape: it
 * infers one, shows the user what it inferred, and lets them correct it before anything is
 * written.
 *
 * Everything parses straight to Long minor units. A statement is the one place a badly parsed
 * number would be believed without question, so it never passes through a Double.
 */

/** One line of a statement, as read. */
data class StatementRow(
    val date: LocalDate,
    val description: String,
    val amountMinor: Long,
    /** The raw cells, kept so the preview can show what a row actually said. */
    val raw: List<String>,
)

/** A row matched against what is already in the ledger. */
data class ImportCandidate(
    val row: StatementRow,
    /** An existing transaction this looks like, if any. */
    val duplicateOf: Txn?,
    val include: Boolean,
)

/** Which column means what. Inferred, then confirmed by the user. */
data class ColumnMap(
    val date: Int = -1,
    val description: Int = -1,
    val amount: Int = -1,
    /** Some banks use two columns instead of a signed one. */
    val debit: Int = -1,
    val credit: Int = -1,
) {
    val usable: Boolean get() = date >= 0 && description >= 0 && (amount >= 0 || debit >= 0 || credit >= 0)
}

data class ParsedStatement(
    val header: List<String>,
    val rows: List<List<String>>,
    val map: ColumnMap,
)

// --- CSV -----------------------------------------------------------------------------------------

/**
 * A small RFC 4180 reader: quoted fields, embedded delimiters, doubled quotes, CRLF.
 *
 * Hand-written rather than pulled in, because the whole grammar is the four rules above and a
 * dependency would be more code to audit than this is.
 */
fun parseCsv(text: String, delimiter: Char = detectDelimiter(text)): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0

    fun endCell() { row.add(cell.toString().trim()); cell.setLength(0) }
    fun endRow() { endCell(); if (row.any { it.isNotEmpty() }) rows.add(row); row = mutableListOf() }

    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
            c == '"' -> quoted = !quoted
            !quoted && c == delimiter -> endCell()
            !quoted && (c == '\n' || c == '\r') -> {
                if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
                if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
            }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}

/** Whichever candidate separator appears most consistently across the first few lines. */
fun detectDelimiter(text: String): Char {
    val sample = text.lineSequence().take(5).toList()
    if (sample.isEmpty()) return ','
    return listOf(',', ';', '\t', '|')
        .maxByOrNull { d -> sample.sumOf { line -> line.count { it == d } } }
        ?: ','
}

// --- Amounts -------------------------------------------------------------------------------------

/**
 * A bank's idea of a number, as minor units.
 *
 * Handles: thousands separators in either convention, a decimal comma or point, a leading or
 * trailing minus, parentheses for negative, a currency symbol, and spaces. Returns null when it
 * cannot tell, which the caller must treat as "ask the user" rather than as zero.
 *
 * The decimal separator is decided by position, not by locale: whichever of `.` or `,` appears
 * last is the decimal one, because "1.234,56" and "1,234.56" are the same number written by two
 * different banks and only the order distinguishes them.
 */
fun parseAmountToMinor(input: String): Long? {
    var s = input.trim()
    if (s.isEmpty()) return null

    var negative = false
    if (s.startsWith('(') && s.endsWith(')')) { negative = true; s = s.substring(1, s.length - 1) }
    if (s.startsWith('-')) { negative = true; s = s.substring(1) }
    if (s.endsWith('-')) { negative = true; s = s.dropLast(1) }
    if (s.startsWith('+')) s = s.substring(1)

    s = s.filter { it.isDigit() || it == '.' || it == ',' }
    if (s.isEmpty() || s.none { it.isDigit() }) return null

    val lastDot = s.lastIndexOf('.')
    val lastComma = s.lastIndexOf(',')
    val decimalAt = maxOf(lastDot, lastComma)

    val whole: String
    val fraction: String
    if (decimalAt < 0) {
        whole = s
        fraction = "00"
    } else {
        val tail = s.substring(decimalAt + 1)
        // A group of exactly three digits after the last separator is a thousands group, not
        // cents: "1,234" is one thousand two hundred and thirty-four, not 1.234 of anything.
        if (tail.length == 3 && s.count { it == '.' || it == ',' } == 1) {
            whole = s.filter { it.isDigit() }
            fraction = "00"
        } else {
            whole = s.substring(0, decimalAt).filter { it.isDigit() }
            fraction = tail.filter { it.isDigit() }.padEnd(2, '0').take(2)
        }
    }

    val minor = (whole.ifEmpty { "0" }.toLongOrNull() ?: return null) * 100 +
        (fraction.toLongOrNull() ?: 0L)
    return if (negative) -minor else minor
}

// --- Dates ---------------------------------------------------------------------------------------

private val DATE_FORMATS = listOf(
    "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
    "yyyy/MM/dd", "d MMM yyyy", "dd MMM yyyy", "MMM d, yyyy", "dd/MM/yy", "MM/dd/yy",
).map { DateTimeFormatter.ofPattern(it) }

/**
 * Tries the formats banks actually use, in order.
 *
 * Ambiguity between day-first and month-first is real and unresolvable from one row, so the
 * import screen shows the parsed dates back to the user before anything is written.
 */
fun parseStatementDate(input: String): LocalDate? {
    val s = input.trim()
    if (s.isEmpty()) return null
    for (format in DATE_FORMATS) {
        try {
            return LocalDate.parse(s, format)
        } catch (_: DateTimeParseException) {
            // try the next shape
        }
    }
    return null
}

// --- Inference ---------------------------------------------------------------------------------

private val DATE_WORDS = listOf("date", "posted", "transaction date", "value date", "tanggal")
private val DESC_WORDS = listOf("description", "details", "narrative", "payee", "merchant", "memo", "reference", "keterangan")
private val AMOUNT_WORDS = listOf("amount", "value", "jumlah", "nominal")
private val DEBIT_WORDS = listOf("debit", "withdrawal", "paid out", "money out", "keluar")
private val CREDIT_WORDS = listOf("credit", "deposit", "paid in", "money in", "masuk")

/** Guesses the column meanings from the header, falling back to the shape of the data. */
fun inferColumns(header: List<String>, sample: List<List<String>>): ColumnMap {
    fun find(words: List<String>) = header.indexOfFirst { cell ->
        val h = cell.lowercase().trim()
        words.any { h == it || h.contains(it) }
    }

    var map = ColumnMap(
        date = find(DATE_WORDS),
        description = find(DESC_WORDS),
        amount = find(AMOUNT_WORDS),
        debit = find(DEBIT_WORDS),
        credit = find(CREDIT_WORDS),
    )

    // No usable header: fall back to what the first rows look like.
    if (map.date < 0) {
        map = map.copy(date = sample.firstOrNull()?.indexOfFirst { parseStatementDate(it) != null } ?: -1)
    }
    if (map.amount < 0 && map.debit < 0 && map.credit < 0) {
        val amountIndex = sample.firstOrNull()?.indexOfLast { parseAmountToMinor(it) != null } ?: -1
        map = map.copy(amount = amountIndex)
    }
    if (map.description < 0) {
        val taken = setOf(map.date, map.amount, map.debit, map.credit)
        map = map.copy(
            description = sample.firstOrNull()
                ?.indices
                ?.firstOrNull { it !in taken && sample.first()[it].any { c -> c.isLetter() } }
                ?: -1,
        )
    }
    return map
}

/** Applies a column map to the raw rows, dropping anything that cannot be read. */
fun readRows(rows: List<List<String>>, map: ColumnMap): List<StatementRow> = rows.mapNotNull { cells ->
    val date = cells.getOrNull(map.date)?.let(::parseStatementDate) ?: return@mapNotNull null
    val description = cells.getOrNull(map.description)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val amount = when {
        map.amount >= 0 -> cells.getOrNull(map.amount)?.let(::parseAmountToMinor)
        else -> {
            val out = cells.getOrNull(map.debit)?.let(::parseAmountToMinor)?.absoluteValue
            val inn = cells.getOrNull(map.credit)?.let(::parseAmountToMinor)?.absoluteValue
            when {
                out != null && out > 0L -> -out
                inn != null && inn > 0L -> inn
                else -> null
            }
        }
    } ?: return@mapNotNull null
    if (amount == 0L) return@mapNotNull null
    StatementRow(date, description, amount, cells)
}

/**
 * Matches each row against the ledger so the same purchase is not counted twice.
 *
 * Same amount and a date within three days is the test: a card transaction is often posted a day
 * or two after it happened, so an exact date match would miss most real duplicates. Matches are
 * flagged and excluded by default, and the user can override either way -- guessing wrong in the
 * direction of "skip it" is recoverable, and the opposite quietly inflates their spending.
 */
fun matchDuplicates(rows: List<StatementRow>, existing: List<Txn>): List<ImportCandidate> =
    rows.map { row ->
        val duplicate = existing.firstOrNull { txn ->
            txn.amountMinor == row.amountMinor &&
                (txn.date.toEpochDay() - row.date.toEpochDay()).absoluteValue <= 3
        }
        ImportCandidate(row, duplicate, include = duplicate == null)
    }
