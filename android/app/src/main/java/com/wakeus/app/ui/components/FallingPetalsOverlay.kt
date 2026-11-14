package com.wakeus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random
import java.util.concurrent.atomic.AtomicLong

private val StaticAnimationClock = androidx.compose.runtime.mutableLongStateOf(0L)

/** Shared frame clock owned by the authenticated app shell, so screens never start their own loops. */
val LocalFallingPetalClock = staticCompositionLocalOf<State<Long>> { StaticAnimationClock }

private data class PetalStyle(
    val xFraction: Float,
    val sizeDp: Float,
    val driftFraction: Float,
    val swayFraction: Float,
    val phase: Float,
    val rotationDegrees: Float,
    val rotationTravel: Float,
    val alpha: Float,
    val color: Color,
)

private data class Petal(
    val cycleSeconds: Float,
    val initialPhase: Float,
    val swayCount: Float,
    val cycles: Array<PetalStyle>,
)

private val petalColors = listOf(
    Color(0xFFFFC9D4),
    Color(0xFFF8B8C7),
    Color(0xFFFFD5DC),
    Color(0xFFEFA9BA),
)

/** Decorative Canvas over screen content, without touch or accessibility semantics. */
@Composable
fun FallingPetalsOverlay(
    modifier: Modifier = Modifier.fillMaxSize(),
    densityScale: Float = 2f,
    resetAnimationOnEnter: Boolean = false,
) {
    // The local is read outside the frame loop; its State is read only inside Canvas drawing.
    val animationClock = LocalFallingPetalClock.current
    val animationStartNanos = remember(resetAnimationOnEnter) { AtomicLong(Long.MIN_VALUE) }
    androidx.compose.runtime.SideEffect {
        if (resetAnimationOnEnter) {
            animationStartNanos.compareAndSet(Long.MIN_VALUE, animationClock.value)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val particleCount = (((maxWidth.value * maxHeight.value) / 20_000f) * densityScale)
            .toInt()
            .coerceIn(if (densityScale < 1f) 6 else 16, 40)
        val petals = remember(particleCount) { createPetals(particleCount) }
        val petalPath = remember { createPetalPath() }

        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val currentFrame = animationClock.value
            val elapsedNanos = if (resetAnimationOnEnter) {
                (currentFrame - animationStartNanos.get()).coerceAtLeast(0L)
            } else {
                currentFrame
            }
            val elapsedSeconds = elapsedNanos / 1_000_000_000f
            val widthPx = size.width
            val heightPx = size.height

            petals.forEach { petal ->
                val cyclePosition = elapsedSeconds / petal.cycleSeconds + petal.initialPhase
                val cycleNumber = floor(cyclePosition).toInt()
                val progress = cyclePosition - cycleNumber
                val style = petal.cycles[Math.floorMod(cycleNumber, petal.cycles.size)]
                val petalHeight = style.sizeDp * density
                val fadeIn = (progress / 0.10f).coerceIn(0f, 1f)
                val fadeOut = ((1f - progress) / 0.12f).coerceIn(0f, 1f)
                val alpha = style.alpha * fadeIn * fadeOut

                val sway = sin((progress * petal.swayCount + style.phase) * (2f * PI.toFloat()))
                val centerX = style.xFraction * widthPx +
                    progress * style.driftFraction * widthPx +
                    sway * style.swayFraction * widthPx
                val centerY = -petalHeight + progress * (heightPx + 2f * petalHeight)
                val rotation = style.rotationDegrees + progress * style.rotationTravel

                withTransform({
                    translate(centerX, centerY)
                    rotate(rotation, pivot = Offset.Zero)
                    scale(petalHeight * 0.58f, petalHeight, pivot = Offset.Zero)
                }) {
                    drawPath(petalPath, style.color.copy(alpha = alpha))
                }
            }
        }
    }
}

private fun createPetals(count: Int): Array<Petal> = Array(count) { index ->
    val random = Random(0x51A7 + index * 7919)
    val cycles = Array(4) { cycle ->
        val styleRandom = Random(0x2D31 + index * 104729 + cycle * 1543)
        PetalStyle(
            xFraction = styleRandom.nextFloat(),
            sizeDp = 8.5f + styleRandom.nextFloat() * 8f,
            driftFraction = (styleRandom.nextFloat() - 0.5f) * 0.14f,
            swayFraction = 0.015f + styleRandom.nextFloat() * 0.035f,
            phase = styleRandom.nextFloat(),
            rotationDegrees = styleRandom.nextFloat() * 360f,
            rotationTravel = (styleRandom.nextFloat() - 0.5f) * 150f,
            alpha = 0.36f + styleRandom.nextFloat() * 0.24f,
            color = petalColors[styleRandom.nextInt(petalColors.size)],
        )
    }
    Petal(
        cycleSeconds = 9f + random.nextFloat() * 9f,
        initialPhase = random.nextFloat(),
        swayCount = 0.6f + random.nextFloat() * 0.8f,
        cycles = cycles,
    )
}

private fun createPetalPath() = Path().apply {
    moveTo(0f, -0.5f)
    cubicTo(0.24f, -0.48f, 0.47f, -0.27f, 0.43f, -0.04f)
    cubicTo(0.40f, 0.19f, 0.13f, 0.43f, 0f, 0.5f)
    cubicTo(-0.13f, 0.42f, -0.42f, 0.17f, -0.43f, -0.07f)
    cubicTo(-0.44f, -0.32f, -0.21f, -0.48f, 0f, -0.5f)
    close()
}
