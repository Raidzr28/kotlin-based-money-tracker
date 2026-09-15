package com.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneymanager.data.LedgerState
import com.moneymanager.data.money
import com.moneymanager.ui.icon
import com.moneymanager.ui.Chip
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.Flag as FlagChip
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.NavRow
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.Routes
import com.moneymanager.ui.ScaffoldNote
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.categoryScale

/* -------------------------------------------------------------------- More */

@Composable
fun MoreScreen(state: LedgerState, onGo: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val scale = categoryScale()

    TabColumn {
        item {
            Text(
                "More",
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        }

        item { SectionHeading("Money") }
        item {
            Plate {
                Column {
                    NavRow(
                        Icons.Rounded.AccountBalanceWallet, "Accounts",
                        subtitle = "${com.moneymanager.data.Accounts.all.size} accounts and wallets",
                        trailing = money(state.liquidMinor),
                        tint = scale[0],
                    ) { onGo(Routes.ACCOUNTS) }
                    NavRow(
                        Icons.Rounded.CalendarMonth, "Bills & subscriptions",
                        subtitle = "${state.bills.size} tracked",
                        trailing = money(state.committedMinor),
                        tint = scale[1],
                    ) { onGo(Routes.BILLS) }
                    NavRow(
                        Icons.Rounded.Savings, "Goals",
                        subtitle = "${state.goals.size} in progress",
                        tint = water.goal,
                    ) { onGo(Routes.GOALS) }
                    NavRow(
                        Icons.Rounded.Flag, "Debt payoff",
                        subtitle = "Snowball or avalanche",
                        trailing = money(state.debts.sumOf { it.balanceMinor }),
                        tint = water.alert,
                    ) { onGo(Routes.DEBT) }
                }
            }
        }

        item { SectionHeading("Getting things in") }
        item {
            Plate {
                Column {
                    NavRow(
                        Icons.Rounded.PhotoCamera, "Receipts, email and statements",
                        subtitle = "The free alternatives to bank sync",
                        tint = scale[3],
                    ) { onGo(Routes.CAPTURE) }
                    NavRow(
                        Icons.Rounded.Language, "Currencies",
                        subtitle = "Base currency and exchange rates",
                        trailing = "USD",
                        tint = scale[6],
                    ) { onGo(Routes.CURRENCY) }
                }
            }
        }

        item { SectionHeading("You") }
        item {
            Plate {
                Column {
                    NavRow(
                        Icons.Rounded.EmojiEvents, "Progress",
                        subtitle = "Streaks, badges and challenges",
                        trailing = "${state.loggingStreakDays} days",
                        tint = water.goal,
                    ) { onGo(Routes.REWARDS) }
                    NavRow(
                        Icons.Rounded.Lock, "Lock & privacy",
                        subtitle = "PIN, biometrics, what leaves the phone",
                        tint = scale[2],
                    ) { onGo(Routes.SECURITY) }
                    NavRow(
                        Icons.Rounded.CloudSync, "Backup & sync",
                        subtitle = "Off. Everything is on this device.",
                        tint = scale[5],
                    ) { onGo(Routes.SYNC) }
                    NavRow(
                        Icons.Rounded.Settings, "Settings",
                        subtitle = "Appearance, periods, widgets, categories",
                        tint = scheme.onSurfaceVariant,
                    ) { onGo(Routes.SETTINGS) }
                }
            }
        }

        item {
            ScaffoldNote(
                "Money Manager 0.1.0 · scaffold build. No data is stored yet and no account is " +
                    "connected."
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/* ---------------------------------------------------------------- Settings */

@Composable
fun SettingsScreen(onBack: () -> Unit, onGo: (String) -> Unit, onLoadDemo: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var theme by remember { mutableStateOf("Follow the system") }
    var materialYou by remember { mutableStateOf(false) }
    var monthStart by remember { mutableStateOf("1st") }
    var weekStart by remember { mutableStateOf("Monday") }

    DetailScaffold(title = "Settings", onBack = onBack) {
        item { SectionHeading("Appearance", caption = "The app is built dark; light is a full second scheme, not an inversion.") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChipRow {
                        listOf("Follow the system", "Always dark", "Always light").forEach {
                            Chip(it, selected = theme == it, onClick = { theme = it })
                        }
                    }
                    ToggleRow(
                        "Use my wallpaper's colours",
                        "Material You replaces the depth palette. The waterline stops carrying " +
                            "meaning, so this is off unless you want it.",
                        materialYou,
                    ) { materialYou = it }
                }
            }
        }

        item { SectionHeading("Periods") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Month starts on the", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        ChipRow {
                            listOf("1st", "15th", "25th", "Payday").forEach {
                                Chip(it, selected = monthStart == it, onClick = { monthStart = it })
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Week starts on", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        ChipRow {
                            listOf("Monday", "Sunday", "Saturday").forEach {
                                Chip(it, selected = weekStart == it, onClick = { weekStart = it })
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeading("Automate the boring parts") }
        item {
            Plate {
                Column {
                    NavRow(
                        Icons.Rounded.Repeat, "Recurring templates",
                        subtitle = "Rent, salary and anything else that repeats",
                        trailing = "3",
                    ) {}
                    NavRow(
                        Icons.Rounded.Category, "Categories",
                        subtitle = "Icons, colours, subcategories",
                        trailing = "${com.moneymanager.data.Categories.all.size}",
                    ) {}
                    NavRow(
                        Icons.Rounded.Widgets, "Home screen widget",
                        subtitle = "Today's spend, balance, next bill",
                    ) {}
                    NavRow(
                        Icons.Rounded.Tune, "Notifications",
                        subtitle = "Bill reminders and streak warnings",
                    ) {}
                }
            }
        }

        item { SectionHeading("Your data") }
        item {
            Plate {
                Column {
                    NavRow(
                        Icons.Rounded.Download, "Load sample data",
                        subtitle = "A month of invented activity, for trying the app out",
                        onClick = onLoadDemo,
                    )
                    NavRow(Icons.Rounded.Download, "Export everything", subtitle = "CSV or JSON, written where you choose") {}
                    NavRow(Icons.Rounded.CloudSync, "Backup & sync", subtitle = "Off") { onGo(Routes.SYNC) }
                    NavRow(Icons.Rounded.Description, "Delete all data", subtitle = "Irreversible, and it asks twice") {}
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/* ---------------------------------------------------------------- Security */

@Composable
fun SecurityScreen(onBack: () -> Unit, onPreviewLock: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    var lock by remember { mutableStateOf(true) }
    var biometric by remember { mutableStateOf(true) }
    var hideAmounts by remember { mutableStateOf(false) }

    DetailScaffold(title = "Lock & privacy", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlagChip(Icons.Rounded.Lock, "Everything stays on this device", water.income)
                    Text(
                        "There is no account to sign into and no server holding your ledger. The " +
                            "lock below protects the phone, not a login.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeading("Opening the app") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    ToggleRow("Require a PIN", "Asked every time the app comes to the front.", lock) { lock = it }
                    ToggleRow(
                        "Allow fingerprint or face",
                        "Uses the phone's own biometric prompt. The PIN always still works.",
                        biometric,
                    ) { biometric = it }
                    ToggleRow(
                        "Hide amounts in the app switcher",
                        "Blurs the screenshot Android takes when you switch apps.",
                        hideAmounts,
                    ) { hideAmounts = it }
                    Fact("Auto-lock", "Immediately")
                    Pill("See the lock screen", Icons.Rounded.Fingerprint, onPreviewLock)
                }
            }
        }

        item { SectionHeading("What leaves the phone") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Fact("Analytics", "None")
                    Fact("Adverts", "None")
                    Fact("Exchange rates", "A daily rate lookup, if multi-currency is on")
                    Fact("Backup", "Only to your own Drive, only when you turn it on")
                    Text(
                        "No transaction, balance, merchant or receipt is sent anywhere unless you " +
                            "switch on backup or sync yourself.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* -------------------------------------------------------------------- Sync */

@Composable
fun SyncScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    var backup by remember { mutableStateOf(false) }
    var sync by remember { mutableStateOf(false) }

    DetailScaffold(title = "Backup & sync", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp), depth = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlagChip(Icons.Rounded.Check, "The app works with the network off", water.income)
                    Text(
                        "Local-first is the default, not a fallback. Everything below is optional, " +
                            "and all of it is free at one household's scale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionHeading("Backup") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleRow(
                        "Back up to my Google Drive",
                        "Written to the app's private folder in your own Drive. It uses your " +
                            "storage quota, costs nothing, and no one else can read it.",
                        backup,
                    ) { backup = it }
                    Fact("Last backup", if (backup) "Never" else "Off")
                    Pill("Restore from a backup", Icons.Rounded.Download, {})
                }
            }
        }

        item { SectionHeading("Two phones, one wallet") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleRow(
                        "Sync across devices",
                        "Uses Firebase's free tier. A household stays well inside it; there is no " +
                            "card on file and no trial that expires.",
                        sync,
                    ) { sync = it }
                    Fact("Shared wallets", if (sync) "None yet" else "Needs sync on")
                    Fact("Conflict rule", "The most recent edit wins, and both versions are kept")
                }
            }
        }

        item { SectionHeading("Where the free tiers stop") }
        item {
            Plate(depth = 2) {
                Text(
                    "Free tiers are quota-capped, not unconditional. One person or one family sits " +
                        "far inside them. A few thousand strangers sharing this project's Firebase " +
                        "would not, and that is worth knowing before you hand the app around.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* ---------------------------------------------------------------- Currency */

@Composable
fun CurrencyScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var base by remember { mutableStateOf("USD") }
    var auto by remember { mutableStateOf(true) }

    val rates = listOf(
        "EUR" to "0.9210", "GBP" to "0.7845", "IDR" to "15,842.00",
        "JPY" to "147.31", "SGD" to "1.2914", "AUD" to "1.5077",
    )

    DetailScaffold(title = "Currencies", onBack = onBack) {
        item { SectionHeading("Base currency", caption = "Everything totals into this one") }
        item {
            Plate {
                ChipRow {
                    listOf("USD", "EUR", "GBP", "IDR", "JPY").forEach {
                        Chip(it, selected = base == it, onClick = { base = it })
                    }
                }
            }
        }

        item { SectionHeading("Rates") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleRow(
                        "Update rates daily",
                        "From Frankfurter, which is free, needs no key and has no call limit. " +
                            "One request a day.",
                        auto,
                    ) { auto = it }
                    Fact("Last updated", "Today, 06:12")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rates.forEach { (code, rate) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "1 $base = $rate",
                                    style = MoneyType.small,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeading("Offline") }
        item {
            Plate(depth = 1) {
                Text(
                    "With no network the last known rate is used and the transaction is marked as " +
                        "converted at that rate. You can override any rate by hand, and the " +
                        "override sticks to that transaction forever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* -------------------------------------------------------------------- Lock */

/**
 * The lock screen is the app's first impression more often than Home is. It gets the same water,
 * the same mark, and a keypad big enough to use without looking.
 */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(pin) { if (pin.length >= 4) onUnlock() }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Box(Modifier.size(34.dp, 6.dp).clip(RoundedCornerShape(50)).background(scheme.primary))
                Box(Modifier.size(24.dp, 6.dp).clip(RoundedCornerShape(50)).background(scheme.primary.copy(alpha = 0.7f)))
                Box(Modifier.size(14.dp, 6.dp).clip(RoundedCornerShape(50)).background(scheme.primary.copy(alpha = 0.45f)))
                Box(Modifier.size(40.dp, 2.dp).background(water.waterline))
            }
        }

        Text(
            "Money Manager",
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurface,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "Enter your PIN",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(
            Modifier.padding(top = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.length) scheme.primary
                            else scheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("bio", "0", "del"),
            ).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { key ->
                        LockKey(key, Modifier.weight(1f)) {
                            when (key) {
                                "del" -> pin = pin.dropLast(1)
                                "bio" -> onUnlock()
                                else -> pin = (pin + key).take(4)
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Forgot your PIN? The ledger cannot be recovered without it.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LockKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .height(62.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.surfaceContainer.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (label) {
            "del" -> Icon(Icons.Rounded.Backspace, "Delete last digit", tint = scheme.onSurfaceVariant)
            "bio" -> Icon(Icons.Rounded.Fingerprint, "Unlock with fingerprint", tint = scheme.primary)
            else -> Text(label, style = MoneyType.medium, color = scheme.onSurface)
        }
    }
}
