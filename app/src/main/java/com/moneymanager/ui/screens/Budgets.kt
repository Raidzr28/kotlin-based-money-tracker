package com.moneymanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymanager.data.LedgerState
import com.moneymanager.data.Budget
import com.moneymanager.data.Categories
import com.moneymanager.data.daysLeft
import com.moneymanager.data.money
import com.moneymanager.data.monthPace
import com.moneymanager.ui.icon
import com.moneymanager.ui.Chip
import com.moneymanager.ui.EmptyWater
import com.moneymanager.ui.Pill
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.ColumnPair
import com.moneymanager.ui.Flag
import com.moneymanager.ui.LedgerRow
import com.moneymanager.ui.Marker
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Plate
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.Stat
import com.moneymanager.ui.WaterBar
import com.moneymanager.ui.WaterColumn
import com.moneymanager.ui.categoryScale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/*
 * The README asks for four budgeting methods. They are not four screens: they are four ways of
 * cutting the same envelopes, so they live as one switch at the top of one screen. Switching
 * method never changes a number, only the framing -- which is the honest behaviour, because the
 * money did not move when the user changed their mind about bookkeeping philosophy.
 */

private enum class Method(val label: String, val blurb: String) {
    Envelopes("Envelopes", "A pot per category. When a pot is empty, it is empty."),
    Fifty("50 / 30 / 20", "Half to needs, a third to wants, a fifth to the future."),
    Zero("Zero-based", "Every dollar that came in this month gets a job."),
}

private val NEEDS = setOf("home", "groceries", "utilities", "health", "transport")
private val WANTS = setOf("food", "fun", "subs", "pets", "travel")

