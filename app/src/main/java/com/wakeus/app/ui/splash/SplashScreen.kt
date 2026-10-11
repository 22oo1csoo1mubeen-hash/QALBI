package com.wakeus.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wakeus.app.R
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.SubtleGold
import kotlinx.coroutines.delay

/**
 * Splash screen.
 * Uses core-splashscreen API for the system window splash (configured in themes.xml).
 * This composable provides the animated transition after the window splash.
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate heart icon in
        scale.animateTo(1f, animationSpec = tween(500))
        alpha.animateTo(1f, animationSpec = tween(400))
        delay(800)
        onSplashComplete()
    }

    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(MidnightBlue),
        contentAlignment  = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.cd_heart_logo),
                modifier           = Modifier
                    .size(80.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
                tint = SubtleGold,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text     = "QALBI",
                style    = MaterialTheme.typography.displaySmall,
                color    = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(alpha.value),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = stringResource(R.string.tagline),
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alpha.value),
            )
        }
    }
}
