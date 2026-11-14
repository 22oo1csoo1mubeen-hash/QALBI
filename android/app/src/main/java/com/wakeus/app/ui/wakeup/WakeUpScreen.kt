package com.wakeus.app.ui.wakeup

import android.app.Activity
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.ui.components.QalbiRomanticButton
import com.wakeus.app.viewmodel.WakeUpViewModel

private val ScreenBgFallback = Color(0xFFFBF6F3)
private val DeepWineTitle = Color(0xFF4A1424)
private val DeepWinePrimary = Color(0xFF8B253E)
private val DarkWineText = Color(0xFF301019)
private val MutedWineText = Color(0xFF7C5560)
private val CardBg = Color(0xFFFFF9FA)
private val CardBorderColor = Color(0xFFF3D4DC)

@Composable
fun WakeUpScreen(
    friendId: String,
    onBack: () -> Unit,
    viewModel: WakeUpViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
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

    LaunchedEffect(friendId) {
        viewModel.loadFriend(friendId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
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
                        text = "Wake Up",
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
                val friend = uiState.friend

                if (friend == null) {
                    CircularProgressIndicator(
                        color = DeepWinePrimary,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Loading friend details…",
                        fontSize = 14.sp,
                        color = MutedWineText,
                    )
                } else {
                    Spacer(Modifier.height(16.dp))

                    // ── Romantic Friend Avatar ──────────────────────────────────
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
                            Text(
                                text = friend.initials.ifBlank { "?" },
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

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = friend.displayName,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = DarkWineText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "@${friend.username}",
                        fontSize = 14.sp,
                        color = MutedWineText,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(28.dp))

                    // ── Dynamic State-based UI ─────────────────────────────────
                    when (uiState.wakeUpState) {
                        WakeUpState.IDLE -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = BorderStroke(1.dp, CardBorderColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFDECEF)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Alarm,
                                            contentDescription = null,
                                            tint = DeepWinePrimary,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    Text(
                                        text = "Morning Wake-Up Call",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp,
                                        color = DarkWineText,
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "Send an authentic wake-up call to ${friend.displayName.split(" ").first()}. Their phone will play the chime and notify them that you're waiting for them to wake up.",
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = MutedWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(28.dp))

                            QalbiRomanticButton(
                                text = "Wake Up ${friend.displayName.split(" ").first()}",
                                onClick = viewModel::requestWakeUp,
                                isLoading = uiState.isSubmitting,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Filled.Alarm,
                            )
                        }

                        WakeUpState.PENDING -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = BorderStroke(1.dp, CardBorderColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(52.dp),
                                        color = DeepWinePrimary,
                                        strokeWidth = 3.dp,
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    Text(
                                        text = "Waiting for ${friend.displayName} to respond…",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = DarkWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Request is active in Firestore. Valid for 15 minutes.",
                                        fontSize = 13.sp,
                                        color = MutedWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = viewModel::cancelActiveRequest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                                enabled = !uiState.isCancelling,
                            ) {
                                Text(
                                    text = if (uiState.isCancelling) "Cancelling…" else "Cancel Request",
                                    color = DeepWinePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        WakeUpState.ACKNOWLEDGED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = BorderStroke(1.dp, CardBorderColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFCEDF0)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(42.dp),
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Text(
                                        text = "${friend.displayName} is awake! ☀️❤️",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = DarkWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Your loved one acknowledged the wake-up request.",
                                        fontSize = 13.sp,
                                        color = MutedWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(28.dp))

                            QalbiRomanticButton(
                                text = "Done",
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        WakeUpState.DECLINED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = BorderStroke(1.dp, CardBorderColor),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFB00020),
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "${friend.displayName} is busy or declined",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp,
                                        color = DarkWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = viewModel::resetWakeUp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text(
                                    text = "Try Again Later",
                                    color = DeepWinePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        WakeUpState.CANCELLED -> {
                            Text(
                                text = "Wake-up request was cancelled.",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = DarkWineText,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = viewModel::resetWakeUp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text(
                                    text = "Send New Request",
                                    color = DeepWinePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        WakeUpState.EXPIRED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                border = BorderStroke(1.dp, CardBorderColor),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = MutedWineText,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Request expired (15-minute limit)",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp,
                                        color = DarkWineText,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = viewModel::resetWakeUp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text(
                                    text = "Send Another Request",
                                    color = DeepWinePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        WakeUpState.SENT_DEMO,
                        WakeUpState.UNAVAILABLE -> {
                            OutlinedButton(
                                onClick = viewModel::resetWakeUp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                            ) {
                                Text(
                                    text = "Try Again",
                                    color = DeepWinePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // ── Romantic Confirmation Dialog ────────────────────────────────────
        if (uiState.showConfirmDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelWakeUp,
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFFF9FA),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = DeepWinePrimary,
                        modifier = Modifier.size(32.dp),
                    )
                },
                title = {
                    Text(
                        text = "Send Wake-Up Request?",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = DarkWineText,
                    )
                },
                text = {
                    Text(
                        text = "Send an authentic wake-up request to ${uiState.friend?.displayName}? This will notify their device and await their response.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MutedWineText,
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmWakeUp) {
                        Text(
                            text = "Send Request",
                            color = DeepWinePrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelWakeUp) {
                        Text(
                            text = "Cancel",
                            color = MutedWineText,
                        )
                    }
                },
            )
        }
    }
}
