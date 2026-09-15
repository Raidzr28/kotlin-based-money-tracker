package com.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moneymanager.R
import com.moneymanager.data.LedgerState
import com.moneymanager.data.Account
import com.moneymanager.data.AccountKind
import com.moneymanager.data.Accounts
import com.moneymanager.data.short
import com.moneymanager.data.money
import com.moneymanager.data.today
import com.moneymanager.ui.icon
import com.moneymanager.ui.accountIcon
import com.moneymanager.ui.BalanceTrace
import com.moneymanager.ui.Chip
import com.moneymanager.ui.DueCalendar
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.EmptyWater
import com.moneymanager.ui.Flag as FlagChip
import com.moneymanager.ui.LedgerRow
import com.moneymanager.ui.Marker
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.Routes
import com.moneymanager.ui.ScaffoldNote
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.Stat
import com.moneymanager.ui.SubmergedPhoto
import com.moneymanager.ui.WaterBar
import com.moneymanager.ui.WaterColumn
import com.moneymanager.ui.categoryScale
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** Sample-only mapping from a goal to its photograph. Goes away with the rest of state.kt. */
private fun goalPhoto(id: String): Int = when (id) {
    "g2" -> R.drawable.goal_laptop
    "g3" -> R.drawable.goal_bali
    else -> R.drawable.goal_emergency
}

/* ---------------------------------------------------------------- Accounts */

