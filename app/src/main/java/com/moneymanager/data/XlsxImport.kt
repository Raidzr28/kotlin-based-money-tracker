package com.moneymanager.data

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/*
 * Reading a bank's .xlsx export.
 *
 * Hand-written, for the same reason [parseCsv] is: an xlsx is a zip of XML, the three parts that
 * matter are a sheet, a string table and a style table, and pulling in Apache POI would add more
 * megabytes to the APK than this app otherwise weighs -- against a zero-cost, offline-first
 * constraint, for a file the user opens perhaps once a month.
 *
 * The output is deliberately the same shape [parseCsv] returns -- rows of trimmed strings -- so
 * everything downstream (column inference, amount parsing, duplicate matching) is shared and a
 * spreadsheet import is tested by the same rules a CSV import is.
 *
 * ponytail: DOM rather than a streaming parser, and the first worksheet rather than the one the
 * workbook lists first. Both are fine for a statement -- hundreds of rows, one sheet. A
 * multi-sheet workbook or a year of tick data would want SAX and a read of xl/workbook.xml.
 */

/** What the sheet said, row by row, with sparse cells filled in so columns stay aligned. */
fun readXlsx(input: InputStream): List<List<String>> {
    val parts = unzip(input)
    val sheet = parts.entries
        .filter { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }
        .minByOrNull { it.key }
        ?.value
        ?: return emptyList()

    val strings = parts["xl/sharedStrings.xml"]?.let(::sharedStrings) ?: emptyList()
    val dateStyles = parts["xl/styles.xml"]?.let(::dateStyleIndices) ?: emptySet()
    val epoch = parts["xl/workbook.xml"]?.let(::workbookEpoch) ?: EXCEL_EPOCH_1900

    val doc = parse(sheet) ?: return emptyList()
    val rows = mutableListOf<List<String>>()

    doc.getElementsByTagName("row").forEach { row ->
        val cells = mutableMapOf<Int, String>()
        (row as Element).getElementsByTagName("c").forEach { node ->
            val cell = node as Element
            val column = columnIndexOf(cell.getAttribute("r"))
            if (column >= 0) cells[column] = cellText(cell, strings, dateStyles, epoch)
        }
        // A row whose cells are all blank is a spacer, not a record. Banks put them between
        // a header block and the transactions.
        val width = (cells.keys.maxOrNull() ?: -1) + 1
        val line = (0 until width).map { cells[it].orEmpty() }
        if (line.any { it.isNotEmpty() }) rows.add(line)
    }

    // Every row padded to the widest, so a short row cannot shift a column under the header.
    val width = rows.maxOfOrNull { it.size } ?: 0
    return rows.map { if (it.size == width) it else it + List(width - it.size) { "" } }
}

// --- The three parts that matter -----------------------------------------------------------------

/**
 * The string table.
 *
 * Text in a sheet is usually not in the sheet: cells hold an index into this, so the same
 * merchant name written a hundred times is stored once. Rich text splits one string across
 * several runs, which is why every `t` under an `si` is joined rather than the first one taken.
 */
internal fun sharedStrings(xml: ByteArray): List<String> {
    val doc = parse(xml) ?: return emptyList()
    return doc.getElementsByTagName("si").map { si ->
        buildString {
            (si as Element).getElementsByTagName("t").forEach { append(it.textContent) }
        }
    }
}

/**
 * Which style indices mean "this number is a date".
 *
 * A date in a sheet is a plain number; only its format says otherwise. Without this, every date
 * column imports as 45678 and the statement lands in the ledger on the wrong day -- silently,
 * which is the failure this app tests hardest against.
 */
internal fun dateStyleIndices(xml: ByteArray): Set<Int> {
    val doc = parse(xml) ?: return emptySet()

    // Custom formats declare themselves; anything with a day, month or year token is a date.
    val customDateIds = doc.getElementsByTagName("numFmt").mapNotNull { node ->
        val e = node as Element
        val id = e.getAttribute("numFmtId").toIntOrNull() ?: return@mapNotNull null
        val code = e.getAttribute("formatCode")
        if (looksLikeDateFormat(code)) id else null
    }.toSet()

    val dateIds = BUILT_IN_DATE_FORMATS + customDateIds

    // cellXfs is the list cells index into with their `s` attribute.
    val cellXfs = doc.getElementsByTagName("cellXfs").firstOrNull() ?: return emptySet()
    return (cellXfs as Element).getElementsByTagName("xf").mapIndexedNotNull { index, node ->
        val id = (node as Element).getAttribute("numFmtId").toIntOrNull()
        if (id != null && id in dateIds) index else null
    }.toSet()
}

/** Day zero. Windows counts from 1899-12-30; a sheet written on a Mac may count from 1904. */
internal fun workbookEpoch(xml: ByteArray): LocalDate {
    val doc = parse(xml) ?: return EXCEL_EPOCH_1900
    val pr = doc.getElementsByTagName("workbookPr").firstOrNull() as? Element ?: return EXCEL_EPOCH_1900
    val is1904 = pr.getAttribute("date1904").let { it == "1" || it.equals("true", ignoreCase = true) }
    return if (is1904) EXCEL_EPOCH_1904 else EXCEL_EPOCH_1900
}

