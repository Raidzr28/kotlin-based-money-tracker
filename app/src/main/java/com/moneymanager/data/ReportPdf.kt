package com.moneymanager.data

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/*
 * The month as a PDF, for the reader who wants a page rather than a spreadsheet.
 *
 * Android has had a PDF writer since API 19, so this needs nothing added to the build: a page,
 * a Canvas and the same figures the Reports screen is already showing. A PDF library would be
 * several megabytes to draw twenty lines of text.
 *
 * Deliberately plain. This is a document somebody prints, files, or sends to an accountant; the
 * app's own depth palette is a screen idea and would come out as grey boxes on paper. What
 * carries over is the rule that matters: every figure is the one the ledger already derived,
 * and nothing is rounded on its way onto the page.
 */

/** A4 at 72dpi, which is what PdfDocument's page units are. */
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 48f
private const val LINE = 18f

/**
 * Writes the month's report to [out].
 *
 * @param title what the period is called, e.g. "September 2026".
 * @param spend category totals, largest first, in [currency].
 * @param merchants the biggest merchants, largest first.
 */
fun writeReportPdf(
    out: OutputStream,
    title: String,
    currency: String,
    inMinor: Long,
    outMinor: Long,
    netWorthMinor: Long,
    spend: List<Pair<String, Long>>,
    merchants: List<MerchantTotal>,
    transactionCount: Int,
    incomplete: List<String> = emptyList(),
) {
    val doc = PdfDocument()
    val heading = paint(15f, bold = true)
    val subheading = paint(11f, bold = true)
    val body = paint(10f)
    val quiet = paint(9f).apply { color = 0xFF6B7280.toInt() }
    val figure = paint(10f).apply { textAlign = Paint.Align.RIGHT }

    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
    var canvas = page.canvas
    var y = MARGIN + 12f
    var pageNumber = 1
    val right = PAGE_WIDTH - MARGIN

    /** Starts a new page when the next line would run off this one. */
    fun room(lines: Int = 1) {
        if (y + lines * LINE <= PAGE_HEIGHT - MARGIN) return
        doc.finishPage(page)
        pageNumber++
        page = doc.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        canvas = page.canvas
        y = MARGIN
    }

    fun line(text: String, p: Paint = body, gap: Float = LINE) {
        room()
        canvas.drawText(text, MARGIN, y, p)
        y += gap
    }

    fun row(label: String, amountMinor: Long) {
        room()
        canvas.drawText(label, MARGIN, y, body)
        canvas.drawText(money(amountMinor, currency), right, y, figure)
        y += LINE
    }

    line(title, heading, gap = LINE * 1.4f)
    line("Money Manager", quiet, gap = LINE * 1.6f)

    line("Summary", subheading)
    row("Money in", inMinor)
    row("Money out", outMinor)
    row("Net worth", netWorthMinor)
    line("$transactionCount transactions", quiet, gap = LINE * 1.8f)

    if (incomplete.isNotEmpty()) {
        // The same refusal the screens make: a total that could not be completed says so rather
        // than presenting a smaller number as if it were the whole picture.
        line("Totals are short", subheading)
        line("No exchange rate for ${incomplete.joinToString(", ")}, so nothing in", quiet, gap = LINE * 0.8f)
        line("those currencies is counted above.", quiet, gap = LINE * 1.8f)
    }

    if (spend.isNotEmpty()) {
        line("Where it went", subheading)
        spend.forEach { (label, amount) -> row(label, amount) }
        y += LINE * 0.8f
    }

    if (merchants.isNotEmpty()) {
        line("Biggest merchants", subheading)
        merchants.forEach { row("${it.name} (${it.count})", it.totalMinor) }
    }

    doc.finishPage(page)
    doc.writeTo(out)
    doc.close()
}

private fun paint(size: Float, bold: Boolean = false) = Paint().apply {
    isAntiAlias = true
    textSize = size
    color = 0xFF111827.toInt()
    typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
}

/** "September 2026", for the page's own title. */
fun monthTitle(month: YearMonth): String =
    "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