@Composable
fun AccountsScreen(state: LedgerState, onBack: () -> Unit, onOpenAccount: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val owned = Accounts.all.filter { it.balanceMinor >= 0 }.sumOf { it.balanceMinor }
    val owed = Accounts.all.filter { it.balanceMinor < 0 }.sumOf { -it.balanceMinor }

    DetailScaffold(title = "Accounts", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        Stat("Across everything", owned - owed, style = MoneyType.large)
                        Stat("Spendable now", state.liquidMinor, style = MoneyType.medium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("Transfer", Icons.Rounded.SwapHoriz, {}, emphasis = true)
                        Pill("Reconcile", Icons.Rounded.Check, {})
                    }
                }
            }
        }

        AccountKind.entries.forEach { kind ->
            val group = Accounts.all.filter { it.kind == kind }
            if (group.isEmpty()) return@forEach
            item(key = "head-$kind") { SectionHeading(kind.label) }
            item(key = "group-$kind") {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        group.forEach { account -> AccountRow(account) { onOpenAccount(account.id) } }
                    }
                }
            }
        }

        item { SectionHeading("Assets", caption = "Things you own, counted toward net worth") }
        item {
            EmptyWater(
                "No assets tracked yet",
                "A motorbike, a laptop, a fridge. Record what it cost and when the warranty ends, " +
                    "and it counts toward net worth instead of vanishing the day you bought it.",
                actionLabel = "Add an asset",
                actionIcon = Icons.Rounded.Bolt,
                onAction = {},
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val negative = account.balanceMinor < 0

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Marker(accountIcon(account.kind), size = 38.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                Text(
                    account.currency + (if (negative) " · owed" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            MoneyText(
                account.balanceMinor,
                style = MoneyType.row,
                color = if (negative) water.alert else scheme.onSurface,
            )
        }
        account.limitMinor?.let { limit ->
            val used = account.balanceMinor.absoluteValue.toFloat() / limit
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WaterBar(fraction = used, over = used > 0.7f, modifier = Modifier.fillMaxWidth(), height = 8.dp)
                Text(
                    "${(used * 100).roundToInt()}% of a ${money(limit)} limit used",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Pill("Open", Icons.Rounded.Check, onClick, container = scheme.surfaceContainerLow)
    }
}

@Composable
fun AccountDetailScreen(
    state: LedgerState,
    id: String,
    onBack: () -> Unit,
    onOpenTxn: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val account = Accounts[id]
    val rows = state.allTransactions.filter { it.accountId == id }

    // A running balance backwards from today, so the trace is the account's own history rather
    // than a decorative squiggle.
    val trace = buildList {
        var running = account.balanceMinor
        add(running)
        rows.forEach { running -= it.amountMinor; add(running) }
    }.reversed()

    DetailScaffold(title = account.name, onBack = onBack) {
        item {
            Box(Modifier.padding(top = 4.dp)) {
                WaterColumn(
                    level = 0.42f,
                    over = account.balanceMinor < 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    swell = false,
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            if (account.balanceMinor < 0) "Owed on this card" else "Balance",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                        )
                        MoneyText(
                            account.balanceMinor.absoluteValue,
                            style = MoneyType.hero,
                            color = if (account.balanceMinor < 0) water.alert else scheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "${account.kind.label} · ${account.currency}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item { SectionHeading("Running balance") }
        item { Plate { BalanceTrace(trace) } }

        item { SectionHeading("Details") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Fact("Kind", account.kind.label)
                    Fact("Currency", account.currency)
                    account.limitMinor?.let { Fact("Credit limit", money(it)) }
                    Fact("Included in totals", "Yes")
                }
            }
        }

        item {
            SectionHeading("Transactions", caption = "${rows.size} this month")
        }
        item {
            if (rows.isEmpty()) {
                EmptyWater(
                    "Nothing on this account yet",
                    "Log something against it and it starts building a history here.",
                )
            } else {
                Plate {
                    Column { rows.forEach { LedgerRow(it, onClick = { onOpenTxn(it.id) }, showDate = true) } }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* ------------------------------------------------------------------- Bills */

@Composable
fun BillsScreen(state: LedgerState, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val overdue = state.bills.filter { it.due < today }.sortedBy { it.due }
    val soon = state.bills.filter { it.due >= today }.sortedBy { it.due }
    val subs = state.bills.filter { it.subscription }
    val idle = subs.filter { (it.idleMonths ?: 0) >= 3 }

    DetailScaffold(title = "Bills & subscriptions", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Stat("Left this month", state.committedMinor, style = MoneyType.large)
                    Stat("Subscriptions", subs.sumOf { it.amountMinor }, style = MoneyType.medium)
                }
            }
        }

        if (overdue.isNotEmpty()) {
            item { SectionHeading("Overdue", caption = "Pay or reschedule these first") }
            item {
                Plate {
                    Column { overdue.forEach { BillRow(it) } }
                }
            }
        }

        item { SectionHeading("Due dates", caption = "Every payment this month, and how they cluster") }
        item {
            Plate {
                DueCalendar(
                    dueDays = state.bills
                        .filter { java.time.YearMonth.from(it.due) == com.moneymanager.data.thisMonth }
                        .map { it.due.dayOfMonth }
                        .toSet(),
                    daysInMonth = com.moneymanager.data.daysInMonth,
                    firstDayOffset = com.moneymanager.data.thisMonth.atDay(1).dayOfWeek.value - 1,
                    todayDay = com.moneymanager.data.dayOfMonth,
                )
            }
        }

        item { SectionHeading("Coming up", caption = "Next ${soon.size} payments") }
        item {
            Plate {
                Column { soon.forEach { BillRow(it) } }
            }
        }

        if (idle.isNotEmpty()) {
            item {
                SectionHeading(
                    "Paying for, not using",
                    caption = "Flagged from how long since the last related transaction",
                )
            }
            item {
                Plate(depth = 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        idle.forEach { bill ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Marker(Icons.Rounded.Subscriptions, tint = water.alert, size = 38.dp)
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(bill.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                                    Text(
                                        "${money(bill.amountMinor)} a month · " +
                                            "${money(bill.amountMinor * 12)} a year",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                FlagChip(
                                    Icons.Rounded.Bolt,
                                    "${bill.idleMonths} mo idle",
                                    water.alert,
                                )
                            }
                        }
                        Text(
                            "Cancelling both would free ${money(idle.sumOf { it.amountMinor } * 12)} a year.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionHeading("Reminders") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Fact("Notify", "Two days before, at 09:00")
                    Fact("Delivered by", "A scheduled job on this device")
                    Text(
                        "Reminders are local. They fire with no network and nothing about your " +
                            "bills leaves the phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* ------------------------------------------------------------------- Goals */

@Composable
fun GoalsScreen(state: LedgerState, onBack: () -> Unit, onGo: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val scale = categoryScale()

    DetailScaffold(title = "Goals", onBack = onBack) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.goals.forEach { goal ->
                    Column(
                        Modifier.width(176.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SubmergedPhoto(
                            painter = painterResource(goalPhoto(goal.id)),
                            level = goal.fraction,
                            shape = RoundedCornerShape(24.dp),
                            contentDescription = "${goal.name}, " +
                                "${(goal.fraction * 100).roundToInt()} percent saved",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(226.dp),
                        )
                        Text(
                            goal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                            maxLines = 1,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MoneyText(
                                goal.savedMinor,
                                style = MoneyType.medium,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(goal.fraction * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            ScaffoldNote(
                "Goal photographs are placeholder stock. Replace them with the real thing: " +
                    "res/drawable-nodpi/goal_*.jpg"
            )
        }

        item { SectionHeading("Every goal") }
        items(state.goals.size) { index ->
            val goal = state.goals[index]
            val monthsLeft = ChronoUnit.MONTHS.between(today, goal.by).coerceAtLeast(1)
            val perMonth = (goal.targetMinor - goal.savedMinor) / monthsLeft

            Plate(Modifier.padding(bottom = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Marker(Icons.Rounded.Savings, tint = water.goal, size = 38.dp)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(goal.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                            Text(
                                "${money(goal.savedMinor)} of ${money(goal.targetMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "${(goal.fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    WaterBar(goal.fraction, modifier = Modifier.fillMaxWidth(), height = 12.dp)
                    Text(
                        "${money(perMonth)} a month for $monthsLeft months hits it by " +
                            "${goal.by.dayOfMonth} ${goal.by.month.short()} ${goal.by.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeading("Debt", caption = "Snowball or avalanche, and what each one costs") }
        item {
            Plate(onClick = { onGo(Routes.DEBT) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Marker(Icons.Rounded.Flag, tint = water.alert, size = 38.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text("Payoff plan", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        Text(
                            "${state.debts.size} debts · ${money(state.debts.sumOf { it.balanceMinor })} owed",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* -------------------------------------------------------------------- Debt */

@Composable
fun DebtScreen(state: LedgerState, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    var avalanche by remember { mutableStateOf(true) }

    val ordered = if (avalanche) {
        state.debts.sortedByDescending { it.aprBasisPoints }
    } else {
        state.debts.sortedBy { it.balanceMinor }
    }
    val total = state.debts.sumOf { it.balanceMinor }
    val minimums = state.debts.sumOf { it.minimumMinor }

    DetailScaffold(title = "Debt payoff", onBack = onBack) {
        item {
            Box(Modifier.padding(top = 4.dp)) {
                WaterColumn(
                    level = 0.62f,
                    over = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp),
                    swell = false,
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Owed in total",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant,
                        )
                        MoneyText(
                            total,
                            style = MoneyType.hero,
                            color = scheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "${money(minimums)} a month in minimums alone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item {
            ChipRow(Modifier.padding(top = 14.dp)) {
                Chip("Avalanche", selected = avalanche, onClick = { avalanche = true })
                Chip("Snowball", selected = !avalanche, onClick = { avalanche = false })
            }
        }
        item {
            Text(
                if (avalanche) {
                    "Avalanche: highest interest rate first. Costs the least overall."
                } else {
                    "Snowball: smallest balance first. Clears an account soonest, which is easier to keep up."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item { SectionHeading("Pay in this order") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    ordered.forEachIndexed { i, debt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .background(
                                        if (i == 0) water.alert.copy(alpha = 0.18f)
                                        else scheme.surfaceContainerLow,
                                        RoundedCornerShape(50),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${i + 1}",
                                    style = MoneyType.small,
                                    color = if (i == 0) water.alert else scheme.onSurfaceVariant,
                                )
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(debt.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                                Text(
                                    if (debt.aprBasisPoints == 0) {
                                        "No interest · ${money(debt.minimumMinor)} a month"
                                    } else {
                                        "${debt.aprBasisPoints / 100.0}% APR · " +
                                            "${money(debt.minimumMinor)} a month"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            MoneyText(debt.balanceMinor, style = MoneyType.row, color = scheme.onSurface)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* ----------------------------------------------------------------- Capture */

/**
 * The free replacements for bank aggregation, each one honest about what it needs. None of them
 * post to the ledger on their own: every path ends at a filled-in form the user confirms.
 */
@Composable
fun CaptureScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water

    DetailScaffold(title = "Catch every transaction", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp), depth = 1) {
                Text(
                    "There is no bank sync, on purpose: every aggregator charges per linked " +
                        "account. These three paths cover the same ground for nothing, and all " +
                        "three stop at a form you confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        item {
            CaptureCard(
                Icons.Rounded.PhotoCamera,
                "Scan a receipt",
                "Photograph it. The merchant, date and total are read off the image on this " +
                    "device, then dropped into a new transaction for you to check.",
                "Needs the camera",
                "Open the camera",
            )
        }
        item {
            CaptureCard(
                Icons.Rounded.Email,
                "Import from email",
                "Connect Gmail read-only. Order confirmations and invoices are parsed into " +
                    "draft transactions; nothing else in the mailbox is touched.",
                "Needs read-only Gmail access, which you can revoke at any time",
                "Connect Gmail",
            )
        }
        item {
            CaptureCard(
                Icons.Rounded.Description,
                "Import a statement",
                "Export CSV, XLSX or PDF from your bank and drop it here. Rows are matched " +
                    "against what you have already logged, so a scanned receipt and its statement " +
                    "line merge instead of counting twice.",
                "Needs nothing at all. Parsed on this device.",
                "Choose a file",
            )
        }

        item { SectionHeading("What it learns") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Fact("Merchant memory", "Kopi Kenangan → Food & drink")
                    Fact("Duplicate window", "Same merchant and amount within 3 days")
                    Text(
                        "Categories learned from your own corrections, kept on this device, and " +
                            "used only to pre-fill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeading("Not available") }
        item {
            Plate(depth = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlagChip(Icons.Rounded.Bolt, "Reading SMS is not possible", water.alert)
                    Text(
                        "Google Play only allows SMS permissions for apps whose main job is " +
                            "handling SMS. A finance app asking for it gets rejected, so it is not " +
                            "coming later either. Email and statement import cover the same need.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CaptureCard(
    icon: ImageVector,
    title: String,
    body: String,
    permission: String,
    action: String,
) {
    val scheme = MaterialTheme.colorScheme
    Plate(Modifier.padding(top = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Marker(icon, size = 42.dp)
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Text(
                permission,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
            Pill(action, Icons.Rounded.Check, {}, emphasis = true)
        }
    }
}

/* ---------------------------------------------------------------- Rewards */

/**
 * Gamification with nothing of its own to lose. Every figure below is computed from real ledger
 * activity at read time -- transactions logged, bills cleared, budgets held -- so there is no
 * second store of progress that can drift away from the money.
 */
@Composable
fun RewardsScreen(state: LedgerState, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    var leaderboard by remember { mutableStateOf(false) }

    val earned = state.badges.count { it.earnedOn != null }
    val xp = state.transactions.size * 10 + earned * 250 + state.loggingStreakDays * 15
    val level = xp / 400 + 1
    val intoLevel = (xp % 400) / 400f

    DetailScaffold(title = "Progress", onBack = onBack) {
        item {
            Box(Modifier.padding(top = 4.dp)) {
                WaterColumn(
                    level = intoLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        FlagChip(
                            Icons.Rounded.LocalFireDepartment,
                            "${state.loggingStreakDays} days logged in a row",
                            water.goal,
                        )
                        Text(
                            "Level $level",
                            style = MaterialTheme.typography.displaySmall,
                            color = scheme.onSurface,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            "${400 - (xp % 400)} XP to the next one. XP comes from logging, " +
                                "clearing bills on time and holding a budget.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }

        item { SectionHeading("Badges", caption = "$earned of ${state.badges.size} earned") }
        items(state.badges.size) { index ->
            val badge = state.badges[index]
            val done = badge.earnedOn != null
            Plate(Modifier.padding(bottom = 10.dp), depth = if (done) 0 else 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Marker(
                        if (done) Icons.Rounded.MilitaryTech else Icons.Rounded.EmojiEvents,
                        tint = if (done) water.goal else scheme.onSurfaceVariant,
                        size = 42.dp,
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(badge.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        Text(
                            badge.blurb,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        if (!done) {
                            WaterBar(badge.progress, modifier = Modifier.fillMaxWidth(), height = 6.dp)
                        }
                    }
                    if (done) {
                        Text(
                            "Earned",
                            style = MaterialTheme.typography.labelMedium,
                            color = water.goal,
                        )
                    } else {
                        Text(
                            "${(badge.progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionHeading("Challenges") }
        items(state.challenges.size) { index ->
            val challenge = state.challenges[index]
            Plate(Modifier.padding(bottom = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                challenge.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                            Text(
                                challenge.blurb,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        if (challenge.savedMinor > 0) {
                            MoneyText(
                                challenge.savedMinor,
                                style = MoneyType.row,
                                color = water.income,
                                showSign = true,
                            )
                        }
                    }
                    WaterBar(
                        challenge.dayOf.toFloat() / challenge.days,
                        modifier = Modifier.fillMaxWidth(),
                        height = 10.dp,
                    )
                    Text(
                        "Day ${challenge.dayOf} of ${challenge.days}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeading("Comparing with people") }
        item {
            Plate(depth = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Marker(Icons.Rounded.Leaderboard, tint = scheme.onSurfaceVariant, size = 38.dp)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                "Share a leaderboard",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                            Text(
                                "Off by default",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = leaderboard, onCheckedChange = { leaderboard = it })
                    }
                    Text(
                        "Only savings rate and streak length are ever shared, and only with people " +
                            "already in a shared wallet. Balances and transactions never are.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
