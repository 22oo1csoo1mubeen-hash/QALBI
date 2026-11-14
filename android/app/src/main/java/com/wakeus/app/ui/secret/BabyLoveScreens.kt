package com.wakeus.app.ui.secret

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakeus.app.R
import com.wakeus.app.ui.components.FallingPetalsOverlay
import kotlinx.coroutines.delay

private val BabyLoveBlush = Color(0xFFFFF3F5)
private val LoadingHeart = Color(0xFFD94368)
private val LoadingText = Color(0xFF741B36)

@Composable
fun BabyLoveLoadingScreen(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val fadeDuration = if (animationsEnabled) 650 else 0
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(fadeDuration, easing = FastOutSlowInEasing),
        label = "baby_love_loading_background_fade",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (animationsEnabled) 800 else 0, easing = FastOutSlowInEasing),
        label = "baby_love_loading_text_fade",
    )
    LaunchedEffect(Unit) {
        visible = true
        delay(5_000L)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BabyLoveBlush),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(R.drawable.bg_love),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(backgroundAlpha),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 60.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingHeartPulse(animationsEnabled)
            Spacer(Modifier.height(9.dp))
            Text(
                text = "Shonamma",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 29.sp,
                color = LoadingText,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

@Composable
private fun LoadingHeartPulse(animationsEnabled: Boolean) {
    val heartScale = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "baby_love_loading_heartbeat")
        val scale by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "baby_love_loading_heart_scale",
        )
        scale
    } else {
        1f
    }
    Icon(
        imageVector = Icons.Filled.Favorite,
        contentDescription = null,
        tint = LoadingHeart.copy(alpha = 0.82f),
        modifier = Modifier.size(43.dp).scale(heartScale),
    )
}

@Composable
fun BabyLoveScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var imageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { imageVisible = true }

    AnimatedVisibility(
        visible = imageVisible,
        enter = fadeIn(animationSpec = tween(if (animationsEnabled) 850 else 0)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BabyLoveBlush),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.baby_love),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
            )
            FallingPetalsOverlay(densityScale = 1.24f, resetAnimationOnEnter = true)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xBFFFF3F5))
                    .border(1.dp, Color(0x60FFFFFF), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = LoadingText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
