package com.moneymanager.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneymanager.MoneyApp
import com.moneymanager.MoneyViewModel
import com.moneymanager.data.AppPrefs
import com.moneymanager.data.Accounts
import com.moneymanager.data.LedgerState
import com.moneymanager.data.money
import com.moneymanager.data.RatesStore
import com.moneymanager.data.SecurityStore
import com.moneymanager.ui.screens.AccountDetailScreen
import com.moneymanager.ui.screens.AccountsScreen
import com.moneymanager.ui.screens.BillsScreen
import com.moneymanager.ui.screens.BudgetDetailScreen
import com.moneymanager.ui.screens.BudgetsScreen
import com.moneymanager.ui.screens.CaptureScreen
import com.moneymanager.ui.screens.CurrencyScreen
import com.moneymanager.ui.screens.DebtScreen
import com.moneymanager.ui.screens.GoalsScreen
import com.moneymanager.ui.screens.HomeScreen
import com.moneymanager.ui.screens.ImportScreen
import com.moneymanager.ui.screens.LedgerScreen
import com.moneymanager.ui.screens.LockScreen
import com.moneymanager.ui.screens.MoreScreen
import com.moneymanager.ui.screens.ReportsScreen
import com.moneymanager.ui.screens.ScanScreen
import com.moneymanager.ui.screens.RewardsScreen
import com.moneymanager.ui.screens.SecurityScreen
import com.moneymanager.ui.screens.SettingsScreen
import com.moneymanager.AppearanceState
import com.moneymanager.data.ledgerCsv
import com.moneymanager.data.scheduleReminders
import com.moneymanager.data.scheduleStreakWarnings
import com.moneymanager.ui.screens.CategoriesScreen
import com.moneymanager.ui.screens.NotificationsScreen
import com.moneymanager.ui.screens.TemplatesScreen
import com.moneymanager.widget.requestPinWidget
import com.moneymanager.ui.screens.SyncScreen
import com.moneymanager.ui.screens.TransactionDetailScreen
import com.moneymanager.ui.screens.TransactionEditorScreen
import kotlinx.coroutines.launch

/*
 * Five destinations in the navigation bar and one floating action, the way Material 3 lays a
 * phone app out. The references all put a circular + inside the bar; that is an iOS tab-bar
 * habit, and a fluent Android user reads it as a broken bar. The primary action gets a real
 * FAB, and the bar keeps its five equal destinations.
 *
 * Everything the README lists is reachable: the four busiest areas are tabs, the rest live one
 * level under More, which is a destination in its own right rather than an overflow menu.
 */

object Routes {
    const val LOCK = "lock"
    const val HOME = "home"
    const val LEDGER = "ledger"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val MORE = "more"

    const val TXN_NEW = "txn/new"
    const val TXN_DETAIL = "txn/{id}"
    const val TXN_EDIT = "txn/{id}/edit"
    const val BUDGET_DETAIL = "budget/{id}"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "account/{id}"
    const val BILLS = "bills"
    const val GOALS = "goals"
    const val DEBT = "debt"
    const val CAPTURE = "capture"
    const val IMPORT = "import"
    const val SCAN = "scan"
    const val REWARDS = "rewards"
    const val CURRENCY = "currency"
    const val SECURITY = "security"
    const val SYNC = "sync"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "settings/notifications"
    const val TEMPLATES = "settings/templates"
    const val CATEGORIES = "settings/categories"

    fun txn(id: String) = "txn/$id"
    fun txnEdit(id: String) = "txn/$id/edit"
    fun budget(id: String) = "budget/$id"
    fun account(id: String) = "account/$id"
}

/**
 * Confirming a committed action, from anywhere.
 *
 * The host and the coroutine scope both belong to the app shell, not to the screen that calls
 * this. A screen that logs a transaction pops itself off the back stack in the same click, and a
 * scope owned by that screen would be cancelled before the snackbar ever showed.
 */
val LocalConfirm = staticCompositionLocalOf<(String) -> Unit> { {} }

