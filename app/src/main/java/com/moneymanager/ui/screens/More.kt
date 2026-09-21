package com.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.os.Build
import com.moneymanager.AppearanceState
import com.moneymanager.data.AppPrefs
import com.moneymanager.data.LedgerState
import com.moneymanager.data.Periods
import com.moneymanager.data.ThemeMode
import java.time.DayOfWeek
import com.moneymanager.data.RATE_SCALE
import com.moneymanager.data.RatesStore
import com.moneymanager.data.SUPPORTED_CURRENCIES
import com.moneymanager.data.SecurityStore
import com.moneymanager.data.money
import com.moneymanager.ui.icon
import com.moneymanager.ui.Chip
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.EmptyWater
import com.moneymanager.ui.Flag as FlagChip
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.biometricAvailable
import com.moneymanager.ui.rememberBiometricPrompt
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.NavRow
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.Routes
import com.moneymanager.ui.ScaffoldNote
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.WaterPulse
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
                        trailing = state.baseCurrency,
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
fun SettingsScreen(
    prefs: AppPrefs,
    appearance: AppearanceState,
    templateCount: Int,
    categoryCount: Int,
    onBack: () -> Unit,
    onGo: (String) -> Unit,
    onLoadDemo: () -> Unit,
    onExport: () -> Unit,
    onPinWidget: () -> Unit,
    onDeleteEverything: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var monthStart by remember { mutableStateOf(prefs.monthStartDay) }
    var weekStart by remember { mutableStateOf(prefs.weekStart) }
    // Two taps to erase a ledger, and the second one carries a different label. The subtitle
    // has always promised this; arming it in local state keeps the promise, and leaving the
    // screen disarms it, which is the behaviour you want from a control this size.
    var armedToDelete by remember { mutableStateOf(false) }

    DetailScaffold(title = "Settings", onBack = onBack) {
        item { SectionHeading("Appearance", caption = "The app is built dark; light is a full second scheme, not an inversion.") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChipRow {
                        ThemeMode.entries.forEach { mode ->
                            Chip(
                                mode.label,
                                selected = appearance.themeMode == mode,
                                onClick = { appearance.useTheme(mode) },
                            )
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ToggleRow(
                            "Use my wallpaper colours",
                            "Material You takes the plates, chips and sheets. The water column " +
                                "keeps its own depth palette, because that ladder is what makes " +
                                "the waterline a reading rather than a decoration.",
                            appearance.materialYou,
                        ) { appearance.useMaterialYou(it) }
                    }
                }
            }
        }

        item { SectionHeading("Periods", caption = "Every figure on Home and Budgets is measured over this.") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Month starts on the", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        ChipRow {
                            listOf(1 to "1st", 15 to "15th", 25 to "25th", Periods.PAYDAY to "Payday")
                                .forEach { (day, label) ->
                                    Chip(
                                        label,
                                        selected = monthStart == day,
                                        onClick = {
                                            monthStart = day
                                            prefs.setPeriods(day, weekStart)
                                        },
                                    )
                                }
                        }
                        if (monthStart == Periods.PAYDAY) {
                            Text(
                                "Read off your own income: the median day of the month money " +
                                    "arrived over the last six. Currently the " +
                                    ordinal(Periods.startDay) + ".",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Week starts on", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        ChipRow {
                            listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).forEach { day ->
                                Chip(
                                    day.name.lowercase().replaceFirstChar { it.uppercase() },
                                    selected = weekStart == day,
                                    onClick = {
                                        weekStart = day
                                        prefs.setPeriods(monthStart, day)
                                    },
                                )
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
                        trailing = templateCount.toString(),
                    ) { onGo(Routes.TEMPLATES) }
                    NavRow(
                        Icons.Rounded.Category, "Categories",
                        subtitle = "Icons, names, sub-categories",
                        trailing = categoryCount.toString(),
                    ) { onGo(Routes.CATEGORIES) }
                    NavRow(
                        Icons.Rounded.Widgets, "Home screen widget",
                        subtitle = "Today spend, balance, next bill",
                        onClick = onPinWidget,
                    )
                    NavRow(
                        Icons.Rounded.Tune, "Notifications",
                        subtitle = if (prefs.remindersEnabled) {
                            "On, " + prefs.remindDaysBefore + " days before at " +
                                "%02d:00".format(prefs.remindHour)
                        } else {
                            "Off"
                        },
                    ) { onGo(Routes.NOTIFICATIONS) }
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
                    NavRow(
                        Icons.Rounded.Download, "Export everything",
                        subtitle = "CSV, written where you choose",
                        onClick = onExport,
                    )
                    NavRow(Icons.Rounded.CloudSync, "Backup & sync", subtitle = "Off") { onGo(Routes.SYNC) }
                    NavRow(
                        Icons.Rounded.Description,
                        if (armedToDelete) "Tap again to erase everything" else "Delete all data",
                        subtitle = if (armedToDelete) {
                            "Every transaction, budget, bill, goal and debt. This cannot be undone."
                        } else {
                            "Irreversible, and it asks twice"
                        },
                        tint = if (armedToDelete) MoneyTheme.water.alert else scheme.primary,
                    ) {
                        if (armedToDelete) {
                            armedToDelete = false
                            onDeleteEverything()
                        } else {
                            armedToDelete = true
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** "25th", for a sentence about a derived payday. */
private fun ordinal(day: Int): String {
    val suffix = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return day.toString() + suffix
}

@Composable
internal fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(
                value = checked,
                onValueChange = onChange,
                role = Role.Switch,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Switch(checked = checked, onCheckedChange = null)
    }
}

/* ---------------------------------------------------------------- Security */

@Composable
fun SecurityScreen(
    security: SecurityStore,
    onBack: () -> Unit,
    onLockNow: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water

    // Mirrored into composition state so the switches respond immediately; the store is the
    // authority and is written on every change.
    var hasPin by remember { mutableStateOf(security.hasPin) }
    var lock by remember { mutableStateOf(security.lockEnabled) }
    var biometric by remember { mutableStateOf(security.biometricEnabled) }
    var hideAmounts by remember { mutableStateOf(security.hideInRecents) }
    var settingPin by remember { mutableStateOf(false) }
    val canBiometric = biometricAvailable()

    if (settingPin) {
        PinSheet(
            title = if (hasPin) "Change your PIN" else "Choose a PIN",
            onDismiss = { settingPin = false },
            onPin = {
                security.setPin(it)
                hasPin = true
                lock = true
                settingPin = false
            },
        )
    }

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

        if (!hasPin) {
            item {
                EmptyWater(
                    "No PIN set",
                    "Without one, anyone who picks up your unlocked phone can read every " +
                        "transaction you have logged.",
                    actionLabel = "Set a PIN",
                    actionIcon = Icons.Rounded.Lock,
                    onAction = { settingPin = true },
                )
            }
        } else {
            item {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ToggleRow(
                            "Require a PIN",
                            "Asked every time the app comes back to the front.",
                            lock,
                        ) { lock = it; security.lockEnabled = it }

                        ToggleRow(
                            if (canBiometric) "Allow fingerprint or face" else "Fingerprint or face",
                            if (canBiometric) {
                                "Uses the phone's own prompt. The PIN always still works."
                            } else {
                                "No strong biometric is enrolled on this device."
                            },
                            biometric && canBiometric,
                        ) { if (canBiometric) { biometric = it; security.biometricEnabled = it } }

                        ToggleRow(
                            "Hide amounts in the app switcher",
                            "Blanks the preview Android takes when you switch apps, and blocks " +
                                "screenshots. Takes effect next time the app starts.",
                            hideAmounts,
                        ) { hideAmounts = it; security.hideInRecents = it }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Pill("Lock now", Icons.Rounded.Lock, onLockNow, emphasis = true)
                            Pill("Change PIN", Icons.Rounded.Fingerprint, { settingPin = true })
                        }

                        Pill(
                            "Remove PIN", Icons.Rounded.Lock,
                            {
                                security.clearPin()
                                hasPin = false
                                lock = false
                            },
                            contentColor = water.alert,
                        )
                    }
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

/**
 * Choosing a PIN: enter it, then enter it again.
 *
 * The confirmation is not ceremony. A PIN nobody can reproduce locks the user out of their own
 * ledger permanently, because there is no account to recover it through and no copy of it
 * anywhere -- only a salted hash that cannot be reversed.
 */
@Composable
private fun PinSheet(
    title: String,
    onDismiss: () -> Unit,
    onPin: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    val confirming = first.length == PIN_LENGTH
    val entry = if (confirming) second else first

    LaunchedEffect(second) {
        if (second.length == PIN_LENGTH) {
            if (second == first) onPin(second) else {
                mismatch = true
                second = ""
                first = ""
            }
        }
    }

    MoneySheet(title = title, onDismiss = onDismiss) {
        Text(
            when {
                mismatch -> "Those did not match. Start again."
                confirming -> "Enter it once more."
                else -> "Four digits. There is no way to recover it, so pick one you will keep."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (mismatch) water.alert else scheme.onSurfaceVariant,
        )

        PinDots(entry.length, error = mismatch)

        Keypad(
            onDigit = {
                mismatch = false
                if (confirming) second = (second + it).take(PIN_LENGTH)
                else first = (first + it).take(PIN_LENGTH)
            },
            onDelete = { if (confirming) second = second.dropLast(1) else first = first.dropLast(1) },
        )
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
                    // Disabled rather than silent: restoring needs a backup to exist and a
                    // Drive account this build cannot ask for yet. A live-looking button that
                    // does nothing is worse than a dim one that says why.
                    Pill("Restore from a backup", Icons.Rounded.Download, {}, enabled = false)
                    Text(
                        "Available once a backup has been written.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
fun CurrencyScreen(rates: RatesStore, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(rates.multiCurrencyEnabled) }
    var base by remember { mutableStateOf(rates.baseCurrency) }
    var table by remember { mutableStateOf(rates.cached()) }
    var refreshing by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        refreshing = true
        scope.launch {
            // Off the main thread: this is a network call, and the app must stay responsive when
            // there is no network to answer it.
            table = withContext(Dispatchers.IO) { rates.refresh(base) }
            refreshing = false
        }
    }

    editing?.let { code ->
        ManualRateSheet(
            currency = code,
            base = base,
            current = table?.microsFor(code),
            onDismiss = { editing = null },
            onSet = { micros ->
                rates.setManualRate(code, micros)
                table = rates.cached()
                editing = null
            },
        )
    }

    DetailScaffold(title = "Currencies", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp)) {
                ToggleRow(
                    "Track more than one currency",
                    "Adds a currency picker when logging, and converts everything into your base " +
                        "currency for totals.",
                    enabled,
                ) {
                    enabled = it
                    rates.multiCurrencyEnabled = it
                    if (it && rates.isStale()) refresh()
                }
            }
        }

        if (!enabled) {
            item {
                ScaffoldNote(
                    "While this is off the app never touches the network. Everything you log is " +
                        "in your accounts' own currencies."
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            return@DetailScaffold
        }

        item { SectionHeading("Base currency", caption = "Everything totals into this one") }
        item {
            Plate {
                ChipRow {
                    SUPPORTED_CURRENCIES.forEach { code ->
                        Chip(
                            code,
                            selected = base == code,
                            onClick = {
                                base = code
                                rates.baseCurrency = code
                                refresh()
                            },
                        )
                    }
                }
            }
        }

        item {
            SectionHeading(
                "Rates",
                caption = table?.let {
                    val age = (System.currentTimeMillis() / 1000 - it.fetchedAtEpochSecond) / 3600
                    when {
                        it.manual -> "Edited by hand"
                        it.fetchedAtEpochSecond == 0L -> "Never fetched"
                        age < 1 -> "Updated within the hour"
                        age < 48 -> "Updated $age hours ago"
                        else -> "Updated ${age / 24} days ago"
                    }
                } ?: "Nothing fetched yet",
                actionLabel = if (refreshing) "Updating" else "Update",
                onAction = { if (!refreshing) refresh() },
            )
        }

        if (refreshing) {
            item { WaterPulse(Modifier.fillMaxWidth().padding(top = 4.dp)) }
        }

        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val known = table?.perBaseMicros.orEmpty()
                    if (known.isEmpty()) {
                        Text(
                            "No rates yet. Tap Update, or set one by hand below to work offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    SUPPORTED_CURRENCIES.filter { it != base }.forEach { code ->
                        val micros = known[code]
                        Row(
                            Modifier.clickable { editing = code },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                code,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (micros == null) "not set" else "1 $base = ${formatRate(micros)}",
                                style = MoneyType.small,
                                color = if (micros == null) scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeading("Where these come from") }
        item {
            Plate(depth = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlagChip(Icons.Rounded.Language, "Frankfurter, from ECB reference rates", water.income)
                    Text(
                        "Free, no key, no limit. The request sends one currency code and nothing " +
                            "else: no account, no amount, no identifier. It is the only outbound " +
                            "request this app makes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        "With no network the last rate is used and the transaction records the rate " +
                            "it was converted at, so it never changes afterwards. Tap any currency " +
                            "to set the rate your bank actually used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** Typing in the rate a card issuer actually used, which is rarely the ECB's. */
@Composable
private fun ManualRateSheet(
    currency: String,
    base: String,
    current: Long?,
    onDismiss: () -> Unit,
    onSet: (Long) -> Unit,
) {
    var digits by remember { mutableStateOf(current?.let { (it / 100).toString() }.orEmpty()) }
    val micros = digitsToMinor(digits) * 100

    MoneySheet(title = "1 $base in $currency", onDismiss = onDismiss) {
        Text(
            "Your card issuer's rate differs from the reference rate, and reconciling a statement " +
                "needs the number they actually used.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AmountInput(digits, { digits = it }, label = "Rate", currency = currency)
        Pill(
            if (micros <= 0L) "Enter a rate" else "Use ${formatRate(micros)}",
            Icons.Rounded.Check,
            { if (micros > 0L) onSet(micros) },
            emphasis = micros > 0L,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Micros back to something readable, trimming the trailing zeroes a rate rarely needs. */
private fun formatRate(micros: Long): String {
    val whole = micros / RATE_SCALE
    val fraction = (micros % RATE_SCALE).toString().padStart(6, '0').trimEnd('0').ifEmpty { "0" }
    return "$whole.$fraction"
}

/* -------------------------------------------------------------------- Lock */

const val PIN_LENGTH = 4

/**
 * The lock screen is the app's first impression more often than Home is. It gets the same water,
 * the same mark, and a keypad big enough to use without looking.
 *
 * The biometric prompt fires by itself on arrival when one is enrolled and enabled, because the
 * common case is a thumb already on the sensor. Dismissing it leaves the keypad, which always
 * works.
 */
@Composable
fun LockScreen(
    security: SecurityStore,
    onUnlock: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val haptics = LocalHapticFeedback.current

    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var lockedOutFor by remember { mutableLongStateOf(security.lockoutRemainingMillis()) }

    val biometric = rememberBiometricPrompt(
        title = "Unlock Money Manager",
        subtitle = "Or use your PIN",
        onSuccess = onUnlock,
    )
    val canBiometric = biometric != null && security.biometricEnabled

    // Tick the lockout down so the user can see it expire rather than guessing.
    LaunchedEffect(lockedOutFor) {
        if (lockedOutFor > 0) {
            kotlinx.coroutines.delay(1_000)
            lockedOutFor = security.lockoutRemainingMillis()
        }
    }

    LaunchedEffect(Unit) { if (canBiometric && lockedOutFor == 0L) biometric?.invoke() }

    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            if (security.verifyPin(pin)) {
                onUnlock()
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                wrong = true
                lockedOutFor = security.lockoutRemainingMillis()
                kotlinx.coroutines.delay(400)
                pin = ""
            }
        }
    }

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
            when {
                lockedOutFor > 0 -> "Too many attempts. Try again in ${lockedOutFor / 1000 + 1}s."
                wrong -> "That is not the PIN."
                else -> "Enter your PIN"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (wrong || lockedOutFor > 0) water.alert else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )

        Box(Modifier.padding(top = 26.dp)) { PinDots(pin.length, error = wrong) }

        Spacer(Modifier.height(32.dp))

        Keypad(
            enabled = lockedOutFor == 0L,
            biometric = if (canBiometric) biometric else null,
            onDigit = { wrong = false; pin = (pin + it).take(PIN_LENGTH) },
            onDelete = { pin = pin.dropLast(1) },
        )

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

/** Four dots that fill as digits land, and turn to the alert tone on a wrong PIN. */
@Composable
private fun PinDots(filled: Int, error: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(PIN_LENGTH) { i ->
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            error -> water.alert
                            i < filled -> scheme.primary
                            else -> scheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

/** The number pad, shared by the lock screen and by choosing a PIN. */
@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    biometric: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (biometric != null) "bio" else "", "0", "del"),
        ).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    LockKey(key, Modifier.weight(1f), enabled = enabled) {
                        when (key) {
                            "" -> Unit
                            "del" -> onDelete()
                            "bio" -> biometric?.invoke()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockKey(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    if (label.isEmpty()) {
        Box(modifier.height(62.dp))
        return
    }
    Box(
        modifier
            .height(62.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.surfaceContainer.copy(alpha = if (enabled) 0.85f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (enabled) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.5f)
        when (label) {
            "del" -> Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete last digit", tint = tint)
            "bio" -> Icon(Icons.Rounded.Fingerprint, "Unlock with fingerprint", tint = scheme.primary)
            else -> Text(label, style = MoneyType.medium, color = tint)
        }
    }
}
