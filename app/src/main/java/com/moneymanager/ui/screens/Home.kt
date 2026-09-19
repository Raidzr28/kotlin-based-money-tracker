package com.moneymanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanager.data.LedgerState
import com.moneymanager.data.Accounts
import com.moneymanager.data.Bill
import com.moneymanager.data.Categories
import com.moneymanager.data.Flow
import com.moneymanager.data.daysInMonth
import com.moneymanager.data.daysLeft
import com.moneymanager.data.dayOfMonth
import com.moneymanager.data.dueLabel
import com.moneymanager.data.money
import com.moneymanager.data.monthPace
import com.moneymanager.data.thisMonth
import com.moneymanager.data.today
import com.moneymanager.ui.icon
import com.moneymanager.ui.accountIcon
import com.moneymanager.ui.DepthGauge
import com.moneymanager.ui.Flag
import com.moneymanager.ui.LedgerRow
import com.moneymanager.ui.LegendDot
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyMotion
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.Marker
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.Routes
import com.moneymanager.ui.ScaffoldNote
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.SoundingLine
import com.moneymanager.ui.Stat
import com.moneymanager.ui.WaterBar
import com.moneymanager.ui.WaterColumn
import com.moneymanager.ui.categoryScale
import com.moneymanager.ui.toneFor
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * The thesis screen.
 *
 * The hero is not a balance card with statistics stapled underneath. The figure and the water
 * are one fact: the fill height IS what is left of the month, and the gauge down the right edge
 * is how much month is left to spend it over. If the water is lower than the gauge has travelled,
 * the user is spending ahead of the clock, and the screen says so in words as well.
 */
