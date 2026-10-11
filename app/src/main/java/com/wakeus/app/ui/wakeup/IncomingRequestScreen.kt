package com.wakeus.app.ui.wakeup

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiOutlinedButton
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold
import com.wakeus.app.viewmodel.IncomingRequestViewModel

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

    // Pulsing animation for active ringing alarm
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
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

    Scaffold(
        containerColor = MidnightBlue,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = { QalbiTopBar(title = "Wake-Up Alert", onBack = onBack) },
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
                CircularProgressIndicator(color = SubtleGold)
                Spacer(Modifier.height(16.dp))
                Text(
                    text  = "Loading wake-up details…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (request == null) {
                Icon(
                    imageVector        = Icons.Filled.Close,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text  = "Wake-up request not found or has been removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                // If alarm is actively ringing, show pulsating alarm banner
                if (alarmState.isRinging) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MutedRose.copy(alpha = 0.2f))
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Filled.Alarm,
                                contentDescription = "Alarm Active",
                                tint               = SubtleGold,
                                modifier           = Modifier
                                    .size(24.dp)
                                    .scale(pulseScale)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text  = "ALARM RINGING",
                                style = MaterialTheme.typography.titleMedium,
                                color = SubtleGold,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Sender Avatar
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(MutedRose),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = request.senderDisplayName.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { request.senderUsername.take(2).uppercase() }

                    Text(
                        text  = initials,
                        style = MaterialTheme.typography.displaySmall,
                        color = MidnightBlue,
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text      = "${request.senderDisplayName} is waking you up! ☀️",
                    style     = MaterialTheme.typography.headlineMedium,
                    color     = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = "@${request.senderUsername}",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))

                // Request status card
                QalbiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint               = SubtleGold,
                            modifier           = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text  = "Status: ${request.status.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text  = if (request.isPending) "Awaiting your response" else "Request completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Actions based on request status
                when (request.status) {
                    WakeUpRequestStatus.PENDING -> {
                        if (request.isExpired) {
                            Text(
                                text  = "This request has expired.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            QalbiOutlinedButton(text = "Go Back", onClick = onBack)
                        } else {
                            // Primary high-contrast I'm Awake button
                            QalbiButton(
                                text      = "☀️ I'M AWAKE!",
                                onClick   = { viewModel.acknowledge(context, onDone = onBack) },
                                isLoading = uiState.isResponding || alarmState.isAcknowledging,
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            )

                            Spacer(Modifier.height(14.dp))

                            QalbiOutlinedButton(
                                text      = "Decline",
                                onClick   = { viewModel.decline(context, onDone = onBack) },
                                isLoading = uiState.isResponding,
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                            )
                        }
                    }
                    WakeUpRequestStatus.ACKNOWLEDGED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint               = SubtleGold,
                                modifier           = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text  = "You let them know you're awake! ❤️",
                                style = MaterialTheme.typography.titleMedium,
                                color = SubtleGold,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        QalbiOutlinedButton(text = "Done", onClick = onBack)
                    }
                    WakeUpRequestStatus.DECLINED -> {
                        Text(
                            text  = "You declined this request.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        QalbiOutlinedButton(text = "Go Back", onClick = onBack)
                    }
                    WakeUpRequestStatus.CANCELLED -> {
                        Text(
                            text  = "${request.senderDisplayName} cancelled this wake-up request.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        QalbiOutlinedButton(text = "Go Back", onClick = onBack)
                    }
                    WakeUpRequestStatus.EXPIRED -> {
                        Text(
                            text  = "This wake-up request has expired.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        QalbiOutlinedButton(text = "Go Back", onClick = onBack)
                    }
                }
            }
        }
    }
}
