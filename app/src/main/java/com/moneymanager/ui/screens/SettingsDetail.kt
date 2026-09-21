package com.moneymanager.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moneymanager.data.Accounts
import com.moneymanager.data.AppPrefs
import com.moneymanager.data.Categories
import com.moneymanager.data.Category
import com.moneymanager.data.Template
import com.moneymanager.data.canNotify
import com.moneymanager.data.money
import com.moneymanager.ui.Chip
import com.moneymanager.ui.ChipRow
import com.moneymanager.ui.EmptyWater
import com.moneymanager.ui.LocalConfirm
import com.moneymanager.ui.NavRow
import com.moneymanager.ui.Pill
import com.moneymanager.ui.Plate
import com.moneymanager.ui.SectionHeading
import com.moneymanager.ui.icon
import com.moneymanager.ui.iconFor
import com.moneymanager.ui.iconKeys

/* ------------------------------------------------------------------- Notifications */

/**
 * The reminder settings, which the worker has been reading since day one with nothing able to
 * set them.
 *
 * Everything here writes straight through to [AppPrefs] and re-arms the daily check. A
 * preference behind a Save button is a preference that gets left half-changed, and the cost of
 * a wrong reminder time is a notification at the wrong hour rather than a wrong figure.
 */
@Composable
fun NotificationsScreen(
    prefs: AppPrefs,
    onBack: () -> Unit,
    onReschedule: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    // Reflects what will actually happen, not just what is stored: the permission can be
    // revoked from system settings, which leaves the preference on and the notifications off.
    var enabled by remember { mutableStateOf(prefs.remindersEnabled && canNotify(context)) }
    var lead by remember { mutableStateOf(prefs.remindDaysBefore) }
    var hour by remember { mutableStateOf(prefs.remindHour) }
    var streaks by remember { mutableStateOf(prefs.streakWarnings && canNotify(context)) }

    // Asked for at the toggle, never at startup, and the answer decides the preference. Turning
    // a switch on that the OS then refuses would leave a control that says it is on and is not.
    var wanted by remember { mutableStateOf<String?>(null) }
    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when (wanted) {
            "bills" -> {
                prefs.remindersEnabled = granted
                enabled = granted
            }
            "streaks" -> {
                prefs.streakWarnings = granted
                streaks = granted
            }
        }
        wanted = null
        onReschedule()
    }

    /** Turn a notification preference on only once the OS has actually agreed to it. */
    fun request(key: String, on: Boolean, apply: (Boolean) -> Unit) {
        if (!on || canNotify(context)) {
            apply(on)
            onReschedule()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wanted = key
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DetailScaffold(title = "Notifications", onBack = onBack) {
        item {
            SectionHeading(
                "Bills",
                caption = "One check a day, on this device. Nothing about a bill leaves the phone.",
            )
        }
        item {
            Plate {
                ToggleRow(
                    "Remind me before a bill is due",
                    "Asks for the notification permission the first time you turn it on.",
                    enabled,
                ) { on ->
                    request("bills", on) {
                        enabled = it
                        prefs.remindersEnabled = it
                    }
                }
            }
        }

        if (enabled) {
            item { SectionHeading("How much warning") }
            item {
                Plate {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Days before it is due",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                            ChipRow {
                                listOf(0, 1, 2, 3, 7).forEach { days ->
                                    Chip(
                                        when (days) {
                                            0 -> "On the day"
                                            1 -> "1 day"
                                            else -> "$days days"
                                        },
                                        selected = lead == days,
                                        onClick = {
                                            lead = days
                                            prefs.remindDaysBefore = days
                                            onReschedule()
                                        },
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Time of day",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                            ChipRow {
                                listOf(7, 9, 12, 18, 21).forEach { h ->
                                    Chip(
                                        "%02d:00".format(h),
                                        selected = hour == h,
                                        onClick = {
                                            hour = h
                                            prefs.remindHour = h
                                            onReschedule()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeading("Streaks") }
        item {
            Plate {
                ToggleRow(
                    "Warn me before a streak lapses",
                    "Late in the evening, and only on a day with nothing logged yet.",
                    streaks,
                ) { on ->
                    request("streaks", on) {
                        streaks = it
                        prefs.streakWarnings = it
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

/* ---------------------------------------------------------------------- Templates */

/**
 * Saved transaction shapes. Rent, salary, the weekly shop.
 *
 * Using one opens the ordinary editor already filled in rather than writing a row. The product
 * rule is that nothing posts without the user confirming, and a recurring amount is exactly the
 * figure that drifts between months -- rent goes up, a salary gets a bonus on it. A template
 * that posted itself would be the one row nobody ever checks.
 */
@Composable
fun TemplatesScreen(
    templates: List<Template>,
    onBack: () -> Unit,
    onUse: (Template) -> Unit,
    onSave: (Template) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<Template?>(null) }
    var adding by remember { mutableStateOf(false) }

    DetailScaffold(title = "Recurring templates", onBack = onBack) {
        item {
            SectionHeading(
                "Templates",
                caption = "One tap fills the editor in. You still confirm before anything is logged.",
            )
        }

        if (templates.isEmpty()) {
            item {
                EmptyWater(
                    "No templates yet",
                    "Rent, salary, the weekly shop -- anything you log with the same merchant " +
                        "and category every time. Save it once and it is one tap after that.",
                    actionLabel = "Add a template",
                    actionIcon = Icons.Rounded.Add,
                    onAction = { adding = true },
                )
            }
        } else {
            item {
                Plate {
                    Column {
                        templates.forEach { template ->
                            NavRow(
                                iconFor(Categories[template.categoryId].iconKey),
                                template.name,
                                subtitle = if (template.amountMinor == 0L) {
                                    template.merchant + " - asks for the amount"
                                } else {
                                    template.merchant + " - " + Accounts[template.accountId].name
                                },
                                trailing = if (template.amountMinor == 0L) null
                                else money(template.amountMinor, template.currency),
                            ) { onUse(template) }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("Add another", Icons.Rounded.Add, { adding = true })
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (adding || editing != null) {
        TemplateSheet(
            existing = editing,
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = {
                onSave(it)
                adding = false
                editing = null
            },
            onDelete = editing?.let { t ->
                {
                    onDelete(t.id)
                    editing = null
                }
            },
        )
    }
}

@Composable
private fun TemplateSheet(
    existing: Template?,
    onDismiss: () -> Unit,
    onSave: (Template) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var merchant by remember { mutableStateOf(existing?.merchant ?: "") }
    var digits by remember {
        mutableStateOf(
            existing?.amountMinor?.takeIf { it != 0L }?.let { minorToDigits(kotlin.math.abs(it)) } ?: ""
        )
    }
    var outgoing by remember { mutableStateOf((existing?.amountMinor ?: -1L) <= 0L) }
    var categoryId by remember {
        mutableStateOf(existing?.categoryId ?: Categories.all.firstOrNull()?.id ?: "")
    }
    var accountId by remember {
        mutableStateOf(existing?.accountId ?: Accounts.all.firstOrNull()?.id ?: "")
    }

    val amount = digitsToMinor(digits)
    val valid = name.isNotBlank() && merchant.isNotBlank() &&
        categoryId.isNotBlank() && accountId.isNotBlank()

    MoneySheet(
        title = if (existing == null) "New template" else "Edit template",
        onDismiss = onDismiss,
    ) {
        TextInput(name, { name = it }, label = "Call it", placeholder = "Rent, Salary, Weekly shop ...")
        TextInput(merchant, { merchant = it }, label = "Merchant", placeholder = "Who it goes to")
        AmountInput(
            digits,
            { digits = it },
            label = "Amount, or leave it empty to be asked each time",
            currency = Accounts.currencyOf(accountId),
        )
        ChipRow {
            Chip("Money out", selected = outgoing, onClick = { outgoing = true })
            Chip("Money in", selected = !outgoing, onClick = { outgoing = false })
        }
        ChipRow {
            Categories.all.forEach { category ->
                Chip(
                    category.label,
                    selected = categoryId == category.id,
                    leading = category.icon,
                    onClick = { categoryId = category.id },
                )
            }
        }
        ChipRow {
            Accounts.all.forEach { account ->
                Chip(
                    account.name,
                    selected = accountId == account.id,
                    onClick = { accountId = account.id },
                )
            }
        }
        Pill(
            if (!valid) "Name it and say who it goes to" else "Save template",
            Icons.Rounded.Check,
            {
                if (valid) {
                    onSave(
                        Template(
                            id = existing?.id ?: "",
                            name = name.trim(),
                            amountMinor = when {
                                amount == 0L -> 0L
                                outgoing -> -amount
                                else -> amount
                            },
                            merchant = merchant.trim(),
                            categoryId = categoryId,
                            accountId = accountId,
                        )
                    )
                }
            },
            emphasis = valid,
        )
        if (onDelete != null) {
            Pill("Delete this template", Icons.Rounded.Delete, onDelete)
        }
    }
}

/* --------------------------------------------------------------------- Categories */

/**
 * The category list, editable.
 *
 * Categories are archived rather than deleted. Every transaction ever filed under one still
 * names it by id, so removing the row would leave those rows pointing at nothing and quietly
 * relabel the user's own history as "Uncategorised" -- a past figure changed by a present
 * setting, which is the one thing this app does not do.
 */
@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (Category, Int) -> Unit,
    onArchive: (String) -> Unit,
) {
    val confirm = LocalConfirm.current
    var editing by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }

    val parents = categories.filter { it.parent == null }

    DetailScaffold(title = "Categories", onBack = onBack) {
        item {
            SectionHeading(
                categories.size.toString() + " categories",
                caption = "Tap one to rename it or change its icon. Sub-categories sit under their parent.",
            )
        }
        item {
            Plate {
                Column {
                    parents.forEach { parent ->
                        NavRow(parent.icon, parent.label) { editing = parent }
                        categories.filter { it.parent == parent.id }.forEach { child ->
                            NavRow(
                                child.icon,
                                child.label,
                                subtitle = "under " + parent.label,
                                modifier = Modifier.padding(start = 20.dp),
                            ) { editing = child }
                        }
                    }
                }
            }
        }
        item { Pill("Add a category", Icons.Rounded.Add, { adding = true }) }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (adding || editing != null) {
        CategorySheet(
            existing = editing,
            parents = parents,
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = { category ->
                onSave(category, editing?.let { categories.indexOf(it) } ?: categories.size)
                adding = false
                editing = null
            },
            onArchive = editing?.let { c ->
                {
                    onArchive(c.id)
                    confirm(c.label + " archived. Past transactions keep their category.")
                    editing = null
                }
            },
        )
    }
}

@Composable
private fun CategorySheet(
    existing: Category?,
    parents: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onArchive: (() -> Unit)?,
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: iconKeys.first()) }
    var parent by remember { mutableStateOf(existing?.parent) }
    val valid = label.isNotBlank()

    MoneySheet(
        title = if (existing == null) "New category" else existing.label,
        onDismiss = onDismiss,
    ) {
        TextInput(label, { label = it }, label = "Name", placeholder = "Coffee, Childcare, Tools ...")

        Text(
            "Icon",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipRow {
            iconKeys.forEach { key ->
                Chip(
                    "",
                    selected = iconKey == key,
                    leading = iconFor(key),
                    onClick = { iconKey = key },
                )
            }
        }

        Text(
            "Sits under",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipRow {
            Chip("Nothing, top level", selected = parent == null, onClick = { parent = null })
            parents.filter { it.id != existing?.id }.forEach { candidate ->
                Chip(
                    candidate.label,
                    selected = parent == candidate.id,
                    leading = candidate.icon,
                    onClick = { parent = candidate.id },
                )
            }
        }

        Pill(
            if (!valid) "Give it a name" else "Save",
            Icons.Rounded.Check,
            {
                if (valid) {
                    onSave(
                        Category(
                            id = existing?.id ?: "",
                            label = label.trim(),
                            iconKey = iconKey,
                            parent = parent,
                        )
                    )
                }
            },
            emphasis = valid,
        )
        if (onArchive != null) {
            Pill("Archive this category", Icons.Rounded.Undo, onArchive)
        }
    }
}
