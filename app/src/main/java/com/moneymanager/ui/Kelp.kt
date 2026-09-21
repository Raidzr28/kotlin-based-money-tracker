package com.moneymanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** A slow, endless sway. Held at zero when motion is off, so the bed simply stands still. */
@Composable
private fun rememberSway(running: Boolean): State<Float> =
    androidx.compose.animation.core.rememberInfiniteTransition(label = "sway").animateFloat(
        initialValue = 0f,
        targetValue = if (running) 1f else 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(
                9_000,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "sway",
    )

/**
 * A bed of kelp that grows with what the user has actually put away.
 *
 * The README asks for "a simple avatar/city/plant that grows as savings goals or budget-adherence
 * streaks improve". A cartoon plant or a little city would be a second visual world bolted to the
 * side of this one; the design system commits to a lit column of water, so the thing that grows
 * in it grows underwater. Same idea, one world.
 *
 * [progress] is not a score this file invents. It is passed in, derived from goals and streaks by
 * the caller, so the bed cannot claim growth the ledger does not support -- the same rule the
 * challenge engine follows.
 *
 * @param progress 0f to 1f. Drives how tall each frond stands and how many there are.
 * @param animate false holds it still, for the reduce-motion setting and for screens that are
 *   not the one the user is looking at.
 */
@Composable
fun KelpBed(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
    animate: Boolean = true,
) {
    val water = MoneyTheme.water
    val scheme = MaterialTheme.colorScheme
    val grown = progress.coerceIn(0f, 1f)
    val sway by rememberSway(animate)

    // Between three and nine fronds. Even an empty bed shows something: a seabed with nothing on
    // it reads as a rendering failure, where three short shoots read as "not yet".
    val fronds = 3 + (grown * 6f).toInt()

    val description = when {
        grown <= 0.01f -> "Savings bed, nothing growing yet"
        grown >= 0.99f -> "Savings bed, fully grown"
        else -> "Savings bed, ${(grown * 100).toInt()} percent grown"
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
    ) {
        val floor = size.height
        val step = size.width / (fronds + 1)

        repeat(fronds) { i ->
            // Deterministic per frond rather than random, so the bed does not reshuffle itself
            // on every recomposition -- a plant that moves when you scroll past is a distraction.
            val seed = (i * 37 % 11) / 11f
            val x = step * (i + 1)

            // The tallest fronds are in the middle of the bed, which reads as a clump rather
            // than a fence. Growth raises the whole bed; the shortest never quite vanishes.
            val centre = 1f - (kotlin.math.abs(i - (fronds - 1) / 2f) / (fronds / 2f + 0.5f))
            val tall = floor * (0.28f + 0.62f * grown) * (0.55f + 0.45f * centre)

            val phase = sway * 2f * PI.toFloat() + seed * 6.28f
            // The tip travels; the base does not. Kelp is anchored.
            val lean = sin(phase) * (step * 0.42f) * (0.35f + 0.65f * grown)

            val path = Path().apply {
                moveTo(x, floor)
                quadraticBezierTo(
                    x + lean * 0.25f, floor - tall * 0.55f,
                    x + lean, floor - tall,
                )
            }

            drawPath(
                path,
                color = if (i % 3 == 0) water.goal.copy(alpha = 0.55f) else water.waterline,
                style = Stroke(
                    width = (2.2f + 2.6f * grown).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = 0.35f + 0.5f * grown,
            )

            // A bud on the tip of the longer fronds once the bed is established.
            if (grown > 0.55f && i % 2 == 0) {
                drawCircle(
                    color = water.goal,
                    radius = (1.6f + 1.8f * grown).dp.toPx(),
                    center = Offset(x + lean, floor - tall),
                    alpha = 0.5f + 0.4f * grown,
                )
            }
        }

        // The seabed itself, so the fronds are planted in something.
        drawLine(
            color = scheme.onSurfaceVariant.copy(alpha = 0.18f),
            start = Offset(0f, floor),
            end = Offset(size.width, floor),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/**
 * How far a goal and a streak have carried the bed.
 *
 * Averaged across goals rather than summed, so opening a second goal does not shrink the plant
 * the first one grew; the streak is worth a quarter and tops out at a month, because a bed that
 * only ever answers to goals stops moving for anyone who has not set one.
 */
fun kelpProgress(goalFractions: List<Float>, loggingStreakDays: Int): Float {
    val goals = if (goalFractions.isEmpty()) 0f else goalFractions.map { it.coerceIn(0f, 1f) }.average().toFloat()
    val streak = (loggingStreakDays / 30f).coerceIn(0f, 1f)
    return (goals * 0.75f + streak * 0.25f).coerceIn(0f, 1f)
}
