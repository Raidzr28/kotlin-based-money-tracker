package com.moneymanager.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/*
 * Turning what a camera read off a receipt into a transaction worth confirming.
 *
 * Everything here is a guess and is presented to the user as one. The README is explicit that a
 * scan never posts silently, and that is not only a privacy nicety: OCR misreads a smudged 8 as a
 * 3 often enough that a receipt scanner which writes straight to the ledger is a receipt scanner
 * that quietly corrupts it.
 *
 * Pure text in, guesses out. No camera, no ML Kit, nothing Android -- which is what lets the
 * awkward part, the parsing, actually be tested.
 */

data class ReceiptGuess(
    val merchant: String?,
    val totalMinor: Long?,
    val date: LocalDate?,
    /** Every line read, so the user can see what the camera actually got. */
    val lines: List<String>,
) {
    val confident: Boolean get() = totalMinor != null && merchant != null
}

/** Words that appear beside the figure you actually paid. */
private val TOTAL_WORDS = listOf(
    "grand total", "total due", "amount due", "balance due", "total to pay", "to pay",
    "total", "jumlah", "total belanja", "tagihan", "betrag", "montant", "importe",
)

/** Words that sit beside a number that is *not* the total. */
private val NOT_TOTAL_WORDS = listOf(
    "subtotal", "sub total", "sub-total", "tax", "vat", "ppn", "service", "discount", "diskon",
    "change", "kembali", "kembalian", "tendered", "cash", "tunai", "card", "saving", "hemat",
    "points", "poin", "qty", "item",
)

/** Lines that are never a shop's name. */
private val NOT_MERCHANT = listOf(
    "receipt", "invoice", "tax invoice", "struk", "nota", "thank you", "terima kasih",
    "welcome", "customer copy", "merchant copy", "vat reg", "npwp",
)

private val DATE_PATTERNS = listOf(
    "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
    "yyyy/MM/dd", "dd/MM/yy", "MM/dd/yy", "dd-MM-yy", "d MMM yyyy", "dd MMM yyyy", "MMM d yyyy",
).map { DateTimeFormatter.ofPattern(it) }

private val DATE_SHAPE = Regex("""\d{1,4}[-/. ]\d{1,2}[-/. ]\d{2,4}""")

/**
 * Reads a receipt.
 *
 * The total is found by looking for the word, not by taking the biggest number: on a receipt with
 * several items the largest figure is often a single expensive line, and on one with a cash
 * payment the biggest number is what the customer handed over rather than what they spent.
 */
fun parseReceipt(text: String): ReceiptGuess {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    return ReceiptGuess(
        merchant = findMerchant(lines),
        totalMinor = findTotal(lines),
        date = findDate(lines),
        lines = lines,
    )
}

private fun findTotal(lines: List<String>): Long? {
    // Read upward: on almost every receipt the total sits below the items, and anything below the
    // total is change, loyalty points or a slogan.
    val labelled = lines.asReversed().firstNotNullOfOrNull { line ->
        val lower = line.lowercase()
        if (NOT_TOTAL_WORDS.any { lower.contains(it) }) return@firstNotNullOfOrNull null
        if (TOTAL_WORDS.none { lower.contains(it) }) return@firstNotNullOfOrNull null
        lastAmountOn(line)
    }
    if (labelled != null) return labelled

    // Nothing labelled: fall back to the largest plausible amount, ignoring lines that announce
    // themselves as something other than the total.
    return lines
        .filterNot { line -> NOT_TOTAL_WORDS.any { line.lowercase().contains(it) } }
        .mapNotNull { lastAmountOn(it) }
        .filter { it > 0L }
        .maxOrNull()
}

/** The rightmost money-shaped token on a line, which is where a receipt puts its figure. */
internal fun lastAmountOn(line: String): Long? {
    // No whitespace inside a token: a receipt aligns its columns with spaces, so allowing them
    // would read "2 x 6.25    12.50" as one number and invent a figure that is on no receipt.
    val tokens = Regex("""\d[\d.,]*\d|\d""").findAll(line).map { it.value }.toList()
    return tokens.asReversed().firstNotNullOfOrNull { token ->
        // A bare run of digits with no separator and more than six of them is a receipt number,
        // a barcode or a till id, not money.
        val digitsOnly = token.filter { it.isDigit() }
        if (!token.contains('.') && !token.contains(',') && digitsOnly.length > 6) null
        else parseAmountToMinor(token)?.takeIf { it != 0L }
    }
}

private fun findDate(lines: List<String>): LocalDate? {
    lines.forEach { line ->
        DATE_SHAPE.find(line)?.value?.let { candidate ->
            val normalised = candidate.replace('.', '/').replace('-', '/').replace(' ', '/')
            DATE_PATTERNS.forEach { format ->
                runCatching {
                    return LocalDate.parse(
                        normalised.replace('/', separatorFor(format)),
                        format,
                    )
                }
            }
            // Second pass on the raw text, for the written-month shapes.
            DATE_PATTERNS.forEach { format ->
                try {
                    return LocalDate.parse(candidate, format)
                } catch (_: DateTimeParseException) {
                    // next shape
                }
            }
        }
    }
    // A receipt with no readable date is almost always one from today.
    return null
}

private fun separatorFor(format: DateTimeFormatter): Char {
    val pattern = format.toString()
    return when {
        pattern.contains("'-'") || pattern.contains("-") -> '-'
        pattern.contains(".") -> '.'
        else -> '/'
    }
}

/**
 * The shop's name, which is nearly always the first real line.
 *
 * Skips the headings a till prints above it, anything that is mostly digits (a phone number, a
 * VAT id, an address), and the pleasantries.
 */
private fun findMerchant(lines: List<String>): String? = lines.take(6).firstOrNull { line ->
    val lower = line.lowercase()
    val letters = line.count { it.isLetter() }
    letters >= 3 &&
        letters >= line.count { it.isDigit() } &&
        NOT_MERCHANT.none { lower.contains(it) } &&
        TOTAL_WORDS.none { lower.contains(it) }
}?.trim()?.take(60)
