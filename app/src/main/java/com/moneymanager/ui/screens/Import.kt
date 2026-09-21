package com.moneymanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanager.data.ColumnMap
import com.moneymanager.data.ImportCandidate
import com.moneymanager.data.LedgerState
import com.moneymanager.data.dayLabel
import com.moneymanager.data.inferColumns
import com.moneymanager.data.matchDuplicates
import com.moneymanager.data.money
import com.moneymanager.data.parseCsv
import com.moneymanager.data.readXlsx
import com.moneymanager.data.readRows
import com.moneymanager.ui.Chip
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.EmptyWater
import com.moneymanager.ui.Flag
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.ScaffoldNote
import com.moneymanager.ui.SectionHeading

/**
 * Importing a bank statement.
 *
 * Four steps, and the middle two exist because no two banks export the same shape: pick a file,
 * confirm what the columns mean, look at what was read, then commit. Nothing is written to the
 * ledger until the last step, and anything that looks like something already there is excluded
 * before the user even sees it.
 */
@Composable
fun ImportScreen(
    state: LedgerState,
    onBack: () -> Unit,
    onImport: (List<ImportCandidate>, String, String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val context = LocalContext.current

    var header by remember { mutableStateOf<List<String>>(emptyList()) }
    var rows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var map by remember { mutableStateOf(ColumnMap()) }
    var candidates by remember { mutableStateOf<List<ImportCandidate>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var accountId by remember { mutableStateOf(state.accounts.firstOrNull()?.id.orEmpty()) }
    var categoryId by remember { mutableStateOf("home") }

    fun reread(newMap: ColumnMap) {
        map = newMap
        candidates = matchDuplicates(readRows(rows, newMap), state.allTransactions)
    }

    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        problem = null

        // A spreadsheet is a zip and a CSV is text, so the two are read differently -- but both
        // come out as rows of strings and everything after this point is shared. The name is the
        // only hint available: a content URI carries no extension of its own.
        val name = displayName(context, uri).orEmpty()
        val spreadsheet = name.endsWith(".xlsx", ignoreCase = true)
        val pdf = name.endsWith(".pdf", ignoreCase = true)

        if (pdf) {
            problem = "PDF statements cannot be read yet. Export CSV or XLSX from your bank " +
                "instead -- both carry the same rows, and neither needs the layout guessed at."
            return@rememberLauncherForActivityResult
        }

        runCatching {
            if (spreadsheet) {
                context.contentResolver.openInputStream(uri)?.use { readXlsx(it) }
            } else {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { parseCsv(it.readText()) }
            }
        }.onFailure {
            problem = "That file could not be opened."
        }.onSuccess { parsed ->
            if (parsed.isNullOrEmpty()) {
                problem = if (spreadsheet) {
                    "That workbook has no readable sheet in it."
                } else {
                    "That file is empty."
                }
                return@onSuccess
            }
            if (parsed.size < 2) {
                problem = "That file has no rows under its header."
                return@onSuccess
            }
            header = parsed.first()
            rows = parsed.drop(1)
            reread(inferColumns(header, rows))
        }
    }

    val included = candidates.filter { it.include }
    val duplicates = candidates.count { it.duplicateOf != null }

    DetailScaffold(title = "Import a statement", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp), depth = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Export CSV or XLSX from your bank and open it here. The file is read " +
                            "on this device and nothing is uploaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Pill(
                        if (rows.isEmpty()) "Choose a file" else "Choose a different file",
                        Icons.Rounded.Description,
                        {
                            pick.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/plain",
                                    XLSX_MIME,
                                    "*/*",
                                )
                            )
                        },
                        emphasis = rows.isEmpty(),
                    )
                }
            }
        }

        problem?.let {
            item {
                Plate(Modifier.padding(top = 14.dp)) {
                    Flag(Icons.Rounded.Description, it, water.alert)
                }
            }
        }

        if (rows.isNotEmpty()) {
            item {
                SectionHeading(
                    "What the columns mean",
                    caption = "Guessed from the header. Correct anything wrong before importing.",
                )
            }
            item {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ColumnPicker("Date", header, map.date) { reread(map.copy(date = it)) }
                        ColumnPicker("Description", header, map.description) { reread(map.copy(description = it)) }
                        ColumnPicker("Amount", header, map.amount) {
                            reread(map.copy(amount = it, debit = -1, credit = -1))
                        }
                        if (map.amount < 0) {
                            ColumnPicker("Money out", header, map.debit) { reread(map.copy(debit = it)) }
                            ColumnPicker("Money in", header, map.credit) { reread(map.copy(credit = it)) }
                        }
                    }
                }
            }

            item { SectionHeading("Into which account") }
            item {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ChipRow {
                            state.accounts.forEach { account ->
                                Chip(
                                    account.name,
                                    selected = accountId == account.id,
                                    onClick = { accountId = account.id },
                                )
                            }
                        }
                        Text(
                            "Starting category, applied to every imported row. You can change them " +
                                "afterwards, and merchants you have categorised before keep theirs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        ChipRow {
                            state.categories.forEach { category ->
                                Chip(
                                    category.label,
                                    selected = categoryId == category.id,
                                    onClick = { categoryId = category.id },
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeading(
                    "What was found",
                    caption = buildString {
                        append("${candidates.size} rows read")
                        if (duplicates > 0) append(" · $duplicates already in your ledger")
                    },
                )
            }

            if (candidates.isEmpty()) {
                item {
                    EmptyWater(
                        "Nothing readable",
                        "No row had both a date and an amount that could be read. Check the column " +
                            "mapping above, or try a different export format from your bank.",
                    )
                }
            }

            items(candidates.size) { index ->
                val candidate = candidates[index]
                CandidateRow(candidate) { include ->
                    candidates = candidates.toMutableList().also {
                        it[index] = candidate.copy(include = include)
                    }
                }
            }

            item {
                Column(
                    Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Pill(
                        if (included.isEmpty()) "Nothing selected" else "Import ${included.size} transactions",
                        Icons.Rounded.Check,
                        { if (included.isNotEmpty()) onImport(included, accountId, categoryId) },
                        emphasis = included.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ScaffoldNote(
                        "Rows that match something already in your ledger are unticked for you. " +
                            "A card payment often posts a day or two after it happened, so a match " +
                            "is anything within three days for the same amount."
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** Which column in the file is this? */
@Composable
private fun ColumnPicker(
    label: String,
    header: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipRow {
            Chip("None", selected = selected < 0, onClick = { onSelect(-1) })
            header.forEachIndexed { index, name ->
                Chip(
                    name.ifBlank { "Column ${index + 1}" },
                    selected = selected == index,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: ImportCandidate, onInclude: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val row = candidate.row

    Plate(Modifier.padding(bottom = 8.dp), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = candidate.include, onCheckedChange = onInclude)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
            ) {
                Text(
                    row.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (candidate.duplicateOf != null) {
                        "${dayLabel(row.date)} · already logged as ${candidate.duplicateOf.merchant}"
                    } else {
                        dayLabel(row.date)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (candidate.duplicateOf != null) water.alert else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MoneyText(
                row.amountMinor,
                style = MoneyType.row,
                color = if (row.amountMinor > 0) water.income else scheme.onSurface,
                showSign = row.amountMinor > 0,
            )
        }
    }
}

/** What the picker called the file. The only way to tell a workbook from a CSV before opening it. */
private fun displayName(context: android.content.Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    }.getOrNull()

private const val XLSX_MIME =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
