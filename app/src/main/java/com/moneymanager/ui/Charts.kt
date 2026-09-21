package com.moneymanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneymanager.data.money
import com.moneymanager.data.weekdayInitials
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Every chart here is drawn rather than pulled from a library, for one reason: the charts have
 * to belong to the same water as the rest of the app. A generic chart kit would put its own
 * house style -- its own gridlines, its own default categorical rainbow -- in the middle of a
 * committed world, and no amount of theming arguments gets it back out.
 *
 * The categorical scale holds no warm hue at all. #F6C566 belongs to goals and streaks and to
 * nothing else, so a warm mark anywhere in the app means one thing; a spending category that
 * happened to land on index four would have quietly broken that.
 */

@Composable
fun categoryScale(): List<Color> = if (isDarkWorld()) listOf(
    Color(0xFF8FD8FF),
    Color(0xFF5AA9F0),
    Color(0xFF3D7FD0),
    Color(0xFF43D39E),
    Color(0xFFE086C8),
    Color(0xFF7C6FE8),
    Color(0xFF7FC4C9),
    Color(0xFF2E5C90),
) else listOf(
    Color(0xFF1E6FB8),
    Color(0xFF15558F),
    Color(0xFF0B3A66),
    Color(0xFF0E7A55),
    Color(0xFFA63A86),
    Color(0xFF4B3FB0),
    Color(0xFF2C7F87),
    Color(0xFF4A80B5),
)

@Composable
private fun isDarkWorld(): Boolean = MaterialTheme.colorScheme.background.luminanceIsDark()

private fun Color.luminanceIsDark(): Boolean =
    (0.2126f * red + 0.7152f * green + 0.0722f * blue) < 0.45f

data class Slice(val label: String, val valueMinor: Long, val color: Color)

/**
 * The ring. Segment labels ride the arc itself rather than sitting in a legend below, because a
 * legend forces the eye to bounce between a colour swatch and a wedge; a label on its own wedge
 * is read once. Each label is a real text node, so TalkBack reads the ring as a list.
 *
 * [strokeWidth] stays generous: a hairline donut is a decoration, and this one is the screen.
 */