@Composable
fun HomeScreen(
    state: LedgerState,
    onOpenTxn: (String) -> Unit,
    onOpenBudget: (String) -> Unit,
    onGo: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val behindPace = state.safeFraction < (1f - monthPace)
    // The shell hands the top inset to the screen. The hero swallows it: the column starts at
    // the physical top of the display and its contents start below the clock.
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // The hero is the top edge of the screen, not a card sitting below one.
    TabColumn(top = 0.dp, bottom = 96.dp, insetTop = false) {
        // The first viewport is one object: the header sits on the water, the figure sits over
        // the surface line, and the fill height is the figure. No gutter, no card.
        item {
            WaterColumn(
                level = state.safeFraction,
                fillMillis = MoneyMotion.FillHero,
                shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp),
                modifier = Modifier
                    .bleed()
                    .fillMaxWidth()
                    .height(408.dp + statusTop)
                    .semantics(mergeDescendants = false) {
                        contentDescription =
                            "Safe to spend ${money(state.safeToSpendMinor)}, " +
                            "${(state.safeFraction * 100).toInt()} percent of this month's budget, " +
                            "$daysLeft days remaining"
                    },
            ) {
                DepthGauge(
                    elapsed = dayOfMonth,
                    total = daysInMonth,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 74.dp + statusTop, bottom = 24.dp, end = 12.dp)
                        .height(310.dp),
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = Gutter,
                            end = Gutter + 24.dp,
                            top = 14.dp + statusTop,
                            bottom = 22.dp,
                        )
                ) {
                    HomeHeader(state, onGo)

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "Safe to spend",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                    MoneyText(
                        state.safeToSpendMinor,
                        style = MoneyType.hero,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "${money(state.perDayMinor)} a day for the $daysLeft days left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (behindPace) {
                        Flag(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            "${(state.safeFraction * 100).toInt()}% of budget left, " +
                                "${(monthPace * 100).toInt()}% of the month gone",
                            water.alert,
                            Modifier.padding(top = 12.dp),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroPill("Transfer", Icons.Rounded.SwapHoriz) { onGo(Routes.ACCOUNTS) }
                        HeroPill("Scan", Icons.Rounded.PhotoCamera) { onGo(Routes.CAPTURE) }
                        HeroPill("Bills", Icons.Rounded.CalendarMonth) { onGo(Routes.BILLS) }
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Today",
                caption = if (state.loggedToday) {
                    "${state.todaysTransactions.size} logged · " +
                        money(state.todaysTransactions.sumOf { -it.amountMinor }) + " out"
                } else {
                    "Nothing logged yet"
                },
                actionLabel = "All",
                onAction = { onGo(Routes.LEDGER) },
            )
        }

        item {
            if (state.loggedToday) {
                Plate {
                    Column {
                        state.todaysTransactions.forEach { txn ->
                            LedgerRow(txn, onClick = { onOpenTxn(txn.id) })
                        }
                    }
                }
            } else {
                Plate(depth = 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Flag(
                            Icons.Rounded.LocalFireDepartment,
                            "${state.loggingStreakDays}-day streak",
                            water.goal,
                        )
                        Text(
                            "Log one thing today and the streak holds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Envelopes",
                caption = "${state.budgets.count { it.over }} over, " +
                    "${state.budgets.count { !it.over }} still holding",
                actionLabel = "All",
                onAction = { onGo(Routes.BUDGETS) },
            )
        }

        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    state.budgets.sortedByDescending { it.fraction }.take(4).forEach { budget ->
                        val category = Categories[budget.categoryId]
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBudget(budget.categoryId) }
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    category.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                MoneyText(
                                    budget.remainingMinor.absoluteValue,
                                    style = MoneyType.small,
                                    color = if (budget.over) water.alert else scheme.onSurfaceVariant,
                                )
                                Text(
                                    if (budget.over) " over" else " left",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (budget.over) water.alert else scheme.onSurfaceVariant,
                                )
                            }
                            WaterBar(
                                fraction = budget.fraction,
                                over = budget.over,
                                paceAt = monthPace,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(scheme.primary, "spent")
                        LegendDot(scheme.onSurfaceVariant, "today's pace")
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Next out",
                caption = "${money(state.committedMinor)} committed before the month ends",
                actionLabel = "Bills",
                onAction = { onGo(Routes.BILLS) },
            )
        }

        item {
            Plate {
                Column {
                    state.bills.sortedBy { it.due }.take(4).forEach { bill ->
                        BillRow(bill)
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Net worth",
                caption = "Twelve months. What you own above the line, what you owe below it.",
            )
        }

        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Stat("Net", state.netWorthMinor, style = MoneyType.large)
                        Stat(
                            "Owned",
                            state.netWorthAssets.last(),
                            tone = scheme.primary,
                            style = MoneyType.medium,
                        )
                        Stat(
                            "Owed",
                            state.netWorthDebts.last(),
                            tone = water.alert,
                            style = MoneyType.medium,
                        )
                    }
                    SoundingLine(state.netWorthAssets, state.netWorthDebts)
                }
            }
        }

        item {
            SectionHeading(
                "Accounts",
                caption = "${money(state.liquidMinor)} available right now",
                actionLabel = "Manage",
                onAction = { onGo(Routes.ACCOUNTS) },
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Accounts.all.forEach { account ->
                    Plate(
                        modifier = Modifier.width(168.dp),
                        depth = 1,
                        padding = 16.dp,
                        onClick = { onGo(Routes.account(account.id)) },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Marker(accountIcon(account.kind), size = 34.dp)
                            Text(
                                account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MoneyText(
                                account.balanceMinor,
                                style = MoneyType.medium,
                                color = if (account.balanceMinor < 0) water.alert else scheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        item {
            ScaffoldNote(
                "Scaffold build. Every figure on this screen is synthetic sample data from " +
                    "data/state.kt. No account is connected and nothing is stored yet."
            )
        }
    }
}

@Composable
private fun HomeHeader(state: LedgerState, onGo: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val monthName = thisMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "$monthName ${today.year}",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface,
            )
            Text(
                "Day $dayOfMonth of $daysInMonth",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Flag(
            Icons.Rounded.LocalFireDepartment,
            "${state.loggingStreakDays}",
            water.goal,
            Modifier.padding(end = 4.dp),
        )
        IconButton(onClick = { onGo(Routes.BILLS) }) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Reminders",
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

/** A pill that sits on top of the water rather than on the ground. */
@Composable
private fun HeroPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Pill(
        label = label,
        icon = icon,
        onClick = onClick,
        container = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun BillRow(
    bill: Bill,
    modifier: Modifier = Modifier,
    onPay: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val overdue = bill.due < today
    val scale = categoryScale()
    val tint = if (bill.subscription) scale[5] else scale[1]

    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Marker(
            if (bill.subscription) Icons.Rounded.Subscriptions else Icons.Rounded.CalendarMonth,
            tint = tint,
            size = 38.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(bill.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${dueLabel(bill.due)} · ${bill.every.label.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) water.alert else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoneyText(
            -bill.amountMinor,
            style = MoneyType.row,
            color = if (overdue) water.alert else toneFor(Flow.Out),
        )
        if (onPay != null) {
            Pill(
                "Paid", Icons.Rounded.Check, onPay,
                modifier = Modifier.padding(start = 10.dp),
                container = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        }
    }
}
