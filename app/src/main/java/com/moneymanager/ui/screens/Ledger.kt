package com.moneymanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneymanager.data.LedgerState
import com.moneymanager.data.Accounts
import com.moneymanager.data.Categories
import com.moneymanager.data.Flow
import com.moneymanager.data.ForeignAmount
import com.moneymanager.data.RATE_SCALE
import com.moneymanager.data.RatesStore
import com.moneymanager.data.ReceiptGuess
import com.moneymanager.data.SUPPORTED_CURRENCIES
import com.moneymanager.data.convertMinor
import com.moneymanager.data.dayLabel
import com.moneymanager.data.Txn
import com.moneymanager.data.money
import com.moneymanager.data.thisMonth
import com.moneymanager.data.symbolOf
import com.moneymanager.ui.icon
import com.moneymanager.ui.accountIcon
import com.moneymanager.ui.Chip
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.EmptyWater
import com.moneymanager.ui.Flag
import com.moneymanager.ui.LocalConfirm
import com.moneymanager.ui.LedgerRow
import com.moneymanager.ui.Marker
import com.moneymanager.ui.MoneyText
import com.moneymanager.ui.MoneyTheme
import com.moneymanager.ui.MoneyType
import com.moneymanager.ui.motionEnabled
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.Routes
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.toneFor
import kotlin.math.absoluteValue
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/* ------------------------------------------------------------------ Ledger */

private enum class LedgerFilter(val label: String) {
    All("Everything"), Out("Money out"), In("Money in"), Transfers("Transfers")
}