/**
 * Confirming an action that can still be taken back.
 *
 * Deleting a transaction used to be one tap with nothing behind it. A dialog asking "are you
 * sure" before every delete trains people to tap through it; a snackbar that undoes the delete
 * costs one tap only when the delete was a mistake. Kept separate from [LocalConfirm] so a
 * screen has to mean it: passing an undo is a promise the action is reversible.
 */
val LocalUndo = staticCompositionLocalOf<(String, () -> Unit) -> Unit> { { _, _ -> } }

/*
 * What to say when a write is refused for want of a rate. Not an error: no network is the normal
 * condition this app is built for, and the fix is one tap away on the Currencies screen.
 */
private const val noRate = "No exchange rate for that yet. Update rates in Currencies."

private fun noRateBetween(fromAccountId: String, toAccountId: String): String {
    val from = Accounts.currencyOf(fromAccountId)
    val to = Accounts.currencyOf(toAccountId)
    return "No rate between $from and $to yet. Update rates in Currencies."
}

data class Tab(val route: String, val label: String, val icon: ImageVector)

val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.LEDGER, "Ledger", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab(Routes.BUDGETS, "Budgets", Icons.Rounded.Savings),
    Tab(Routes.REPORTS, "Reports", Icons.Rounded.Insights),
    Tab(Routes.MORE, "More", Icons.Rounded.MoreHoriz),
)

