package com.moneymanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import com.moneymanager.data.Accounts
import com.moneymanager.data.Categories
import com.moneymanager.data.Flow
import com.moneymanager.data.Txn
import com.moneymanager.data.moneyParts
import kotlin.math.absoluteValue

/*
 * The shared vocabulary. Two rules run through all of it.
 *
 * One: cents are always shown and never emphasised. Rounding money away is the thing a tracker
 * may not do, but the cents are not what the eye should land on, so they ship at two thirds the
 * size and a little over half the opacity everywhere in the app.
 *
 * Two: colour never carries the direction of money on its own. Every amount has a sign, and a
 * screen reader hears the sign whether or not the user can see the hue.
 */

/** The tone an amount is set in. Expenses are ordinary; only trouble is loud. */
@Composable
fun toneFor(flow: Flow, over: Boolean = false): Color {
    val water = MoneyTheme.water
    return when {
        over -> water.alert
        flow == Flow.In -> water.income
        flow == Flow.Transfer -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> water.expense
    }
}

@Composable
fun MoneyText(
    minor: Long,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    style: TextStyle = MoneyType.row,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showSign: Boolean = false,
    dimCents: Boolean = true,
) {
    val parts = moneyParts(minor, currency, showSign)
    val base = style.fontSize.takeOrElse { 16.sp }
    Text(
        text = buildAnnotatedString {
            append(parts.sign)
            append(parts.symbol)
            append(parts.whole)
            if (dimCents) {
                withStyle(
                    SpanStyle(color = color.copy(alpha = 0.52f), fontSize = base * 0.64f)
                ) { append(parts.fraction) }
            } else {
                append(parts.fraction)
            }
        },
        style = style,
        color = color,
        modifier = modifier,
    )
}

/** A glass plate. The default container for anything that is not the hero. */
@Composable
fun Plate(
    modifier: Modifier = Modifier,
    depth: Int = 0,
    shape: Shape = MaterialTheme.shapes.large,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .glassPlate(shape, depth)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding)
    ) { content() }
}

/**
 * A section heading. No eyebrow, no numbering: the heading carries its own weight, and the
 * optional trailing action is a text button rather than a chevron pretending to be one.
 */
@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            caption?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/**
 * A merchant mark: the merchant's own initials on a disc tinted deterministically from its name,
 * with the category as a small badge.
 *
 * The ledger is the screen a user reads most, and a column of identical category glyphs makes
 * every row look like every other row. Initials give each merchant a face without needing a logo
 * for it, and the same merchant keeps the same colour forever because the tint is a function of
 * the name rather than of the row index.
 */
@Composable
fun Monogram(
    name: String,
    modifier: Modifier = Modifier,
    badge: ImageVector? = null,
    size: Dp = 42.dp,
) {
    val scale = categoryScale()
    val tint = scale[((name.hashCode().toLong().absoluteValue) % scale.size).toInt()]
    val initials = remember(name) { initialsOf(name) }
    val scheme = MaterialTheme.colorScheme

    Box(modifier.size(size)) {
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                style = MoneyType.small.copy(fontSize = (size.value * 0.31f).sp),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.46f)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    badge,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.26f),
                )
            }
        }
    }
}

private fun initialsOf(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "?"
    }
}

/** A category or account mark: one drawn glyph on a tinted disc, at one consistent weight. */
@Composable
fun Marker(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 42.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

/**
 * One line of the ledger. The merchant leads because that is what the user remembers; the
 * category and account follow in one quiet line; the amount holds the right edge in tabular
 * figures so a column of them aligns on the decimal.
 */
@Composable
fun LedgerRow(
    txn: Txn,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDate: Boolean = false,
) {
    val category = Categories[txn.categoryId]
    val account = Accounts[txn.accountId]
    val subtitle = "${category.label} · ${account.name}"
    // Split and converted are states, not metadata. They leave the separator line and sit beside
    // it in the accent, so the line keeps one separator no matter how many states are true.
    val states = listOfNotNull(
        "split".takeIf { txn.splits.isNotEmpty() },
        "converted".takeIf { txn.originalMinor != null },
    ).joinToString(", ")

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Monogram(txn.merchant, badge = category.icon)
        Column(Modifier.weight(1f)) {
            Text(
                txn.merchant,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (states.isNotEmpty()) {
                    Text(
                        states,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                txn.amountMinor,
                style = MoneyType.row,
                color = toneFor(txn.flow),
                showSign = txn.flow == Flow.In,
                currency = txn.currency,
            )
            Text(
                if (showDate) com.moneymanager.data.dayLabel(txn.date)
                else "%02d:%02d".format(txn.time.hour, txn.time.minute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A pill action. The hero's row of three, and the only button shape that sits on the water. */
@Composable
fun Pill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    container: Color? = null,
    contentColor: Color? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val bg = container ?: if (emphasis) scheme.primary else water.glass
    val fg = (contentColor ?: if (emphasis) scheme.onPrimary else scheme.onSurface)
        .copy(alpha = if (enabled) 1f else 0.38f)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) bg else bg.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** A short flag: one drawn glyph, one line, one tone. Used for streaks, warnings and states. */
@Composable
fun Flag(
    icon: ImageVector,
    text: String,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(tone.copy(alpha = 0.15f))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone)
    }
}

/** A tag or filter chip. Selected state is a fill change, never a colour-only outline change. */
@Composable
fun Chip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerLow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.let {
            Icon(
                it,
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** A labelled figure. Used wherever a plate needs two or three facts side by side. */
@Composable
fun Stat(
    label: String,
    valueMinor: Long,
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MoneyType.medium,
    showSign: Boolean = false,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            valueMinor,
            style = style,
            color = tone,
            showSign = showSign,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** A row that navigates somewhere. One chevron, 48dp tall, never a whole card per link. */
@Composable
fun NavRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Marker(icon, tint = tint, size = 38.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .clearAndSetSemantics { },
        )
    }
}

/**
 * The empty state. It says what will be here, and offers the one action that fills it -- never a
 * shrug and a grey illustration.
 */
@Composable
fun EmptyWater(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
) {
    Plate(modifier, depth = 1) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 460.dp),
            )
            if (actionLabel != null && onAction != null && actionIcon != null) {
                Pill(
                    actionLabel, actionIcon, onAction,
                    emphasis = true,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .defaultMinSize(minHeight = 48.dp),
                )
            }
        }
    }
}

/** A line of small print that belongs to the scaffold, not the product. */
@Composable
fun ScaffoldNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(top = 10.dp),
    )
}
