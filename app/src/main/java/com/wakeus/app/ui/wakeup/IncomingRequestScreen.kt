package com.wakeus.app.ui.wakeup

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.ui.components.QalbiRomanticButton
import com.wakeus.app.viewmodel.IncomingRequestViewModel

private val ScreenBgFallback = Color(0xFFFBF6F3)
private val DeepWineTitle = Color(0xFF4A1424)
private val DeepWinePrimary = Color(0xFF8B253E)
private val DarkWineText = Color(0xFF301019)
private val MutedWineText = Color(0xFF7C5560)
private val CardBg = Color(0xFFFFF9FA)
private val CardBorderColor = Color(0xFFF3D4DC)

@Composable
fun IncomingRequestScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: IncomingRequestViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val alarmState by viewModel.alarmState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    // Pulsing animation for active ringing alarm
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    LaunchedEffect(requestId) {
        viewModel.loadRequest(context, requestId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.actionSuccessMessage) {
        uiState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgFallback)
    ) {
        Image(
            painter = painterResource(id = R.drawable.overall_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepWinePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Wake-Up Alert",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = DeepWineTitle,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val request = uiState.request

                if (uiState.isLoading && request == null) {
                    CircularProgressIndicator(
                        color = DeepWinePrimary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Loading wake-up details…",
                        fontSize = 14.sp,
                        color = MutedWineText,
                    )
                } else if (request == null) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color(0xFFB00020),
                        modifier = Modifier.size(54.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Wake-up request not found or has been removed.",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = DarkWineText,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(Modifier.height(12.dp))

                    // If alarm is actively ringing, show pulsating alarm banner
                    if (alarmState.isRinging) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFCEBEF),
                            border = BorderStroke(1.2.dp, Color(0xFFF0B6C2)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = "Alarm Active",
                                    tint = DeepWinePrimary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .scale(pulseScale)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "WAKE-UP CALL RINGING",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp,
                                    color = DeepWinePrimary,
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Sender Avatar ──────────────────────────────────────────
                    Box(
                        modifier = Modifier.size(116.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .background(Color(0x20E57388))
                                .border(1.5.dp, Color(0x60F2A9B8), CircleShape),
                        )

                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFEB7288), Color(0xFF912440)),
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            val initials = request.senderDisplayName.split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .take(2)
                                .joinToString("")
                                .ifEmpty { request.senderUsername.take(2).uppercase() }

                            Text(
                                text = initials,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 42.sp,
                                color = Color.White,
                            )
                        }

                        // Romantic heart badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 4.dp, bottom = 4.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3395F))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "${request.senderDisplayName} is waking you up! ☀️",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = DarkWineText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "@${request.senderUsername}",
                        fontSize = 14.sp,
                        color = MutedWineText,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Request status card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFDECEF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    tint = DeepWinePrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Status: ${request.status.name}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (request.isPending) "Awaiting your wake-up response" else "Request completed",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Actions based on request status
                    when (request.status) {
                        WakeUpRequestStatus.PENDING -> {
                            if (request.isExpired) {
                                Text(
                                    text = "This request has expired.",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 14.sp,
                                    color = MutedWineText,
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(26.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                                ) {
                                    Text("Go Back", color = DeepWinePrimary, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                // Primary prominent I'M AWAKE button
                                QalbiRomanticButton(
                                    text = "☀️ I'M AWAKE!",
                                    onClick = { viewModel.acknowledge(context, onDone = onBack) },
                                    isLoading = uiState.isResponding || alarmState.isAcknowledging,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = { viewModel.decline(context, onDone = onBack) },
                                    enabled = !uiState.isResponding,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(26.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                                ) {
                                    Text(
                                        text = "Decline",
                                        color = DeepWinePrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    )
                                }
                            }
                        }

                        WakeUpRequestStatus.ACKNOWLEDGED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "You let them know you're awake! ❤️",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = DarkWineText,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            QalbiRomanticButton(text = "Done", onClick = onBack, modifier = Modifier.fillMaxWidth())
                        }

                        WakeUpRequestStatus.DECLINED -> {
                            Text(
                                text = "You declined this request.",
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                color = MutedWineText,
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text("Go Back", color = DeepWinePrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        WakeUpRequestStatus.CANCELLED -> {
                            Text(
                                text = "${request.senderDisplayName} cancelled this wake-up request.",
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                color = MutedWineText,
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text("Go Back", color = DeepWinePrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        WakeUpRequestStatus.EXPIRED -> {
                            Text(
                                text = "This wake-up request has expired.",
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                color = MutedWineText,
                            )
                            Spacer(Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text("Go Back", color = DeepWinePrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