@Composable
fun LedgerScreen(state: LedgerState, onOpenTxn: (String) -> Unit, onGo: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LedgerFilter.All) }
    var taggedOnly by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val matching = state.transactions.filter { txn ->
        val hitsQuery = query.isBlank() ||
            txn.merchant.contains(query, ignoreCase = true) ||
            Categories[txn.categoryId].label.contains(query, ignoreCase = true) ||
            txn.tags.any { it.contains(query, ignoreCase = true) }
        val hitsFilter = when (filter) {
            LedgerFilter.All -> true
            LedgerFilter.Out -> txn.flow == Flow.Out
            LedgerFilter.In -> txn.flow == Flow.In
            LedgerFilter.Transfers -> txn.flow == Flow.Transfer
        }
        hitsQuery && hitsFilter && (!taggedOnly || txn.tags.isNotEmpty())
    }
    val days = matching.groupBy { it.date }.toList().sortedByDescending { it.first }

    TabColumn(top = 12.dp, bottom = 96.dp) {
        item {
            Text(
                "Ledger",
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(scheme.surfaceContainer, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Merchant, category or tag",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; focus.clearFocus() }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear the search",
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Icon(
                        Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            ChipRow(Modifier.padding(top = 12.dp)) {
                LedgerFilter.entries.forEach { option ->
                    Chip(option.label, selected = filter == option, onClick = { filter = option })
                }
                Chip(
                    thisMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                    leading = Icons.Rounded.CalendarMonth,
                )
                Chip(
                    "Tagged",
                    selected = taggedOnly,
                    leading = Icons.Rounded.Sell,
                    onClick = { taggedOnly = !taggedOnly },
                )
            }
        }

        if (days.isEmpty()) {
            item {
                EmptyWater(
                    title = "Nothing matches",
                    body = if (taggedOnly && query.isBlank()) {
                        "Nothing this month carries a tag yet. Tags are added when you log or " +
                            "edit a transaction."
                    } else {
                        "No transaction in this month matches \"$query\". Clear the search, or " +
                            "widen the filter above."
                    },
                    modifier = Modifier.padding(top = 24.dp),
                    actionLabel = "Clear search",
                    actionIcon = Icons.Rounded.Check,
                    onAction = { query = ""; filter = LedgerFilter.All },
                )
            }
        }

        days.forEach { (date, rows) ->
            item(key = "head-$date") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        dayLabel(date),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    MoneyText(
                        rows.sumOf { it.amountMinor },
                        style = MoneyType.small,
                        color = scheme.onSurfaceVariant,
                        showSign = true,
                    )
                }
            }
            item(key = "plate-$date") {
                Plate {
                    Column {
                        rows.forEach { txn -> LedgerRow(txn, onClick = { onOpenTxn(txn.id) }) }
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Pill("Scan", Icons.Rounded.PhotoCamera, { onGo(Routes.CAPTURE) })
                Pill("Import", Icons.Rounded.Description, { onGo(Routes.CAPTURE) })
            }
        }
    }
}

/* ------------------------------------------------- Add / edit a transaction */

/**
 * The fastest path in the app, and the reason the keypad is here rather than the system one:
 * logging happens standing up, one-handed, seconds after paying. Amount first, everything else
 * pre-filled with the likeliest answer and one tap away from being changed.
 */
@Composable
fun TransactionEditorScreen(
    state: LedgerState,
    rates: RatesStore,
    editing: Txn? = null,
    /** What a receipt scan read, if the user arrived that way. */
    prefill: ReceiptGuess? = null,
    onPrefillUsed: () -> Unit = {},
    onBack: () -> Unit,
    onGo: (String) -> Unit = {},
    onSave: (
        id: String?,
        merchant: String,
        categoryId: String,
        accountId: String,
        amountMinor: Long,
        flow: Flow,
        note: String?,
        foreign: ForeignAmount?,
    ) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val confirm = LocalConfirm.current
    val haptics = LocalHapticFeedback.current
    // Keyed on the row being edited, so opening an existing transaction fills the form with it
    // and opening a new one starts clean.
    var digits by remember(editing) {
        mutableStateOf(editing?.amountMinor?.absoluteValue?.toString().orEmpty())
    }
    var flow by remember(editing) { mutableStateOf(editing?.flow ?: Flow.Out) }
    var merchant by remember(editing) { mutableStateOf(editing?.merchant.orEmpty()) }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }

    // Left empty until the user picks something, so the first category and account to arrive from
    // the database become the defaults without ever overwriting a choice already made.
    var pickedCategory by remember(editing) { mutableStateOf(editing?.categoryId.orEmpty()) }
    var pickedAccount by remember(editing) { mutableStateOf(editing?.accountId.orEmpty()) }
    val categoryId = pickedCategory.ifEmpty { state.categories.firstOrNull()?.id.orEmpty() }
    val accountId = pickedAccount.ifEmpty { state.accounts.firstOrNull()?.id.orEmpty() }

    // Filled in once, then cleared, so returning to this screen later starts clean and an
    // edited figure is never overwritten by the scan that started it.
    LaunchedEffect(prefill) {
        prefill?.let { guess ->
            guess.totalMinor?.let { digits = it.toString() }
            guess.merchant?.let { merchant = it }
            onPrefillUsed()
        }
    }

    val minor = digits.toLongOrNull() ?: 0L

    // Multi-currency, only when it is switched on. Everyone else never sees a currency control.
    val multiCurrency = rates.multiCurrencyEnabled
    val accountCurrency = Accounts[accountId].currency
    var pickedCurrency by remember(accountId, editing) {
        mutableStateOf(editing?.originalCurrency ?: accountCurrency)
    }
    val table = remember(multiCurrency) { rates.cached() }
    val foreignEntry = multiCurrency && pickedCurrency != accountCurrency

    // What lands in the ledger is always the account's own currency.
    val convertedMinor = if (!foreignEntry) minor else {
        table?.let { convertMinor(minor, pickedCurrency, accountCurrency, it) }
    }
    val rateMicros = table?.let { t ->
        val from = t.microsFor(pickedCurrency)
        val to = t.microsFor(accountCurrency)
        if (from == null || to == null || from == 0L) null else to * RATE_SCALE / from
    }
    val missingRate = foreignEntry && (convertedMinor == null || rateMicros == null)

    val signed = if (flow == Flow.In) (convertedMinor ?: 0L) else -(convertedMinor ?: 0L)

    // Feedback, not decoration: the figure answers the key you just pressed, so a hurried tap in
    // a queue is confirmed without looking closely. Material's own grammar, not a second gesture.
    val moving = motionEnabled()
    val pop = remember { Animatable(1f) }
    LaunchedEffect(digits) {
        if (digits.isNotEmpty() && moving) {
            pop.snapTo(1.05f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f))
        }
    }

    DetailScaffold(title = if (editing == null) "Log" else "Edit", onBack = onBack) {
        item {
            Plate(Modifier.padding(top = 4.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            symbolOf("USD"),
                            style = MoneyType.large,
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            if (digits.isEmpty()) "0.00" else formatDigits(digits),
                            style = MoneyType.hero,
                            color = if (digits.isEmpty()) scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else toneFor(flow),
                            modifier = Modifier.graphicsLayer {
                                scaleX = pop.value
                                scaleY = pop.value
                            },
                        )
                    }
                    Text(
                        when (flow) {
                            Flow.Out -> "out of ${Accounts[accountId].name}"
                            Flow.In -> "into ${Accounts[accountId].name}"
                            Flow.Transfer -> "between accounts"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            ChipRow(Modifier.padding(top = 14.dp)) {
                Chip("Money out", selected = flow == Flow.Out, onClick = { flow = Flow.Out })
                Chip("Money in", selected = flow == Flow.In, onClick = { flow = Flow.In })
                Chip("Transfer", selected = flow == Flow.Transfer, onClick = { flow = Flow.Transfer })
            }
        }

        item { SectionHeading("Category") }
        item {
            ChipRow {
                Categories.all.forEach { category ->
                    Chip(
                        category.label,
                        selected = categoryId == category.id,
                        leading = category.icon,
                        onClick = { pickedCategory = category.id },
                    )
                }
            }
        }

        item { SectionHeading("Account") }
        item {
            ChipRow {
                Accounts.all.forEach { account ->
                    Chip(
                        account.name,
                        selected = accountId == account.id,
                        leading = accountIcon(account.kind),
                        onClick = { pickedAccount = account.id },
                    )
                }
            }
        }

        if (multiCurrency) {
            item { SectionHeading("Currency", caption = "What you actually paid in") }
            item {
                ChipRow {
                    (listOf(accountCurrency) + SUPPORTED_CURRENCIES.filter { it != accountCurrency })
                        .forEach { code ->
                            Chip(
                                code,
                                selected = pickedCurrency == code,
                                onClick = { pickedCurrency = code },
                            )
                        }
                }
            }
            if (foreignEntry) {
                item {
                    Plate(Modifier.padding(top = 12.dp), depth = 1) {
                        if (missingRate) {
                            Flag(
                                Icons.Rounded.Check,
                                "No rate for $pickedCurrency. Set one in Currencies.",
                                water.alert,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Goes in as ${money(convertedMinor ?: 0L, accountCurrency)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    "Converted once, now, and kept with the transaction. Later rate " +
                                        "changes will not rewrite it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeading("Merchant") }
        item {
            Plate(padding = 16.dp) {
                Box {
                    if (merchant.isEmpty()) {
                        Text(
                            "Where did it go?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item { SectionHeading("Note") }
        item {
            Plate(padding = 16.dp) {
                Box {
                    if (note.isEmpty()) {
                        Text(
                            "What was it for?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                Pill("Photograph a receipt", Icons.Rounded.PhotoCamera, { onGo(Routes.SCAN) })
            }
        }

        item { SectionHeading("Amount") }
        item { Keypad(onDigit = { digits = (digits + it).take(9) }, onDelete = { digits = digits.dropLast(1) }) }

        item {
            Box(Modifier.padding(top = 18.dp)) {
                Pill(
                    label = when {
                        missingRate -> "No rate for $pickedCurrency"
                        minor == 0L -> "Enter an amount"
                        editing != null -> "Save changes"
                        else -> "Log ${money(signed, showSign = true)}"
                    },
                    icon = Icons.Rounded.Check,
                    onClick = {
                        // The one moment in the app where money is committed. It gets the one
                        // piece of haptic feedback, so the confirmation reaches the hand as well
                        // as the eye.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(
                            editing?.id, merchant, categoryId, accountId,
                            convertedMinor ?: minor, flow, note.ifBlank { null },
                            if (foreignEntry && rateMicros != null) {
                                ForeignAmount(minor, pickedCurrency, rateMicros)
                            } else null,
                        )
                        confirm(
                            if (editing != null) "Updated ${money(signed, showSign = true)}"
                            else "Logged ${money(signed, showSign = true)} to ${Accounts[accountId].name}"
                        )
                        onBack()
                    },
                    emphasis = minor > 0L,
                    enabled = minor > 0L && !missingRate &&
                        categoryId.isNotEmpty() && accountId.isNotEmpty(),
                    container = if (minor == 0L) scheme.surfaceContainer else null,
                    contentColor = if (minor == 0L) scheme.onSurfaceVariant else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Flag(
                Icons.Rounded.Check,
                "Saved on this device only",
                water.income,
                Modifier.padding(top = 14.dp),
            )
        }
    }
}

private fun formatDigits(digits: String): String {
    val padded = digits.padStart(3, '0')
    val whole = padded.dropLast(2).trimStart('0').ifEmpty { "0" }
    val grouped = buildString {
        whole.forEachIndexed { i, c ->
            if (i > 0 && (whole.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
    return "$grouped.${padded.takeLast(2)}"
}

/**
 * A keypad sized for a thumb, not for a desk. Keys are 64dp tall with 10dp between them, which
 * clears the 48dp floor with room for a hurried tap.
 */
@Composable
private fun Keypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("00", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Key(key, Modifier.weight(1f)) {
                        if (key == "⌫") onDelete() else onDigit(key)
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .height(64.dp)
            .background(scheme.surfaceContainer, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "⌫") {
            Icon(
                Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Delete last digit",
                tint = scheme.onSurfaceVariant,
            )
        } else {
            Text(label, style = MoneyType.medium, color = scheme.onSurface)
        }
    }
}

/* --------------------------------------------------------- Transaction detail */

@Composable
fun TransactionDetailScreen(
    state: LedgerState,
    id: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (Txn) -> Unit,
    onDuplicate: (Txn) -> Unit,
    onGo: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val txn = state.allTransactions.firstOrNull { it.id == id }

    if (txn == null) {
        DetailScaffold(title = "Transaction", onBack = onBack) {
            item {
                EmptyWater(
                    "That transaction is gone",
                    "It may have been deleted from another device. The ledger still has everything else.",
                )
            }
        }
        return
    }

    val category = Categories[txn.categoryId]
    val account = Accounts[txn.accountId]

    DetailScaffold(title = category.label, onBack = onBack) {
        item {
            Plate {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Marker(category.icon, size = 52.dp)
                    MoneyText(
                        txn.amountMinor,
                        style = MoneyType.hero,
                        color = toneFor(txn.flow),
                        showSign = txn.flow == Flow.In,
                        currency = txn.currency,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(txn.merchant, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                    Text(
                        "${dayLabel(txn.date)} · %02d:%02d".format(txn.time.hour, txn.time.minute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
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
                Pill("Edit", Icons.Rounded.Edit, { onEdit(txn.id) }, emphasis = true)
                Pill("Duplicate", Icons.Rounded.ContentCopy, { onDuplicate(txn); onBack() })
                Pill(
                    "Delete", Icons.Rounded.Delete,
                    { onDelete(txn); onBack() },
                    contentColor = water.alert,
                )
            }
        }

        item { SectionHeading("Details") }
        item {
            Plate {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Fact("Account", account.name)
                    Fact("Category", category.label)
                    txn.note?.let { Fact("Note", it) }
                    txn.originalMinor?.let {
                        Fact(
                            "Original amount",
                            money(it, txn.originalCurrency ?: txn.currency),
                        )
                    }
                    if (txn.tags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Tags",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                            )
                            ChipRow { txn.tags.forEach { Chip(it, leading = Icons.Rounded.Sell) } }
                        }
                    }
                }
            }
        }

        if (txn.splits.isNotEmpty()) {
            item { SectionHeading("Split across categories") }
            item {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        txn.splits.forEach { split ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Marker(Categories[split.categoryId].icon, size = 34.dp)
                                Text(
                                    Categories[split.categoryId].label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                )
                                MoneyText(
                                    split.amountMinor.absoluteValue,
                                    style = MoneyType.row,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeading("Receipt") }
        item {
            if (txn.hasReceipt) {
                Plate(padding = 0.dp) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.45f)
                            .background(scheme.surfaceContainerLowest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                            )
                            Text(
                                "Receipt photo",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            } else {
                EmptyWater(
                    "No receipt attached",
                    "Photograph it and the amount, merchant and date get read off it on-device. " +
                        "Nothing is uploaded.",
                    actionLabel = "Attach a photo",
                    actionIcon = Icons.Rounded.PhotoCamera,
                    onAction = { onGo(Routes.SCAN) },
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
internal fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp, top = 2.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
