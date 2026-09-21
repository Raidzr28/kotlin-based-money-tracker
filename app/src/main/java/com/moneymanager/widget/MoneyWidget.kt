package com.moneymanager.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.moneymanager.MainActivity
import com.moneymanager.MoneyApp
import com.moneymanager.data.Flow
import com.moneymanager.data.LedgerState
import com.moneymanager.data.daysLeft
import com.moneymanager.data.dueLabel
import com.moneymanager.data.money
import com.moneymanager.data.today
import kotlinx.coroutines.flow.first
import kotlin.math.absoluteValue

/*
 * The home-screen widget.
 *
 * Three facts, which are the three the README asks for and the three worth a glance: what is left
 * to spend this month, what has gone today, and what lands next. Anything more is a screen, and
 * the app is one tap away.
 *
 * It reads the same LedgerState every screen reads, so a widget can never disagree with the app.
 * The colours are written out rather than pulled from the theme because a widget lives on someone
 * else's wallpaper, outside the app's composition, with no access to its MaterialTheme.
 */

private val Night = Color(0xFF071628)
private val Shelf = Color(0xFF17406C)
private val Spray = Color(0xFFDCEAF8)
private val Muted = Color(0xFFA9C4E0)
private val Surf = Color(0xFF6FB4FA)
private val Coral = Color(0xFFFF7A6B)

class MoneyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MoneyApp
        // One emission of the same state the app renders from. Heavier than a bespoke query, and
        // worth it: a widget that computes its own totals is a widget that eventually disagrees
        // with the ledger.
        val state = app.repository.state.first()
        provideContent { WidgetBody(state) }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(state: LedgerState) {
    val budgeted = state.budgets.isNotEmpty()
    val spentToday = state.transactions
        .filter { it.date == today && it.flow == Flow.Out }
        .sumOf { it.amountMinor.absoluteValue }
    val nextBill = state.bills.filter { it.due >= today }.minByOrNull { it.due }
    val overdue = state.bills.any { it.due < today }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Night)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            if (budgeted) "Safe to spend" else "Nothing budgeted",
            style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp),
        )

        Text(
            if (budgeted) money(state.safeToSpendMinor) else "Set a limit",
            style = TextStyle(
                color = ColorProvider(Spray),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )

        if (budgeted) {
            Text(
                "${money(state.perDayMinor)} a day · $daysLeft days left",
                style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp),
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Shelf)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "Today",
                    style = TextStyle(color = ColorProvider(Muted), fontSize = 10.sp),
                )
                Text(
                    if (spentToday == 0L) "nothing yet" else money(spentToday),
                    style = TextStyle(
                        color = ColorProvider(Spray),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            if (nextBill != null) {
                Column {
                    Text(
                        nextBill.name.take(18),
                        style = TextStyle(color = ColorProvider(Muted), fontSize = 10.sp),
                    )
                    Text(
                        "${money(nextBill.amountMinor)} · ${dueLabel(nextBill.due).lowercase()}",
                        style = TextStyle(
                            color = ColorProvider(if (overdue) Coral else Surf),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
    }
}

class MoneyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoneyWidget()
}

/**
 * Asks the launcher to place the widget, and says what happened.
 *
 * `requestPinAppWidget` is the only way an app can offer this from inside itself, and it needs
 * API 26 plus a launcher that supports pinning -- a good many do not, and they report it by
 * returning false rather than by throwing. Both cases get a sentence the user can act on
 * instead of a tap that appears to do nothing.
 */
fun requestPinWidget(context: Context): String {
    val manager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, MoneyWidgetReceiver::class.java)
    return when {
        !manager.isRequestPinAppWidgetSupported ->
            "This launcher cannot add widgets from inside an app. Long-press the home screen instead."
        manager.requestPinAppWidget(provider, null, null) ->
            "Check your home screen to place it."
        else ->
            "The launcher turned that down. Long-press the home screen and add it from there."
    }
}
