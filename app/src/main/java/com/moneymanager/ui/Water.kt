package com.moneymanager.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/*
 * The materials of the world. Everything here exists so that one fact is legible at a glance:
 * how much water is left in the month.
 *
 * Glass is not applied for looks. A plate's translucency and tint are a function of how deep it
 * sits in the stack -- the same function Material 3 already uses for tonal elevation -- and the
 * specular hairline along a plate's top edge is the light that reaches it. A plate with no depth
 * argument is at the surface and is the brightest thing on screen.
 *
 * The plates transmit: their bed is translucent, so the ground's gradient and its caustics are
 * visible through every card rather than being painted and then buried. What they do not do is
 * refract -- Compose has no backdrop filter at this version, and a blur applied to a layer blurs
 * that layer's own content, not what sits behind it. Transmission without refraction is the
 * honest half of the material; a fake blur would be the dishonest half.
 * ponytail: revisit when a real backdrop-blur API lands in Compose.
 */

/**
 * Every duration and curve in the app, in one place.
 *
 * The curves matter more than the numbers. Material's [FastOutSlowInEasing] is its *standard*
 * curve, which accelerates in and decelerates out; it is right for something moving between two
 * on-screen positions and wrong for something arriving. An entrance should already be at speed
 * when it appears and settle from there, which is [LinearOutSlowInEasing]. An exit should build
 * speed as it leaves, which is [FastOutLinearInEasing].
 */
object MoneyMotion {
    /** Every quiet fill in the app. One gesture, one duration. */
    const val Fill = 720

    /** The Home hero: the same gesture, given room because it is the one carrying the screen. */
    const val FillHero = 1100

    const val Enter = 190
    const val Exit = 150
    const val Push = 280

    /** Arriving. Already at speed, settling. */
    val EnterEasing: Easing = LinearOutSlowInEasing

    /** Leaving. Gathering speed on the way out. */
    val ExitEasing: Easing = FastOutLinearInEasing
}

/** The system "Remove animations" setting. Every animated surface here reads it first. */
@Composable
fun motionEnabled(): Boolean = remember { ValueAnimator.areAnimatorsEnabled() }

/**
 * A fill that plays once and stays filled.
 *
 * A LazyColumn disposes an item the moment it scrolls out of view, so a plain
 * `animateFloatAsState` replays its reveal every single time the user scrolls back to it. That
 * turns a one-shot gesture into a loop triggered by scrolling, which is the thing the design
 * rules out. [rememberSaveable] survives that disposal, because the list holds saveable state per
 * item key, so the second look arrives already full.
 */
@Composable
fun rememberFill(target: Float, durationMillis: Int = MoneyMotion.Fill): State<Float> {
    var played by rememberSaveable { mutableStateOf(false) }
    val moving = motionEnabled()
    val anim = remember { Animatable(if (played) target else 0f) }
    LaunchedEffect(target, moving) {
        if (!moving) {
            anim.snapTo(target)
        } else {
            anim.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = if (played) MoneyMotion.Fill else durationMillis,
                    easing = MoneyMotion.EnterEasing,
                ),
            )
        }
        played = true
    }
    return anim.asState()
}

@Composable
private fun rememberDrift(running: Boolean, durationMillis: Int): State<Float> =
    rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = if (running) 1f else 0f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "drift",
    )

/**
 * The ground: a lit column of water. Light enters top-left, scatters through three slow
 * caustics, and falls away into the abyss at the bottom of the scroll.
 *
 * Caustics drift only while [animate] is true, which callers scope to the screen that owns the
 * hero. A background that animates on every screen is a battery bill, not a design.
 */
@Composable
fun WaterField(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val water = MoneyTheme.water
    val drift by rememberDrift(animate && motionEnabled(), durationMillis = 24_000)

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to water.depth[0],
                        0.22f to water.depth[1],
                        0.52f to water.depth[2],
                        0.78f to water.depth[3],
                        1f to water.depth[4],
                    )
                )
                caustic(water.causticHigh, 0.18f, 0.06f, 0.95f, drift, 0f)
                caustic(water.causticHigh, 0.86f, 0.20f, 0.70f, drift, 0.62f)
                caustic(water.causticLow, 0.42f, 0.58f, 1.30f, drift, 0.35f)
            }
    )
}