@Composable
fun BudgetsScreen(
    state: LedgerState,
    onOpenBudget: (String) -> Unit,
    onSetBudget: (categoryId: String, limitMinor: Long, rollsOver: Boolean) -> Unit,
    onClearBudget: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var method by remember { mutableStateOf(Method.Envelopes) }
    var editing by remember { mutableStateOf<String?>(null) }

    editing?.let { categoryId ->
        BudgetSheet(
            state = state,
            initialCategoryId = categoryId,
            onDismiss = { editing = null },
            onSave = { id, limit, rolls -> onSetBudget(id, limit, rolls); editing = null },
            onClear = { id -> onClearBudget(id); editing = null },
        )
    }

    TabColumn {
        item {
            Text(
                "Budgets",
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 14.dp),
            )
        }

        item {
            ChipRow {
                Method.entries.forEach { option ->
                    Chip(option.label, selected = method == option, onClick = { method = option })
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Pill(
                    if (state.budgets.isEmpty()) "Set your first budget" else "Set a budget",
                    Icons.Rounded.Tune,
                    { editing = state.categories.firstOrNull()?.id.orEmpty() },
                    emphasis = state.budgets.isEmpty(),
                )
            }
        }

        item {
            Text(
                method.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item {
            Box(Modifier.padding(top = 16.dp)) {
                WaterColumn(
                    level = state.safeFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                    swell = false,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Stat("Budgeted", state.budgetedMinor, style = MoneyType.medium)
                        Stat("Spent", state.spentMinor, style = MoneyType.medium)
                        Stat(
                            "Left",
                            state.budgetedMinor - state.spentMinor,
                            style = MoneyType.medium,
                            tone = scheme.onSurface,
                        )
                    }
                }
            }
        }

        when (method) {
            Method.Envelopes -> envelopes(state, onOpenBudget) { editing = it }
            Method.Fifty -> fiftyThirtyTwenty(state)
            Method.Zero -> zeroBased(state)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.envelopes(
    state: LedgerState,
    onOpenBudget: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    item {
        SectionHeading(
            "Envelopes",
            caption = "$daysLeft days left · the tick on each bar is today's pace",
        )
    }
    if (state.budgets.isEmpty()) {
        item {
            EmptyWater(
                "No envelopes yet",
                "Give a category a monthly limit and it starts showing up here, on Home, and in " +
                    "every report.",
                actionLabel = "Set a budget",
                actionIcon = Icons.Rounded.Tune,
                onAction = { onEdit(state.categories.firstOrNull()?.id.orEmpty()) },
            )
        }
    }
    items(state.budgets.size) { index ->
        val budget = state.budgets[index]
        EnvelopePlate(budget, index, onClick = { onEdit(budget.categoryId) })
    }
}

/**
 * Pick a category, give it a limit. The whole of budgeting in this app starts here, which is why
 * it is a sheet and not a pushed screen: the user already decided before they tapped.
 */
@Composable
private fun BudgetSheet(
    state: LedgerState,
    initialCategoryId: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, Boolean) -> Unit,
    onClear: (String) -> Unit,
) {
    var categoryId by remember { mutableStateOf(initialCategoryId) }
    val existing = state.budgets.firstOrNull { it.categoryId == categoryId }
    var digits by remember(categoryId) { mutableStateOf(minorToDigits(existing?.limitMinor ?: 0L)) }
    var rolls by remember(categoryId) { mutableStateOf(existing?.rollsOver ?: false) }
    val limit = digitsToMinor(digits)

    MoneySheet(
        title = if (existing == null) "Set a budget" else "Budget for ${Categories[categoryId].label}",
        onDismiss = onDismiss,
    ) {
        ChipRow {
            state.categories.forEach { category ->
                Chip(
                    category.label,
                    selected = categoryId == category.id,
                    leading = category.icon,
                    onClick = { categoryId = category.id },
                )
            }
        }

        AmountInput(digits, { digits = it }, label = "Monthly limit")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    "Roll unspent over",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Anything left at the end of the month is added to next month's limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = rolls, onCheckedChange = { rolls = it })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(
                if (limit == 0L) "Enter a limit" else "Save ${money(limit)}",
                Icons.Rounded.Check,
                { if (limit > 0L) onSave(categoryId, limit, rolls) },
                emphasis = limit > 0L,
                modifier = Modifier.weight(1f),
            )
            if (existing != null) {
                Pill(
                    "Remove", Icons.Rounded.Bolt, { onClear(categoryId) },
                    contentColor = MoneyTheme.water.alert,
                )
            }
        }
    }
}

@Composable
private fun EnvelopePlate(budget: Budget, index: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val category = Categories[budget.categoryId]
    val scale = categoryScale()

    Plate(
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Marker(category.icon, tint = scale[index % scale.size], size = 38.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(category.label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                    Text(
                        "${money(budget.spentMinor)} of ${money(budget.limitMinor)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        budget.remainingMinor.absoluteValue,
                        style = MoneyType.row,
                        color = if (budget.over) water.alert else scheme.onSurface,
                    )
                    Text(
                        if (budget.over) "over" else "left",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (budget.over) water.alert else scheme.onSurfaceVariant,
                    )
                }
            }
            WaterBar(
                fraction = budget.fraction,
                over = budget.over,
                paceAt = monthPace,
                modifier = Modifier.fillMaxWidth(),
                height = 12.dp,
            )
            if (budget.rollsOver || budget.over) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (budget.rollsOver) {
                        Flag(Icons.Rounded.Repeat, "Rolls over", scheme.onSurfaceVariant)
                    }
                    if (budget.over) {
                        Flag(
                            Icons.Rounded.Bolt,
                            "Over by ${money(budget.remainingMinor.absoluteValue)}",
                            water.alert,
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.fiftyThirtyTwenty(state: LedgerState) {
    item { SectionHeading("Where it is meant to go") }
    item {
        val needs = state.budgets.filter { it.categoryId in NEEDS }
        val wants = state.budgets.filter { it.categoryId in WANTS }
        val income = state.monthlyIn.last()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Band("Needs", 50, needs.sumOf { it.limitMinor }, needs.sumOf { it.spentMinor }, income)
            Band("Wants", 30, wants.sumOf { it.limitMinor }, wants.sumOf { it.spentMinor }, income)
            Band("Future", 20, income * 20 / 100, 200_00L, income)
        }
    }
    item {
        Plate(Modifier.padding(top = 10.dp), depth = 1) {
            Text(
                "The rule is measured against money in, not against what you budgeted. This month " +
                    "${money(state.monthlyIn.last())} came in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Band(name: String, targetPercent: Int, plannedMinor: Long, spentMinor: Long, incomeMinor: Long) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val actual = if (incomeMinor == 0L) 0 else (plannedMinor * 100 / incomeMinor).toInt()
    val off = actual - targetPercent

    Plate {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
                Text(
                    "$actual% of $targetPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (off > 5) water.alert else scheme.onSurfaceVariant,
                )
            }
            WaterBar(
                fraction = if (plannedMinor == 0L) 0f else spentMinor.toFloat() / plannedMinor,
                over = spentMinor > plannedMinor,
                paceAt = monthPace,
                modifier = Modifier.fillMaxWidth(),
                height = 12.dp,
            )
            Text(
                "${money(spentMinor)} spent of ${money(plannedMinor)} planned",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.zeroBased(state: LedgerState) {
    item { SectionHeading("Give every dollar a job") }
    item {
        val income = state.monthlyIn.last()
        val assigned = state.budgetedMinor
        val unassigned = income - assigned
        Plate {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("Came in", income, style = MoneyType.medium)
                    Stat("Assigned", assigned, style = MoneyType.medium)
                }
                WaterBar(
                    fraction = assigned.toFloat() / income,
                    modifier = Modifier.fillMaxWidth(),
                    height = 14.dp,
                )
                Text(
                    if (unassigned > 0) {
                        "${money(unassigned)} is still unassigned. Zero-based budgeting is not " +
                            "finished until this reads zero."
                    } else {
                        "Every dollar has a job."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Flag(
                    if (unassigned > 0) Icons.Rounded.Tune else Icons.Rounded.Check,
                    if (unassigned > 0) "Assign ${money(unassigned)}" else "Balanced",
                    if (unassigned > 0) MaterialTheme.colorScheme.primary else MoneyTheme.water.income,
                )
            }
        }
    }
    item { SectionHeading("Unassigned money could go to") }
    item {
        Plate {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.goals.forEach { goal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Marker(Icons.Rounded.Savings, tint = MoneyTheme.water.goal, size = 34.dp)
                        Text(
                            goal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        )
                        MoneyText(
                            goal.targetMinor - goal.savedMinor,
                            style = MoneyType.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------- Budget detail */

@Composable
fun BudgetDetailScreen(
    state: LedgerState,
    categoryId: String,
    onBack: () -> Unit,
    onOpenTxn: (String) -> Unit,
    onSetBudget: (categoryId: String, limitMinor: Long, rollsOver: Boolean) -> Unit,
    onClearBudget: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val category = Categories[categoryId]
    val budget = state.budgets.firstOrNull { it.categoryId == categoryId }
    var editing by remember { mutableStateOf(false) }

    if (editing) {
        BudgetSheet(
            state = state,
            initialCategoryId = categoryId,
            onDismiss = { editing = false },
            onSave = { id, limit, rolls -> onSetBudget(id, limit, rolls); editing = false },
            onClear = { id -> onClearBudget(id); editing = false },
        )
    }

    val rows = state.transactions.filter {
        it.categoryId == categoryId || it.splits.any { s -> s.categoryId == categoryId }
    }

    DetailScaffold(title = category.label, onBack = onBack) {
        if (budget == null) {
            item {
                EmptyWater(
                    "No budget on ${category.label} yet",
                    "Set one and this category joins the waterline on Home.",
                    actionLabel = "Set a budget",
                    actionIcon = Icons.Rounded.Tune,
                    onAction = { editing = true },
                )
            }
            return@DetailScaffold
        }

        item {
            Box(Modifier.padding(top = 4.dp)) {
                WaterColumn(
                    level = (1f - budget.fraction).coerceIn(0f, 1f),
                    over = budget.over,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(212.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            if (budget.over) "Over budget" else "Left this month",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                        )
                        MoneyText(
                            budget.remainingMinor.absoluteValue,
                            style = MoneyType.hero,
                            color = if (budget.over) water.alert else scheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            if (budget.over) {
                                "${money(budget.spentMinor)} spent against a ${money(budget.limitMinor)} limit"
                            } else {
                                "${money(budget.remainingMinor / daysLeft.coerceAtLeast(1))} a day " +
                                    "for the $daysLeft days left"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item { SectionHeading("Six months of ${category.label.lowercase()}") }
        item {
            Plate {
                ColumnPair(
                    incoming = List(6) { budget.limitMinor },
                    outgoing = listOf(
                        (budget.limitMinor * 0.82).roundToInt().toLong(),
                        (budget.limitMinor * 0.95).roundToInt().toLong(),
                        (budget.limitMinor * 0.71).roundToInt().toLong(),
                        (budget.limitMinor * 1.08).roundToInt().toLong(),
                        (budget.limitMinor * 0.88).roundToInt().toLong(),
                        budget.spentMinor,
                    ),
                    labels = state.monthLabels,
                )
            }
        }

        item { SectionHeading("Settings") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Fact("Limit", money(budget.limitMinor))
                    Fact("Method", "Monthly, resets on the 1st")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Roll unspent over", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                            Text(
                                "Anything left on the 30th is added to next month's limit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = budget.rollsOver,
                            onCheckedChange = { onSetBudget(categoryId, budget.limitMinor, it) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Transactions",
                caption = "${rows.size} in ${category.label.lowercase()} this month",
            )
        }
        item {
            Plate {
                Column {
                    rows.forEach { txn ->
                        LedgerRow(txn, onClick = { onOpenTxn(txn.id) }, showDate = true)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
