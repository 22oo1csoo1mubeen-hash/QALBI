package com.wakeus.app.ui.secret

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.wakeus.app.R
import kotlinx.coroutines.delay
import kotlin.math.sin

private val LoadingWineTitle  = Color(0xFF6B1426)
private val LoadingHeartPink  = Color(0xFFD63B60)
private val LoadingTrackBg    = Color(0x35E8A5B2)
private val LoadingBarFillTop = Color(0xFFEB6B88)
private val LoadingBarFillBtm = Color(0xFF881934)

private enum class ParticleType { HEART, PETAL, SPARKLE }

private data class RomanticParticle(
    val id: Int,
    val type: ParticleType,
    val xFraction: Float,
    val speed: Float,
    val size: Float,
    val phase: Float,
    val color: Color,
)

/**
 * Secret Romantic Easter Egg Screen for QALBI.
 *
 * Displays `secret_reference.png` as the single, complete, un-duplicated visual composition
 * preceded by an elegant "Loving" loading sequence.
 */
@Composable
fun SecretRomanticScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    var isReady by remember { mutableStateOf(false) }

    // Romantic transition timer: allows the "Loving" heart animation to be appreciated
    LaunchedEffect(Unit) {
        delay(1800)
        isReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C0B14))
    ) {
        // ── 1. The Complete Secret Reference Visual ─────────────────────────
        AnimatedVisibility(
            visible = isReady,
            enter = fadeIn(animationSpec = tween(700, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Scrollable container ensures the complete 841x1870 reference artwork
                // is beautifully visible from top lanterns to bottom balcony on all screens
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.secret_reference),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentScale = ContentScale.FillWidth,
                    )
                }

                // Ambient floating sparkles & soft hearts (low opacity, decorative only)
                AmbientRomanticParticlesLayer(
                    modifier = Modifier.fillMaxSize(),
                )

                // Top Floating Frosted Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x8AFFF0F4))
                        .border(1.dp, Color(0x60FFFFFF), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LoadingWineTitle,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // ── 2. Romantic "Loving" Loading Screen ──────────────────────────────
        AnimatedVisibility(
            visible = !isReady,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing)),
        ) {
            LovingLoadingExperience()
        }
    }
}

/**
 * Romantic "Loving" loading transition.
 * Features an animated pulsing heart, a horizontal loading progress line,
 * and the word "Loving" in burgundy serif italics.
 */
@Composable
private fun LovingLoadingExperience() {
    val infiniteTransition = rememberInfiniteTransition(label = "loving_loading_transition")

    // Gentle heartbeat pulse: scale 0.96f ↔ 1.20f
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heart_pulse",
    )

    // Indeterminate shimmer progress along the horizontal line
    val progressShimmer by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "line_shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF7F8), Color(0xFFFBE6ED)),
                    radius = 900f,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // Heart Container with Breathing Halo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Subtle glowing background halo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x25E57388)),
                )

                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = LoadingHeartPink,
                    modifier = Modifier.size(52.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            // Thin Horizontal Loading Line with Smooth Shimmer
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LoadingTrackBg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(3.dp)
                        .graphicsLayer {
                            translationX = (progressShimmer * 170.dp.toPx()) - 80f
                        }
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    LoadingBarFillTop,
                                    LoadingBarFillBtm,
                                    Color.Transparent,
                                )
                            )
                        ),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Exact text requirement: "Loving"
            Text(
                text = "Loving",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    color = LoadingWineTitle,
                    letterSpacing = 2.5.sp,
                ),
            )
        }
    }
}

/**
 * Ambient floating particles canvas layer.
 * Drifts floating glowing hearts and sparkles gently on GPU without obscuring text.
 */
@Composable
private fun AmbientRomanticParticlesLayer(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_layer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ambient_progress",
    )

    val particles = remember {
        listOf(
            RomanticParticle(1, ParticleType.HEART, 0.12f, 0.70f, 13f, 0.1f, Color(0x75FF6B8B)),
            RomanticParticle(2, ParticleType.PETAL, 0.22f, 0.55f, 15f, 0.3f, Color(0x60E5395C)),
            RomanticParticle(3, ParticleType.SPARKLE, 0.35f, 0.40f, 11f, 0.5f, Color(0x80FFDF9E)),
            RomanticParticle(4, ParticleType.HEART, 0.52f, 0.75f, 14f, 0.2f, Color(0x70FF85A0)),
            RomanticParticle(5, ParticleType.PETAL, 0.68f, 0.60f, 16f, 0.8f, Color(0x60D82D54)),
            RomanticParticle(6, ParticleType.SPARKLE, 0.82f, 0.45f, 12f, 0.4f, Color(0x80FFEAA8)),
            RomanticParticle(7, ParticleType.HEART, 0.90f, 0.65f, 14f, 0.6f, Color(0x70FF6B8B)),
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            when (p.type) {
                ParticleType.HEART -> {
                    // Hearts drift gently upward
                    val y = ((1f - ((progress * p.speed + p.phase) % 1f)) * height)
                    val x = (p.xFraction * width) + (sin(progress * 6.28f + p.phase * 3f) * 18f)
                    val alpha = if (y < height * 0.15f) (y / (height * 0.15f)) else 1f

                    drawCircle(
                        color = p.color.copy(alpha = (p.color.alpha * alpha).coerceIn(0f, 1f)),
                        radius = p.size * 0.3f,
                        center = Offset(x, y),
                    )
                }
                ParticleType.PETAL -> {
                    // Petals drift slowly downward
                    val y = (((progress * p.speed + p.phase) % 1f) * height)
                    val x = (p.xFraction * width) + (sin(progress * 5f + p.phase * 4f) * 22f)
                    val alpha = if (y > height * 0.85f) ((height - y) / (height * 0.15f)) else 1f

                    val path = Path().apply {
                        moveTo(x, y - p.size)
                        quadraticBezierTo(x + p.size, y, x, y + p.size)
                        quadraticBezierTo(x - p.size, y, x, y - p.size)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = p.color.copy(alpha = (p.color.alpha * alpha).coerceIn(0f, 1f)),
                    )
                }
                ParticleType.SPARKLE -> {
                    val pulse = (sin(progress * 12.5f + p.phase * 6f) + 1f) / 2f
                    val y = (p.phase * height) + (sin(progress * 4f) * 10f)
                    val x = p.xFraction * width

                    drawCircle(
                        color = p.color.copy(alpha = (0.2f + 0.5f * pulse).coerceIn(0f, 1f)),
                        radius = (p.size * (0.5f + 0.4f * pulse)),
                        center = Offset(x, y),
                    )
                }
            }
        }
    }
}
