package com.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneymanager.data.LedgerState
import com.moneymanager.data.ledgerCsv
import com.moneymanager.data.money
import com.moneymanager.data.thisMonth
import com.moneymanager.ui.icon
import com.moneymanager.ui.CashFlowGrid
import com.moneymanager.ui.Chip
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.ColumnPair
import com.moneymanager.ui.LegendDot
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.RingChart
import com.moneymanager.ui.LocalConfirm
import com.moneymanager.ui.Routes
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.Slice
import com.moneymanager.ui.SoundingLine
import com.moneymanager.ui.Stat
import com.moneymanager.ui.categoryScale
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private enum class Period(val label: String) { Month("This month"), Quarter("Quarter"), Year("Year") }

/**
 * The ring is the screen, not an ornament on it. It is sized to the viewport rather than tucked
 * into a card corner, its labels sit on their own wedges, and the figure in the middle is the
 * total those wedges add up to -- so the chart answers "where did it go" without a legend hunt.
 */
@Composable
fun ReportsScreen(state: LedgerState, onGo: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val confirm = LocalConfirm.current
    // The system picker owns the destination. No storage permission is requested, nothing is
    // written until a folder is chosen, and the file lands somewhere the user already trusts.
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val rows = state.allTransactions
        val written = runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(ledgerCsv(rows).toByteArray())
            } ?: error("no stream")
        }
        confirm(
            if (written.isSuccess) "Exported ${rows.size} transactions"
            else "Could not write that file. Try another folder."
        )
    }
    val water = MoneyTheme.water
    var period by remember { mutableStateOf(Period.Month) }

    val scale = categoryScale()
    val spend = state.spendByCategory()
    val total = spend.sumOf { it.second }
    val slices = spend.mapIndexed { i, (category, amount) ->
        Slice(category.label, amount, scale[i % scale.size])
    }
    val monthName = thisMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())

    TabColumn {
        item {
            Text(
                "Reports",
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 14.dp),
            )
        }

        item {
            ChipRow {
                Period.entries.forEach { option ->
                    Chip(option.label, selected = period == option, onClick = { period = option })
                }
            }
        }

        item {
            Box(Modifier.padding(top = 18.dp)) {
                RingChart(
                    slices = slices,
                    centreValueMinor = total,
                    centreCaption = "out in $monthName",
                    modifier = Modifier.fillMaxWidth(),
                    strokeWidth = 30.dp,
                )
            }
        }

        item {
            Plate(Modifier.padding(top = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    spend.forEachIndexed { i, (category, amount) ->
                        val share = if (total == 0L) 0 else (amount * 1000 / total).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(scale[i % scale.size])
                            )
                            Text(
                                category.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                            )
                            Text(
                                "${(share / 10f).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 14.dp),
                            )
                            MoneyText(amount, style = MoneyType.small, color = scheme.onSurface)
                        }
                    }
                }
            }
        }

        item {
            SectionHeading(
                "In against out",
                caption = "Six months. Green came in, blue went out.",
            )
        }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        Stat("Came in", state.monthlyIn.last(), tone = water.income, style = MoneyType.medium)
                        Stat("Went out", state.monthlyOut.last(), style = MoneyType.medium)
                        Stat(
                            "Kept",
                            state.monthlyIn.last() - state.monthlyOut.last(),
                            tone = water.income,
                            style = MoneyType.medium,
                            showSign = true,
                        )
                    }
                    ColumnPair(state.monthlyIn, state.monthlyOut, state.monthLabels)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        LegendDot(water.income, "in")
                        LegendDot(scheme.primary, "out")
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Net worth",
                caption = "Owned above the line, owed below it, the difference traced across.",
            )
        }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        Stat("Net", state.netWorthMinor, style = MoneyType.large)
                        Stat(
                            "Change over 12 months",
                            (state.netWorthAssets.last() - state.netWorthDebts.last()) -
                                (state.netWorthAssets.first() - state.netWorthDebts.first()),
                            tone = water.income,
                            style = MoneyType.medium,
                            showSign = true,
                        )
                    }
                    SoundingLine(state.netWorthAssets, state.netWorthDebts, height = 172.dp)
                }
            }
        }

        item {
            SectionHeading(
                "Cash flow calendar",
                caption = "Darker days cost more. Pale days are days nothing was logged.",
            )
        }
        item {
            Plate {
                CashFlowGrid(
                    daily = state.dailySpend,
                    firstDayOfWeekOffset = thisMonth.atDay(1).dayOfWeek.value - 1,
                )
            }
        }

        item {
            SectionHeading(
                "Where it goes most often",
                caption = "By merchant, this month",
                actionLabel = "Ledger",
                onAction = { onGo(Routes.LEDGER) },
            )
        }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    state.topMerchants.forEach { merchant ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    merchant.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    "${merchant.count} visits · ${money(merchant.totalMinor / merchant.count)} each",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            MoneyText(merchant.totalMinor, style = MoneyType.row, color = scheme.onSurface)
                        }
                    }
                }
            }
        }

        item { SectionHeading("Biggest single expenses") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.transactions
                        .filter { it.amountMinor < 0 }
                        .sortedBy { it.amountMinor }
                        .take(5)
                        .forEach { txn ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    txn.merchant,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                MoneyText(
                                    txn.amountMinor.absoluteValue,
                                    style = MoneyType.row,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                }
            }
        }

        item { SectionHeading("Export") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(
                    "Export CSV",
                    Icons.Rounded.Download,
                    { exportCsv.launch("money-manager-${thisMonth.year}-%02d.csv".format(thisMonth.monthValue)) },
                    emphasis = true,
                    enabled = state.allTransactions.isNotEmpty(),
                )
            }
        }
        item {
            Text(
                if (state.allTransactions.isEmpty()) {
                    "There is nothing to export yet."
                } else {
                    "Written to the folder you choose, in the same column layout this app " +
                        "imports. Nothing is sent anywhere."
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
