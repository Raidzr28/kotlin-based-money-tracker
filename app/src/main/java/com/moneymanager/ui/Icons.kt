package com.moneymanager.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector
import com.moneymanager.data.AccountKind

/*
 * The one place a stored key becomes a drawn glyph.
 *
 * The database holds "restaurant", never a vector: a drawable is a rendering decision and has no
 * business being persisted alongside someone's spending. An unknown key falls back rather than
 * throwing, so a category written by a newer version of the app still renders on an older one.
 */
private val byKey: Map<String, ImageVector> = mapOf(
    "restaurant" to Icons.Rounded.Restaurant,
    "groceries" to Icons.Rounded.ShoppingBag,
    "transport" to Icons.Rounded.DirectionsCar,
    "home" to Icons.Rounded.Home,
    "utilities" to Icons.Rounded.Bolt,
    "health" to Icons.Rounded.LocalHospital,
    "fun" to Icons.Rounded.SportsEsports,
    "subs" to Icons.Rounded.Subscriptions,
    "travel" to Icons.Rounded.Flight,
    "pets" to Icons.Rounded.Pets,
    "income" to Icons.Rounded.Payments,
    "savings" to Icons.Rounded.Savings,
)

/** Every icon a user can pick from, in the order the picker offers them. */
val iconKeys: List<String> = byKey.keys.toList()

fun iconFor(key: String): ImageVector = byKey[key] ?: Icons.Rounded.Category

/** Reads exactly like the old model property, so no call site had to change. */
val com.moneymanager.data.Category.icon: ImageVector get() = iconFor(iconKey)

fun accountIcon(kind: AccountKind): ImageVector = when (kind) {
    AccountKind.Cash -> Icons.Rounded.Payments
    AccountKind.Bank -> Icons.Rounded.AccountBalance
    AccountKind.Card -> Icons.Rounded.CreditCard
    AccountKind.Wallet -> Icons.Rounded.AccountBalanceWallet
    AccountKind.Savings -> Icons.Rounded.Savings
}
