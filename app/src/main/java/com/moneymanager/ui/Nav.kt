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
import androidx.compose.material.icons.rounded.ReceiptLong
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.moneymanager.data.LedgerState
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
import com.moneymanager.ui.screens.LedgerScreen
import com.moneymanager.ui.screens.LockScreen
import com.moneymanager.ui.screens.MoreScreen
import com.moneymanager.ui.screens.ReportsScreen
import com.moneymanager.ui.screens.RewardsScreen
import com.moneymanager.ui.screens.SecurityScreen
import com.moneymanager.ui.screens.SettingsScreen
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
    const val BUDGET_DETAIL = "budget/{id}"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "account/{id}"
    const val BILLS = "bills"
    const val GOALS = "goals"
    const val DEBT = "debt"
    const val CAPTURE = "capture"
    const val REWARDS = "rewards"
    const val CURRENCY = "currency"
    const val SECURITY = "security"
    const val SYNC = "sync"
    const val SETTINGS = "settings"

    fun txn(id: String) = "txn/$id"
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

data class Tab(val route: String, val label: String, val icon: ImageVector)

val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.LEDGER, "Ledger", Icons.Rounded.ReceiptLong),
    Tab(Routes.BUDGETS, "Budgets", Icons.Rounded.Savings),
    Tab(Routes.REPORTS, "Reports", Icons.Rounded.Insights),
    Tab(Routes.MORE, "More", Icons.Rounded.MoreHoriz),
)

@Composable
fun MoneyManagerApp() {
    val app = LocalContext.current.applicationContext as MoneyApp
    val vm: MoneyViewModel = viewModel(factory = MoneyViewModel.Factory(app.repository))
    val state by vm.state.collectAsState()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = tabs.any { it.route == route }
    val locked = route == Routes.LOCK
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val confirm: (String) -> Unit = remember(snackbar, scope) {
        { message ->
            scope.launch { snackbar.showSnackbar(message, withDismissAction = true) }
            Unit
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 640.dp

        // The ground is drawn once, behind everything, and only drifts on the screen that owns
        // the hero.
        WaterField(animate = route == Routes.HOME || locked)

        CompositionLocalProvider(LocalConfirm provides confirm) {
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
    modifier: Modifier,
) {
    val moving = motionEnabled()
    val tabRoutes = tabs.map { it.route }
    val enterSpec = tween<Float>(MoneyMotion.Enter, easing = MoneyMotion.EnterEasing)
    val exitSpec = tween<Float>(MoneyMotion.Exit, easing = MoneyMotion.ExitEasing)

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
        composable(Routes.LOCK) {
            LockScreen(onUnlock = { nav.navigate(Routes.HOME) { popUpTo(Routes.LOCK) { inclusive = true } } })
        }
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
            BudgetsScreen(state, onOpenBudget = { nav.navigate(Routes.budget(it)) })
        }
        composable(Routes.REPORTS) { ReportsScreen(state, onGo = nav::navigate) }
        composable(Routes.MORE) { MoreScreen(state, onGo = nav::navigate) }

        composable(Routes.TXN_NEW) {
            TransactionEditorScreen(
                state = state,
                onBack = nav::popBackStack,
                onSave = { merchant, categoryId, accountId, amountMinor, flow, note ->
                    vm.logTransaction(merchant, categoryId, accountId, amountMinor, flow, note)
                },
            )
        }
        composable(
            Routes.TXN_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            TransactionDetailScreen(
                state = state,
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = nav::popBackStack,
                onDelete = vm::deleteTransaction,
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
            )
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(state, onBack = nav::popBackStack, onOpenAccount = { nav.navigate(Routes.account(it)) })
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
        composable(Routes.BILLS) { BillsScreen(state, onBack = nav::popBackStack) }
        composable(Routes.GOALS) { GoalsScreen(state, onBack = nav::popBackStack, onGo = nav::navigate) }
        composable(Routes.DEBT) { DebtScreen(state, onBack = nav::popBackStack) }
        composable(Routes.CAPTURE) { CaptureScreen(onBack = nav::popBackStack) }
        composable(Routes.REWARDS) { RewardsScreen(state, onBack = nav::popBackStack) }
        composable(Routes.CURRENCY) { CurrencyScreen(onBack = nav::popBackStack) }
        composable(Routes.SECURITY) { SecurityScreen(onBack = nav::popBackStack, onPreviewLock = { nav.navigate(Routes.LOCK) }) }
        composable(Routes.SYNC) { SyncScreen(onBack = nav::popBackStack) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = nav::popBackStack,
                onGo = nav::navigate,
                onLoadDemo = { vm.loadDemoData(); nav.popBackStack() },
            )
        }
    }
}