// --- One cell ------------------------------------------------------------------------------------

private fun cellText(
    cell: Element,
    strings: List<String>,
    dateStyles: Set<Int>,
    epoch: LocalDate,
): String {
    val type = cell.getAttribute("t")
    val raw = cell.getElementsByTagName("v").firstOrNull()?.textContent.orEmpty()

    return when {
        // Text written straight into the cell rather than the shared table.
        type == "inlineStr" ->
            cell.getElementsByTagName("t").joinToString("") { it.textContent }.trim()

        type == "s" -> strings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty().trim()

        // A formula's cached result is already in `v`; the formula itself is not our business.
        type == "str" -> raw.trim()

        type == "b" -> if (raw == "1") "TRUE" else "FALSE"

        raw.isEmpty() -> ""

        else -> {
            val styled = cell.getAttribute("s").toIntOrNull()
            if (styled != null && styled in dateStyles) {
                serialToDate(raw, epoch)?.toString() ?: raw
            } else {
                // Left exactly as written, so the amount parser sees the bank's own digits
                // rather than something a locale-aware formatter rewrote on the way past.
                raw.trim()
            }
        }
    }
}

/**
 * A serial number to a date, as ISO, which [parseStatementDate] already reads.
 *
 * Serials below 61 are not handled specially: they are January and February 1900, where the
 * format carries a fifty-year-old leap-year bug, and no bank statement is from then.
 */
internal fun serialToDate(raw: String, epoch: LocalDate): LocalDate? {
    val serial = raw.trim().toDoubleOrNull() ?: return null
    if (serial < 1 || serial > 100_000) return null
    return epoch.plusDays(serial.toLong())
}

/** "BC12" is column 54. Sparse sheets omit empty cells, so the letters are the only order. */
internal fun columnIndexOf(ref: String): Int {
    var column = 0
    var seen = false
    for (c in ref) {
        if (!c.isLetter()) break
        column = column * 26 + (c.uppercaseChar() - 'A' + 1)
        seen = true
    }
    return if (seen) column - 1 else -1
}

/** A format code with a day, month or year token in it, ignoring anything quoted or escaped. */
internal fun looksLikeDateFormat(code: String): Boolean {
    var quoted = false
    var i = 0
    while (i < code.length) {
        val c = code[i]
        when {
            c == '"' -> quoted = !quoted
            // Escapes the next character, and [Red] style sections are colours, not dates.
            c == '\\' -> i++
            c == '[' -> { while (i < code.length && code[i] != ']') i++ }
            !quoted && (c == 'y' || c == 'd' || c == 'h' || c == 's') -> return true
            // `m` is minutes after an hour token and months otherwise; either way it is a date
            // or a time, and both belong in a date column rather than an amount one.
            !quoted && c == 'm' -> return true
        }
        i++
    }
    return false
}

// --- Plumbing ------------------------------------------------------------------------------------

private val EXCEL_EPOCH_1900: LocalDate = LocalDate.of(1899, 12, 30)
private val EXCEL_EPOCH_1904: LocalDate = LocalDate.of(1904, 1, 1)

/** The formats Excel ships with that are dates or times. */
private val BUILT_IN_DATE_FORMATS = setOf(14, 15, 16, 17, 18, 19, 20, 21, 22, 45, 46, 47)

/** Only the handful of entries worth reading, so a workbook of images stays out of memory. */
private fun unzip(input: InputStream): Map<String, ByteArray> {
    val wanted = setOf("xl/sharedStrings.xml", "xl/styles.xml", "xl/workbook.xml")
    val out = mutableMapOf<String, ByteArray>()
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            val keep = name in wanted ||
                (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml"))
            if (keep) out[name] = zip.readBytes()
            zip.closeEntry()
        }
    }
    return out
}

private fun parse(xml: ByteArray): Element? = runCatching {
    DocumentBuilderFactory.newInstance()
        .apply {
            isNamespaceAware = false
            // A spreadsheet arriving from outside gets no say in what this process reads.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            isExpandEntityReferences = false
        }
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(xml))
        .documentElement
}.getOrNull()

// --- The DOM API predates iterables ---------------------------------------------------------------

private inline fun org.w3c.dom.NodeList.forEach(action: (Node) -> Unit) {
    for (i in 0 until length) action(item(i))
}

private inline fun <T> org.w3c.dom.NodeList.map(transform: (Node) -> T): List<T> =
    (0 until length).map { transform(item(it)) }

private inline fun <T> org.w3c.dom.NodeList.mapNotNull(transform: (Node) -> T?): List<T> =
    (0 until length).mapNotNull { transform(item(it)) }

private inline fun <T> org.w3c.dom.NodeList.mapIndexedNotNull(transform: (Int, Node) -> T?): List<T> =
    (0 until length).mapIndexedNotNull { i, _ -> transform(i, item(i)) }

private inline fun org.w3c.dom.NodeList.joinToString(separator: String, crossinline transform: (Node) -> String): String =
    (0 until length).joinToString(separator) { transform(item(it)) }

private fun org.w3c.dom.NodeList.firstOrNull(): Node? = if (length > 0) item(0) else null
