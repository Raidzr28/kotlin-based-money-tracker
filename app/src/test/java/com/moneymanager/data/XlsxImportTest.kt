package com.moneymanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/*
 * Reading a bank's spreadsheet export.
 *
 * The workbooks here are built in memory rather than checked in as fixtures, so what each test
 * is asserting is visible in the test itself -- an .xlsx in the repository is an opaque zip.
 *
 * The case that matters most is the date. A date in a sheet is a plain number and only its
 * format says otherwise; get that wrong and a statement imports onto the wrong day without
 * anything looking broken, which is the exact failure this app is built to refuse.
 */
class XlsxImportTest {

    // --- Building a workbook -------------------------------------------------------------------

    private fun xlsx(
        sheet: String,
        sharedStrings: String? = null,
        styles: String? = null,
        workbook: String? = null,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, body: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
            put("xl/worksheets/sheet1.xml", sheet)
            sharedStrings?.let { put("xl/sharedStrings.xml", it) }
            styles?.let { put("xl/styles.xml", it) }
            workbook?.let { put("xl/workbook.xml", it) }
        }
        return out.toByteArray()
    }

    private fun read(bytes: ByteArray) = readXlsx(ByteArrayInputStream(bytes))

    private fun sheet(rows: String) =
        """<worksheet><sheetData>$rows</sheetData></worksheet>"""

    /** cellXfs index 1 uses numFmtId 14, a built-in date format. Index 0 is a plain number. */
    private val dateStyles = """
        <styleSheet><cellXfs>
          <xf numFmtId="0"/>
          <xf numFmtId="14"/>
        </cellXfs></styleSheet>
    """.trimIndent()

    // --- Cells ---------------------------------------------------------------------------------

    @Test
    fun `numbers come through exactly as the bank wrote them`() {
        val rows = read(
            xlsx(sheet("""<row r="1"><c r="A1"><v>-1284.12</v></c></row>"""))
        )
        // Not reformatted on the way past: parseAmountToMinor is the only thing that gets to
        // decide what a number means.
        assertEquals(listOf(listOf("-1284.12")), rows)
    }

    @Test
    fun `text comes out of the shared string table`() {
        val rows = read(
            xlsx(
                sheet("""<row r="1"><c r="A1" t="s"><v>1</v></c></row>"""),
                sharedStrings = """<sst><si><t>Date</t></si><si><t>Tesco</t></si></sst>""",
            )
        )
        assertEquals(listOf(listOf("Tesco")), rows)
    }

    @Test
    fun `a rich text run is joined rather than truncated to its first piece`() {
        val rows = read(
            xlsx(
                sheet("""<row r="1"><c r="A1" t="s"><v>0</v></c></row>"""),
                sharedStrings = """<sst><si><r><t>Card </t></r><r><t>payment</t></r></si></sst>""",
            )
        )
        assertEquals(listOf(listOf("Card payment")), rows)
    }

    @Test
    fun `inline text is read too`() {
        val rows = read(
            xlsx(sheet("""<row r="1"><c r="A1" t="inlineStr"><is><t>Rent</t></is></c></row>"""))
        )
        assertEquals(listOf(listOf("Rent")), rows)
    }

    // --- Dates ---------------------------------------------------------------------------------

    /*
     * The anchor for every serial below: 2020-01-01 is 43831, and counting whole years forward
     * (2020 and 2024 leap) reaches 45658 for 2025-01-01. The 1904 system runs 1462 days behind
     * the 1900 one, so the same day there is 44196.
     */


    @Test
    fun `a date-formatted number reads as a date`() {
        val rows = read(
            xlsx(
                sheet("""<row r="1"><c r="A1" s="1"><v>45658</v></c></row>"""),
                styles = dateStyles,
            )
        )
        assertEquals(listOf(listOf(LocalDate.of(2025, 1, 1).toString())), rows)
    }

    @Test
    fun `the same number without a date format stays a number`() {
        // s="0" is the plain-number style. This is what separates an amount of 45658 from a date.
        val rows = read(
            xlsx(
                sheet("""<row r="1"><c r="A1" s="0"><v>45658</v></c></row>"""),
                styles = dateStyles,
            )
        )
        assertEquals(listOf(listOf("45658")), rows)
    }

    @Test
    fun `a custom format that spells out a date counts as one`() {
        val styles = """
            <styleSheet>
              <numFmts><numFmt numFmtId="164" formatCode="dd/mm/yyyy"/></numFmts>
              <cellXfs><xf numFmtId="164"/></cellXfs>
            </styleSheet>
        """.trimIndent()
        val rows = read(
            xlsx(sheet("""<row r="1"><c r="A1" s="0"><v>45658</v></c></row>"""), styles = styles)
        )
        assertEquals(listOf(listOf(LocalDate.of(2025, 1, 1).toString())), rows)
    }

    @Test
    fun `a currency format is not mistaken for a date`() {
        // "#,##0.00" has no date token in it; the 'm' rule must not fire on the digits.
        val styles = """
            <styleSheet>
              <numFmts><numFmt numFmtId="164" formatCode="#,##0.00"/></numFmts>
              <cellXfs><xf numFmtId="164"/></cellXfs>
            </styleSheet>
        """.trimIndent()
        val rows = read(
            xlsx(sheet("""<row r="1"><c r="A1" s="0"><v>1284.12</v></c></row>"""), styles = styles)
        )
        assertEquals(listOf(listOf("1284.12")), rows)
    }

    @Test
    fun `a quoted month token in a format is text, not a date`() {
        assertTrue(looksLikeDateFormat("dd/mm/yyyy"))
        assertTrue(looksLikeDateFormat("d mmm yy"))
        // The only tokens here are inside quotes or a colour section.
        assertEquals(false, looksLikeDateFormat("""[Red]#,##0.00" items";"none""""))
        assertEquals(false, looksLikeDateFormat("0.00"))
    }

    @Test
    fun `a Mac workbook counts its days from 1904`() {
        val rows = read(
            xlsx(
                sheet("""<row r="1"><c r="A1" s="1"><v>44196</v></c></row>"""),
                styles = dateStyles,
                workbook = """<workbook><workbookPr date1904="1"/></workbook>""",
            )
        )
        assertEquals(listOf(listOf(LocalDate.of(2025, 1, 1).toString())), rows)
    }

    // --- Rows ----------------------------------------------------------------------------------

    @Test
    fun `a skipped cell leaves a gap rather than shifting the column`() {
        // Sheets omit empty cells entirely. Reading B and D as columns 1 and 2 would file every
        // amount under the wrong heading.
        val rows = read(
            xlsx(
                sheet(
                    """<row r="1">
                         <c r="A1"><v>1</v></c>
                         <c r="C1"><v>3</v></c>
                       </row>"""
                )
            )
        )
        assertEquals(listOf(listOf("1", "", "3")), rows)
    }

    @Test
    fun `short rows are padded so every column keeps its heading`() {
        val rows = read(
            xlsx(
                sheet(
                    """<row r="1"><c r="A1"><v>1</v></c><c r="B1"><v>2</v></c></row>
                       <row r="2"><c r="A2"><v>3</v></c></row>"""
                )
            )
        )
        assertEquals(listOf(listOf("1", "2"), listOf("3", "")), rows)
    }

    @Test
    fun `a blank spacer row is dropped`() {
        val rows = read(
            xlsx(
                sheet(
                    """<row r="1"><c r="A1" t="inlineStr"><is><t>Date</t></is></c></row>
                       <row r="2"><c r="A2"/></row>
                       <row r="3"><c r="A3" t="inlineStr"><is><t>Tesco</t></is></c></row>"""
                )
            )
        )
        assertEquals(listOf(listOf("Date"), listOf("Tesco")), rows)
    }

    @Test
    fun `column letters past Z keep counting`() {
        assertEquals(0, columnIndexOf("A1"))
        assertEquals(25, columnIndexOf("Z9"))
        assertEquals(26, columnIndexOf("AA1"))
        assertEquals(53, columnIndexOf("BB12"))
        assertEquals(-1, columnIndexOf("12"))
    }

    // --- Refusing rather than inventing ---------------------------------------------------------

    @Test
    fun `a file that is not a workbook gives nothing rather than throwing`() {
        assertEquals(emptyList<List<String>>(), read("not a zip".toByteArray()))
    }

    @Test
    fun `a workbook with no sheet gives nothing`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write("""<sst><si><t>x</t></si></sst>""".toByteArray())
            zip.closeEntry()
        }
        assertEquals(emptyList<List<String>>(), read(out.toByteArray()))
    }

    // --- The whole way through ------------------------------------------------------------------

    @Test
    fun `a statement reads end to end through the same pipeline a CSV uses`() {
        val bytes = xlsx(
            sheet(
                """<row r="1">
                     <c r="A1" t="s"><v>0</v></c>
                     <c r="B1" t="s"><v>1</v></c>
                     <c r="C1" t="s"><v>2</v></c>
                   </row>
                   <row r="2">
                     <c r="A2" s="1"><v>45658</v></c>
                     <c r="B2" t="s"><v>3</v></c>
                     <c r="C2" s="0"><v>-12.50</v></c>
                   </row>"""
            ),
            sharedStrings = """<sst>
                <si><t>Date</t></si><si><t>Description</t></si>
                <si><t>Amount</t></si><si><t>Tesco</t></si></sst>""",
            styles = dateStyles,
        )

        val rows = read(bytes)
        val header = rows.first()
        val map = inferColumns(header, rows.drop(1))
        val parsed = readRows(rows.drop(1), map)

        assertEquals(1, parsed.size)
        assertEquals("Tesco", parsed.single().description)
        assertEquals(-12_50L, parsed.single().amountMinor)
        assertEquals(LocalDate.of(2025, 1, 1), parsed.single().date)
    }
}