private fun DrawScope.caustic(
    tint: Color,
    fx: Float,
    fy: Float,
    fr: Float,
    drift: Float,
    seed: Float,
) {
    val radius = size.minDimension * fr
    val sway = sin((drift + seed) * 2f * PI.toFloat()) * radius * 0.07f
    val bob = sin((drift * 0.7f + seed) * 2f * PI.toFloat()) * radius * 0.04f
    val centre = Offset(size.width * fx + sway, size.height * fy + bob)
    drawCircle(
        brush = Brush.radialGradient(
            0f to tint,
            0.55f to tint.copy(alpha = tint.alpha * 0.45f),
            1f to Color.Transparent,
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

/**
 * The plate material. [depth] 0 sits at the surface and catches the most light; each step down
 * is denser and duller, which is how a stacked sheet reads as *under* the one above it rather
 * than merely beside it.
 */
@Composable
fun Modifier.glassPlate(
    shape: Shape = MaterialTheme.shapes.large,
    depth: Int = 0,
): Modifier {
    val tokens = MoneyTheme.water
    val scheme = MaterialTheme.colorScheme
    val sink = depth.coerceIn(0, 3)
    val fill = tokens.glass.copy(alpha = tokens.glass.alpha * (1f - sink * 0.18f))
    // Deeper plates hold more of their own colour; every one of them still lets the ground read
    // through, which is the whole reason the ground is drawn.
    val bed = when (sink) {
        0 -> scheme.surfaceContainerHigh.copy(alpha = 0.62f)
        1 -> scheme.surfaceContainer.copy(alpha = 0.70f)
        2 -> scheme.surfaceContainerLow.copy(alpha = 0.78f)
        else -> scheme.surfaceContainerLowest.copy(alpha = 0.86f)
    }
    val edge = tokens.glassEdge.copy(alpha = tokens.glassEdge.alpha * (1f - sink * 0.25f))
    return this
        .clip(shape)
        .background(bed, shape)
        .background(fill, shape)
        .drawBehind {
            // Specular: light lands on the top edge of a submerged sheet and skims off it.
            val r = 22.dp.toPx().coerceAtMost(size.height / 2f)
            drawPath(
                path = Path().apply {
                    moveTo(0f, r)
                    quadraticTo(0f, 0f, r, 0f)
                    lineTo(size.width - r, 0f)
                    quadraticTo(size.width, 0f, size.width, r)
                },
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.3f to edge,
                    0.7f to edge,
                    1f to Color.Transparent,
                ),
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
}

/**
 * A container filled with water to [level] (0f..1f of its own height).
 *
 * This is the one authored motion in the app: on first composition the level rises from empty
 * with an ease-out, and the surface keeps a slow two-wave swell afterwards. It runs on the Home
 * hero and nowhere else, so the gesture stays a statement instead of a tic.
 *
 * [over] inverts the reading -- the budget is spent, the level is past the line, the fill turns
 * to the alert tone. Never colour alone: callers pair it with a sign and a written label.
 */
@Composable
fun WaterColumn(
    level: Float,
    modifier: Modifier = Modifier,
    over: Boolean = false,
    shape: Shape = RoundedCornerShape(32.dp),
    swell: Boolean = true,
    fillMillis: Int = MoneyMotion.Fill,
    /** Darkens the upper part of the column so type over it holds contrast at any fill level. */
    scrim: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val water = MoneyTheme.water
    val moving = motionEnabled()
    val settled by rememberFill(level.coerceIn(0f, 1f), fillMillis)
    val phase by rememberDrift(moving && swell, durationMillis = 6_400)

    val body = if (over) water.alert else MaterialTheme.colorScheme.primary
    val line = if (over) water.alert else water.waterline
    val bed = MaterialTheme.colorScheme.surfaceContainerLowest

    Box(
        modifier
            .clip(shape)
            .background(bed, shape)
            .drawBehind {
                drawSwell(settled, phase, body, line)
                if (scrim) {
                    // The figure sits near the top and the water can reach it in a healthy month.
                    // Light falls off with depth anyway, so the scrim is the world's own physics
                    // doing the accessibility work.
                    drawRect(
                        Brush.verticalGradient(
                            0f to bed.copy(alpha = 0.72f),
                            0.5f to bed.copy(alpha = 0.26f),
                            0.85f to Color.Transparent,
                        )
                    )
                }
            }
    ) { content() }
}

private fun DrawScope.drawSwell(
    level: Float,
    phase: Float,
    body: Color,
    line: Color,
    tint: Float = 1f,
) {
    val surfaceY = size.height * (1f - level)
    val a1 = 5.dp.toPx()
    val a2 = 2.6.dp.toPx()
    val steps = 48
    val t = phase * 2f * PI.toFloat()

    fun yAt(x: Float): Float {
        val u = x / size.width
        return surfaceY +
            sin(u * 2.1f * PI.toFloat() + t) * a1 +
            sin(u * 4.7f * PI.toFloat() - t * 1.6f) * a2
    }

    val body_ = Path().apply {
        moveTo(0f, yAt(0f))
        for (i in 1..steps) lineTo(size.width * i / steps, yAt(size.width * i / steps))
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(
        body_,
        Brush.verticalGradient(
            0f to body.copy(alpha = 0.92f * tint),
            0.45f to body.copy(alpha = 0.55f * tint),
            1f to body.copy(alpha = 0.30f * tint),
            startY = surfaceY,
            endY = size.height,
        ),
    )

    // The waterline. The brightest stroke on the screen, because it is the one number the user
    // opened the app to read.
    val edge = Path().apply {
        moveTo(0f, yAt(0f))
        for (i in 1..steps) lineTo(size.width * i / steps, yAt(size.width * i / steps))
    }
    drawPath(edge, line, style = Stroke(width = 2.dp.toPx()))
}

/**
 * A photograph with the water rising over it.
 *
 * This is the one place in the app where a picture belongs, and it is the app's own gesture
 * applied to it: the thing you are saving for goes under as the goal fills. The water is tinted
 * lighter than it is on the hero so the photograph reads through the submerged part rather than
 * being painted out by it.
 */
@Composable
fun SubmergedPhoto(
    painter: Painter,
    level: Float,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentDescription: String? = null,
) {
    val moving = motionEnabled()
    val settled by rememberFill(level.coerceIn(0f, 1f))
    val phase by rememberDrift(moving, durationMillis = 6_400)
    val body = MaterialTheme.colorScheme.primary
    val line = MoneyTheme.water.waterline

    Box(modifier.clip(shape)) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Canvas(Modifier.matchParentSize()) {
            drawSwell(settled, phase, body, line, tint = 0.62f)
        }
    }
}

/**
 * Work happening, for the moments where the app cannot say how long it will take.
 *
 * A bar with no motion reads as frozen, and reading a receipt or fetching a rate takes long
 * enough to matter. This is the one looping animation in the app and it is allowed to be one:
 * an indeterminate indicator is a progress indicator, not a gesture, which is also why it is the
 * only place [LinearEasing] is correct -- an eased sweep would appear to stall at each end.
 *
 * It exists only while something is genuinely in flight. Callers remove it, not hide it.
 */
@Composable
fun WaterPulse(modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val scheme = MaterialTheme.colorScheme
    val moving = motionEnabled()
    val sweep by rememberDrift(moving, durationMillis = 1_200)
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier
            .height(height)
            .clip(shape)
            .background(scheme.surfaceContainerLowest, shape)
            .drawBehind {
                // With animations off the bar simply sits filled: still honest about something
                // being in progress, without moving.
                val width = if (!moving) size.width else size.width * 0.35f
                val travel = if (!moving) 0f else (size.width + width) * sweep - width
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            scheme.primary.copy(alpha = 0f),
                            scheme.primary,
                            scheme.primary.copy(alpha = 0f),
                        ),
                        startX = travel,
                        endX = travel + width,
                    ),
                    topLeft = Offset(travel, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
    )
}

/**
 * A budget envelope as a level in a tube. Horizontal, because a list of them reads as a row of
 * gauges; the tick at [paceAt] is where the month's own clock has reached, so a bar short of the
 * tick is under pace and a bar past it is burning too fast.
 */
@Composable
fun WaterBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    over: Boolean = false,
    paceAt: Float? = null,
    height: Dp = 10.dp,
) {
    val water = MoneyTheme.water
    val scheme = MaterialTheme.colorScheme
    val settled by rememberFill(fraction.coerceIn(0f, 1f))
    val shape = RoundedCornerShape(percent = 50)
    val fill = if (over) water.alert else scheme.primary
    Box(
        modifier
            .height(height)
            .clip(shape)
            .background(scheme.surfaceContainerLowest, shape)
            .drawBehind {
                val w = size.width * settled
                if (w > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(fill.copy(alpha = 0.5f), fill),
                        ),
                        size = Size(w, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                }
                paceAt?.let {
                    val x = size.width * it.coerceIn(0f, 1f)
                    drawLine(
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }
            }
    )
}
