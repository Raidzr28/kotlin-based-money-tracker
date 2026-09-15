package com.moneymanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The gutter every screen shares. Content never touches the edge of the water. */
val Gutter = 20.dp

/**
 * Lets one item escape the list gutter and run the full width of the screen.
 *
 * The Home hero needs this: a thesis about a column of water cannot be delivered inside a card
 * inset on all four sides, which is the exact arrangement the thesis refuses.
 */
fun Modifier.bleed(gutter: Dp = Gutter): Modifier = layout { measurable, constraints ->
    val extra = (gutter * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra,
        )
    )
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-gutter.roundToPx(), 0)
    }
}

/**
 * The frame for everything that is not a tab: a transparent top bar so the water runs behind it,
 * a real back arrow that mirrors in RTL, and the system Back gesture left completely alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding(),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

/**
 * A tab body.
 *
 * [bottom] is FAB clearance and nothing else -- the outer Scaffold has already padded for the
 * navigation bar -- so a tab without a FAB passes the default and does not end in dead space.
 *
 * [insetTop] is where the status bar is paid for. The shell does not apply the top inset, so a
 * tab that wants its content below the status bar keeps this true; a tab whose first item is
 * meant to reach the top edge of the screen passes false and pays the inset inside that item.
 */
@Composable
fun TabColumn(
    modifier: Modifier = Modifier,
    top: Dp = 8.dp,
    bottom: Dp = 24.dp,
    insetTop: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val statusTop =
        if (insetTop) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else 0.dp
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = Gutter,
            end = Gutter,
            top = top + statusTop,
            bottom = bottom,
        ),
        content = content,
    )
}

/** A block of rows inside one plate, with the plate's own padding already applied. */
@Composable
fun Stack(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) { content() }
}