@Composable
fun RingChart(
    slices: List<Slice>,
    centreValueMinor: Long,
    centreCaption: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp,
) {
    val total = slices.sumOf { it.valueMinor }.coerceAtLeast(1)
    val scheme = MaterialTheme.colorScheme
    val sweepIn by rememberFill(1f)

    BoxWithConstraints(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val labelRadius = side / 2 - strokeWidth / 2 - 2.dp

        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = scheme.surfaceContainerLowest,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )

            var start = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.valueMinor / total * sweepIn
                if (sweep > 0.6f) {
                    drawArc(
                        brush = Brush.linearGradient(
                            listOf(slice.color, slice.color.copy(alpha = 0.72f)),
                        ),
                        startAngle = start + 0.8f,
                        sweepAngle = sweep - 1.6f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
        }

        // Labels, each rotated to its own wedge's tangent.
        var cursor = -90f
        slices.forEach { slice ->
            val sweep = 360f * slice.valueMinor / total
            val mid = cursor + sweep / 2
            cursor += sweep
            if (sweep < 26f) return@forEach // too thin to letter; the list below carries it

            val rad = mid * PI.toFloat() / 180f
            val dx = cos(rad) * labelRadius.value
            val dy = sin(rad) * labelRadius.value
            // Keep the lettering upright-ish rather than upside down on the left half.
            val flip = if (mid > 90f || mid < -90f) 180f else 0f

            Text(
                text = slice.label,
                style = MaterialTheme.typography.labelSmall,
                color = onArcInk(slice.color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(dx.dp, dy.dp)
                    .graphicsLayer { rotationZ = mid + 90f + flip },
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                money(centreValueMinor),
                style = MoneyType.large,
                color = scheme.onSurface,
            )
            Text(
                centreCaption,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Ink that survives on top of an arc of the given colour. */
private fun onArcInk(on: Color): Color =
    if (on.luminanceIsDark()) Color(0xFFEAF4FF) else Color(0xFF04182C)

/**
 * Income against spend, month by month. Paired columns rather than stacked, because the question
 * this chart answers is "did more come in than went out", and a stack hides exactly that.
 */
@Composable
fun ColumnPair(
    incoming: List<Long>,
    outgoing: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val peak = max(incoming.maxOrNull() ?: 1L, outgoing.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val grow by rememberFill(1f)

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val slots = labels.size.coerceAtLeast(1)
            val slotW = size.width / slots
            val barW = (slotW * 0.28f).coerceAtMost(18.dp.toPx())
            val gap = slotW * 0.08f
            val r = CornerRadius(barW / 2f)

            // Three quiet depth lines instead of a gridded chart frame.
            repeat(3) { i ->
                val y = size.height * (i + 1) / 4f
                drawLine(
                    color = scheme.outlineVariant.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }

            labels.indices.forEach { i ->
                val cx = slotW * i + slotW / 2f
                val inH = size.height * (incoming.getOrElse(i) { 0 }.toFloat() / peak) * grow
                val outH = size.height * (outgoing.getOrElse(i) { 0 }.toFloat() / peak) * grow

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(water.income, water.income.copy(alpha = 0.45f)),
                        startY = size.height - inH,
                        endY = size.height,
                    ),
                    topLeft = Offset(cx - barW - gap / 2, size.height - inH),
                    size = Size(barW, inH),
                    cornerRadius = r,
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(scheme.primary, scheme.primary.copy(alpha = 0.4f)),
                        startY = size.height - outH,
                        endY = size.height,
                    ),
                    topLeft = Offset(cx + gap / 2, size.height - outH),
                    size = Size(barW, outH),
                    cornerRadius = r,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            labels.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Net worth as a sounding: what you own runs up from the line, what you owe hangs below it, and
 * the bright trace is the difference. Reading the gap closing is the whole point, so the two
 * areas share one scale and one baseline.
 */
@Composable
fun SoundingLine(
    assets: List<Long>,
    debts: List<Long>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    val peak = max(assets.maxOrNull() ?: 1L, debts.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
    val reveal by rememberFill(1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val n = assets.size.coerceAtLeast(2)
        val step = size.width / (n - 1)
        val mid = size.height / 2f
        val span = size.height / 2f - 6.dp.toPx()

        fun x(i: Int) = step * i
        fun up(v: Long) = mid - span * (v / peak)
        fun down(v: Long) = mid + span * (v / peak)

        fun area(values: List<Long>, project: (Long) -> Float, tint: Color) {
            val p = Path()
            p.moveTo(0f, mid)
            values.forEachIndexed { i, v -> p.lineTo(x(i), mid + (project(v) - mid) * reveal) }
            p.lineTo(x(values.lastIndex), mid)
            p.close()
            drawPath(p, Brush.verticalGradient(listOf(tint.copy(alpha = 0.42f), tint.copy(alpha = 0.04f))))
            val edge = Path()
            values.forEachIndexed { i, v ->
                val y = mid + (project(v) - mid) * reveal
                if (i == 0) edge.moveTo(x(i), y) else edge.lineTo(x(i), y)
            }
            drawPath(edge, tint.copy(alpha = 0.8f), style = Stroke(width = 1.5.dp.toPx()))
        }

        area(assets, ::up, scheme.primary)
        area(debts, ::down, water.alert)

        // The baseline: zero. Everything above it is yours.
        drawLine(
            color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
            start = Offset(0f, mid),
            end = Offset(size.width, mid),
            strokeWidth = 1.dp.toPx(),
        )

        // Net worth itself, on the assets scale.
        val net = Path()
        assets.indices.forEach { i ->
            val v = assets[i] - debts.getOrElse(i) { 0 }
            val y = mid - span * (v / peak) * reveal
            if (i == 0) net.moveTo(x(i), y) else net.lineTo(x(i), y)
        }
        drawPath(net, water.waterline, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        val lastV = assets.last() - debts.last()
        drawCircle(
            color = water.waterline,
            radius = 4.dp.toPx(),
            center = Offset(x(assets.lastIndex), mid - span * (lastV / peak) * reveal),
        )
    }
}

/**
 * The month as a tide table: one cell per day, darker where more left the account. Days with
 * nothing logged read as gaps rather than as zeros, which is the honest rendering -- a blank day
 * usually means the user forgot, not that they spent nothing.
 */
@Composable
fun CashFlowGrid(
    daily: List<Long>,
    firstDayOfWeekOffset: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val peak = (daily.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
    // Whichever day the user starts their week on leads the row.
    val weekdays = weekdayInitials()

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val cells = firstDayOfWeekOffset + daily.size
        val rows = (cells + 6) / 7
        Canvas(
            Modifier
                .fillMaxWidth()
                .height((rows * 30).dp)
                .padding(top = 6.dp)
                .semantics {
                    contentDescription =
                        "Cash flow calendar, ${daily.count { it > 0 }} days with spending this month"
                }
        ) {
            val cell = size.width / 7f
            val pad = 3.dp.toPx()
            val r = CornerRadius(7.dp.toPx())
            daily.forEachIndexed { i, amount ->
                val slot = firstDayOfWeekOffset + i
                val col = slot % 7
                val row = slot / 7
                val t = (amount / peak).coerceIn(0f, 1f)
                val fill = if (amount == 0L) {
                    scheme.surfaceContainerLowest.copy(alpha = 0.6f)
                } else {
                    scheme.primary.copy(alpha = 0.20f + 0.72f * t)
                }
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(col * cell + pad, row * 30.dp.toPx() + pad),
                    size = Size(cell - pad * 2, 30.dp.toPx() - pad * 2),
                    cornerRadius = r,
                )
            }
        }
    }
}

/** A plain trace for an account's running balance. Quiet on purpose; the list beneath is the news. */
@Composable
fun BalanceTrace(
    values: List<Long>,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (values.size < 2) return@Canvas
        val lo = values.min().toFloat()
        val hi = values.max().toFloat()
        val span = (hi - lo).takeIf { it > 0f } ?: 1f
        val step = size.width / (values.size - 1)
        val p = Path()
        values.forEachIndexed { i, v ->
            val y = size.height - (v - lo) / span * (size.height - 8.dp.toPx()) - 4.dp.toPx()
            if (i == 0) p.moveTo(0f, y) else p.lineTo(step * i, y)
        }
        val fill = Path().apply {
            addPath(p)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(tint.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(p, tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * A depth gauge: ticks down the edge of the hero, one per remaining day, brightening toward the
 * end of the month. It turns "18 days left" from a sentence into a length.
 */
@Composable
fun DepthGauge(
    elapsed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    Canvas(
        modifier
            .width(14.dp)
            .fillMaxHeight()
    ) {
        val n = total.coerceAtLeast(1)
        val step = size.height / n
        repeat(n) { i ->
            val spent = i < elapsed
            val long = (i + 1) % 7 == 0
            drawLine(
                color = if (spent) water.waterline.copy(alpha = 0.75f)
                else scheme.onSurfaceVariant.copy(alpha = 0.3f),
                start = Offset(size.width - (if (long) 12.dp.toPx() else 6.dp.toPx()), step * i + step / 2),
                end = Offset(size.width, step * i + step / 2),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

/** Small shared helper: a horizontal legend row for a chart that needs one anyway. */
@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The month with its due dates marked. A list already tells you what is coming; the grid tells
 * you how it clusters -- three things landing in the same week is the fact a list hides.
 */
@Composable
fun DueCalendar(
    dueDays: Set<Int>,
    daysInMonth: Int,
    firstDayOffset: Int,
    todayDay: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val water = MoneyTheme.water
    // Whichever day the user starts their week on leads the row.
    val weekdays = weekdayInitials()

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val rows = (firstDayOffset + daysInMonth + 6) / 7
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            val cell = maxWidth / 7
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(cell * rows)
            ) {
                val w = size.width / 7f
                val h = w
                (1..daysInMonth).forEach { day ->
                    val slot = firstDayOffset + day - 1
                    val col = slot % 7
                    val row = slot / 7
                    val cx = col * w + w / 2f
                    val cy = row * h + h / 2f
                    if (day == todayDay) {
                        drawCircle(
                            color = scheme.primary.copy(alpha = 0.22f),
                            radius = w * 0.34f,
                            center = Offset(cx, cy),
                        )
                    }
                    if (day in dueDays) {
                        drawCircle(
                            color = if (day < todayDay) water.alert else water.waterline,
                            radius = 4.dp.toPx(),
                            center = Offset(cx, cy + h * 0.26f),
                        )
                    }
                    drawCircle(
                        color = scheme.onSurfaceVariant.copy(
                            alpha = if (day in dueDays || day == todayDay) 0.9f else 0.28f,
                        ),
                        radius = 1.6.dp.toPx(),
                        center = Offset(cx, cy - h * 0.06f),
                    )
                }
            }
        }
    }
}