@Composable
fun MoneyManagerApp() {
    val app = LocalContext.current.applicationContext as MoneyApp
    val vm: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory(app.repository, app))
    val state by vm.state.collectAsState()
    val security = app.security
    val prefs = app.prefs
    val rates = app.rates
    val nav = rememberNavController()

    // The gate, not a destination. A lock that lives in the back stack can be navigated around;
    // this one wraps everything and nothing renders behind it.
    var locked by rememberSaveable { mutableStateOf(security.lockEnabled) }

    // Re-lock whenever the app leaves the foreground, which is what "auto-lock: immediately"
    // actually means. ON_STOP rather than ON_PAUSE, so a permission dialog does not lock you out
    // of the screen that raised it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, security) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && security.lockEnabled) locked = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = tabs.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val confirm: (String) -> Unit = remember(snackbar, scope) {
        { message ->
            scope.launch { snackbar.showSnackbar(message, withDismissAction = true) }
            Unit
        }
    }
    val undoable: (String, () -> Unit) -> Unit = remember(snackbar, scope) {
        { message, undo ->
            scope.launch {
                // Only one of these can be on screen at a time, so an undo that arrives while an
                // older one is still showing replaces it rather than queueing behind it.
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showSnackbar(message, actionLabel = "Undo") == SnackbarResult.ActionPerformed) {
                    undo()
                }
            }
            Unit
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 640.dp

        // The ground is drawn once, behind everything, and only drifts on the screen that owns
        // the hero.
        WaterField(animate = route == Routes.HOME || locked)

        if (locked) {
            LockScreen(security = security, onUnlock = { locked = false })
            return@BoxWithConstraints
        }

        CompositionLocalProvider(LocalConfirm provides confirm, LocalUndo provides undoable) {
            Row(Modifier.fillMaxSize()) {
                if (wide && onTab) MoneyRail(nav, route)
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    // The top inset is deliberately not applied here. Content begins at the very
                    // top of the screen so the Home hero can run under the status bar, and each
                    // screen re-applies the status-bar inset to its own content: TabColumn via
                    // insetTop, DetailScaffold through its own top app bar, the lock screen
                    // through windowInsetsPadding. Bottom and horizontal stay exactly as they
                    // were, which is what keeps the FAB, the snackbar and the bar in place.
                    contentWindowInsets = WindowInsets.systemBars
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    bottomBar = { if (onTab && !wide) MoneyNavBar(nav, route) },
                    snackbarHost = { SnackbarHost(snackbar) },
                    floatingActionButton = { PrimaryAction(route, nav) },
                ) { inner ->
                    // Consumed, so a detail screen's own Scaffold does not apply the bottom and
                    // side insets a second time. The top was never applied, so it stays available
                    // to whichever screen wants it.
                    MoneyNavHost(
                        nav = nav,
                        vm = vm,
                        state = state,
                        security = security,
                        prefs = prefs,
                        appearance = app.appearance,
                        rates = rates,
                        onLockNow = { locked = true },
                        modifier = Modifier
                            .padding(inner)
                            .consumeWindowInsets(inner),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyNavBar(nav: NavHostController, current: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { nav.toTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/** Tablets and unfolded foldables get a rail, never a stretched phone bar. */
@Composable
private fun MoneyRail(nav: NavHostController, current: String?) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
    ) {
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = current == tab.route,
                onClick = { nav.toTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

private fun NavHostController.toTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * One action, on the two screens where it is the point: log a spend. The other add-flows are not
 * built yet, and a FAB that does nothing is worse than no FAB, so those screens have none rather
 * than a decoration.
 */
@Composable
private fun PrimaryAction(route: String?, nav: NavHostController) {
    when (route) {
        Routes.HOME -> ExtendedFloatingActionButton(
            onClick = { nav.navigate(Routes.TXN_NEW) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Log") },
        )

        Routes.LEDGER -> FloatingActionButton(
            onClick = { nav.navigate(Routes.TXN_NEW) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(Icons.Rounded.Add, contentDescription = "Log a transaction") }

        else -> Unit
    }
}

@Composable
private fun MoneyNavHost(
    nav: NavHostController,
    vm: MoneyViewModel,
    state: LedgerState,
    security: SecurityStore,
    prefs: AppPrefs,
    appearance: AppearanceState,
    rates: RatesStore,
    onLockNow: () -> Unit,
    modifier: Modifier,
) {
    val moving = motionEnabled()
    val tabRoutes = tabs.map { it.route }
    val enterSpec = tween<Float>(MoneyMotion.Enter, easing = MoneyMotion.EnterEasing)
    val exitSpec = tween<Float>(MoneyMotion.Exit, easing = MoneyMotion.ExitEasing)

    val confirm = LocalConfirm.current
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = {
            when {
                !moving -> EnterTransition.None
                targetState.destination.route in tabRoutes -> fadeIn(enterSpec)
                else -> slideInHorizontally(
                    tween(MoneyMotion.Push, easing = MoneyMotion.EnterEasing)
                ) { it / 5 } + fadeIn(enterSpec)
            }
        },
        exitTransition = { if (!moving) ExitTransition.None else fadeOut(exitSpec) },
        popEnterTransition = { if (!moving) EnterTransition.None else fadeIn(enterSpec) },
        popExitTransition = {
            if (!moving) ExitTransition.None
            else slideOutHorizontally(
                tween(MoneyMotion.Push, easing = MoneyMotion.ExitEasing)
            ) { it / 5 } + fadeOut(exitSpec)
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                state = state,
                onOpenTxn = { nav.navigate(Routes.txn(it)) },
                onOpenBudget = { nav.navigate(Routes.budget(it)) },
                onGo = nav::navigate,
            )
        }
        composable(Routes.LEDGER) {
            LedgerScreen(state, onOpenTxn = { nav.navigate(Routes.txn(it)) }, onGo = nav::navigate)
        }
        composable(Routes.BUDGETS) {
            BudgetsScreen(
                state = state,
                onOpenBudget = { nav.navigate(Routes.budget(it)) },
                onSetBudget = vm::setBudget,
                onClearBudget = vm::clearBudget,
            )
        }
        composable(Routes.REPORTS) { ReportsScreen(state, onGo = nav::navigate) }
        composable(Routes.MORE) { MoreScreen(state, onGo = nav::navigate) }

        composable(Routes.TXN_NEW) {
            val pending by vm.pendingScan.collectAsState()
            val pendingTemplate by vm.pendingTemplate.collectAsState()
            TransactionEditorScreen(
                state = state,
                rates = rates,
                prefill = pending,
                onPrefillUsed = vm::consumeScan,
                template = pendingTemplate,
                onTemplateUsed = vm::consumeTemplate,
                onBack = nav::popBackStack,
                onGo = nav::navigate,
                onSave = { id, merchant, categoryId, accountId, amountMinor, flow, note, foreign ->
                    vm.logTransaction(
                        id = id, merchant = merchant, categoryId = categoryId,
                        accountId = accountId, amountMinor = amountMinor, flow = flow,
                        note = note, foreign = foreign,
                    )
                },
            )
        }
        composable(
            Routes.TXN_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val editingId = entry.arguments?.getString("id").orEmpty()
            TransactionEditorScreen(
                state = state,
                rates = rates,
                editing = state.allTransactions.firstOrNull { it.id == editingId },
                onBack = nav::popBackStack,
                onGo = nav::navigate,
                onSave = { id, merchant, categoryId, accountId, amountMinor, flow, note, foreign ->
                    vm.logTransaction(
                        id = id, merchant = merchant, categoryId = categoryId,
                        accountId = accountId, amountMinor = amountMinor, flow = flow,
                        note = note, foreign = foreign,
                    )
                },
            )
        }
        composable(
            Routes.TXN_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val undo = LocalUndo.current
            TransactionDetailScreen(
                state = state,
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = nav::popBackStack,
                onEdit = { nav.navigate(Routes.txnEdit(it)) },
                onGo = nav::navigate,
                onDelete = { txn ->
                    vm.deleteTransaction(txn.id)
                    // Restored under its own id, date and time, so undo puts the row back where
                    // it was rather than logging a lookalike at the top of today.
                    undo("Deleted ${txn.merchant}") {
                        vm.logTransaction(
                            id = txn.id,
                            merchant = txn.merchant,
                            categoryId = txn.categoryId,
                            accountId = txn.accountId,
                            amountMinor = txn.amountMinor,
                            flow = txn.flow,
                            note = txn.note,
                            tags = txn.tags,
                            splits = txn.splits,
                            date = txn.date,
                            time = txn.time,
                        )
                    }
                },
                onDuplicate = { txn ->
                    // A copy is a new row dated now: the point of duplicating is the weekly shop
                    // you just did again, not a second record of the old one.
                    vm.logTransaction(
                        merchant = txn.merchant,
                        categoryId = txn.categoryId,
                        accountId = txn.accountId,
                        amountMinor = txn.amountMinor,
                        flow = txn.flow,
                        note = txn.note,
                        tags = txn.tags,
                    )
                    confirm("Copied ${txn.merchant} to today")
                },
            )
        }
        composable(
            Routes.BUDGET_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            BudgetDetailScreen(
                state = state,
                categoryId = entry.arguments?.getString("id").orEmpty(),
                onBack = nav::popBackStack,
                onOpenTxn = { nav.navigate(Routes.txn(it)) },
                onSetBudget = vm::setBudget,
                onClearBudget = vm::clearBudget,
            )
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(
                state = state,
                onBack = nav::popBackStack,
                onOpenAccount = { nav.navigate(Routes.account(it)) },
                onSaveAccount = { name, kind, opening, limit, currency ->
                    vm.saveAccount(name, kind, opening, limit, currency = currency)
                },
                onTransfer = { from, to, amount ->
                    vm.transfer(from, to, amount) { ok ->
                        confirm(if (ok) "Transferred" else noRateBetween(from, to))
                    }
                },
                onSaveAsset = vm::saveAsset,
                onDeleteAsset = vm::deleteAsset,
            )
        }
        composable(
            Routes.ACCOUNT_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            AccountDetailScreen(
                state = state,
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = nav::popBackStack,
                onOpenTxn = { nav.navigate(Routes.txn(it)) },
            )
        }
        composable(Routes.BILLS) {
            BillsScreen(
                state = state,
                prefs = prefs,
                onBack = nav::popBackStack,
                onSaveBill = vm::saveBill,
                onPayBill = { id ->
                    val bill = state.bills.firstOrNull { it.id == id }
                    vm.payBill(id)
                    confirm(
                        if (bill == null) "Marked paid"
                        else "Logged ${money(bill.amountMinor, bill.currency)} for ${bill.name}"
                    )
                },
            )
        }
        composable(Routes.GOALS) {
            GoalsScreen(
                state = state,
                onBack = nav::popBackStack,
                onGo = nav::navigate,
                onSaveGoal = vm::saveGoal,
                onContribute = { goalId, from, amount ->
                    vm.contributeToGoal(goalId, from, amount) { ok ->
                        confirm(if (ok) "Added to the goal" else noRate)
                    }
                },
            )
        }
        composable(Routes.DEBT) {
            DebtScreen(
                state = state,
                onBack = nav::popBackStack,
                onSaveDebt = vm::saveDebt,
                onPayDebt = { debtId, from, amount ->
                    vm.payDebt(debtId, from, amount) { ok ->
                        confirm(if (ok) "Payment recorded" else noRate)
                    }
                },
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(onBack = nav::popBackStack, onGo = nav::navigate)
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = nav::popBackStack,
                onUse = { guess, _ ->
                    vm.offerScan(guess)
                    nav.popBackStack()
                    nav.navigate(Routes.TXN_NEW)
                },
            )
        }
        composable(Routes.IMPORT) {
            val confirm = LocalConfirm.current
            ImportScreen(
                state = state,
                onBack = nav::popBackStack,
                onImport = { candidates, accountId, categoryId ->
                    vm.importStatement(candidates, accountId, categoryId) { n ->
                        confirm("Imported $n transactions")
                    }
                    nav.popBackStack()
                },
            )
        }
        composable(Routes.REWARDS) {
            val confirm = LocalConfirm.current
            RewardsScreen(
                state = state,
                prefs = prefs,
                onBack = nav::popBackStack,
                onSweep = { goalId, amount ->
                    // Straight through contributeToGoal, so a sweep is an ordinary goal
                    // contribution and shows up in the ledger like any other.
                    vm.contributeToGoal(goalId, "", amount) { ok ->
                        confirm(if (ok) "Swept into your goal" else noRate)
                    }
                },
            )
        }
        composable(Routes.CURRENCY) { CurrencyScreen(rates, onBack = nav::popBackStack) }
        composable(Routes.SECURITY) {
            SecurityScreen(
                security = security,
                onBack = nav::popBackStack,
                onLockNow = { nav.popBackStack(); onLockNow() },
            )
        }
        composable(Routes.SYNC) { SyncScreen(onBack = nav::popBackStack) }
        composable(Routes.NOTIFICATIONS) {
            val context = LocalContext.current
            NotificationsScreen(
                prefs = prefs,
                onBack = nav::popBackStack,
                // Re-armed on every change rather than on leaving the screen: WorkManager
                // replaces the existing request, and a user who changes the hour and then
                // force-stops the app should still get the reminder at the hour they picked.
                onReschedule = {
                    scheduleReminders(context, prefs)
                    scheduleStreakWarnings(context, prefs)
                },
            )
        }
        composable(Routes.TEMPLATES) {
            val templates by vm.templates.collectAsState()
            TemplatesScreen(
                templates = templates,
                onBack = nav::popBackStack,
                onUse = { template ->
                    // Straight into the ordinary editor, pre-filled. Nothing is written here.
                    vm.offerTemplate(template)
                    nav.navigate(Routes.TXN_NEW)
                },
                onSave = vm::saveTemplate,
                onDelete = vm::deleteTemplate,
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                categories = state.categories,
                onBack = nav::popBackStack,
                onSave = vm::saveCategory,
                onArchive = vm::archiveCategory,
            )
        }
        composable(Routes.SETTINGS) {
            val context = LocalContext.current
            val confirm = LocalConfirm.current
            val templates by vm.templates.collectAsState()
            val exportAll = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/csv")
            ) { uri ->
                if (uri != null) {
                    val rows = state.allTransactions
                    val written = runCatching {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(ledgerCsv(rows).toByteArray())
                        } ?: error("no stream")
                    }
                    confirm(
                        if (written.isSuccess) "Exported " + rows.size + " transactions"
                        else "Could not write that file. Try another folder."
                    )
                }
            }
            SettingsScreen(
                prefs = prefs,
                appearance = appearance,
                templateCount = templates.size,
                categoryCount = state.categories.size,
                onBack = nav::popBackStack,
                onGo = nav::navigate,
                onLoadDemo = { vm.loadDemoData(); nav.popBackStack() },
                onExport = { exportAll.launch("money-manager-export.csv") },
                onPinWidget = { confirm(requestPinWidget(context)) },
                onDeleteEverything = {
                    vm.deleteEverything { }
                    confirm("Everything erased. Categories and a cash account are back.")
                },
            )
        }
    }
}
