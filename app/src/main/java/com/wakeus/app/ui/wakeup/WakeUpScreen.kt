package com.wakeus.app.ui.wakeup

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiOutlinedButton
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold
import com.wakeus.app.viewmodel.WakeUpViewModel

@Composable
fun WakeUpScreen(
    friendId: String,
    onBack: () -> Unit,
    viewModel: WakeUpViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(friendId) {
        viewModel.loadFriend(friendId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        containerColor = MidnightBlue,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = { QalbiTopBar(title = "Wake Up", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val friend = uiState.friend

            if (friend == null) {
                CircularProgressIndicator(color = SubtleGold)
                Spacer(Modifier.height(16.dp))
                Text(
                    text  = "Loading friend details…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // ── Friend avatar ──────────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MutedRose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = friend.initials,
                        style = MaterialTheme.typography.displaySmall,
                        color = MidnightBlue,
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    text  = friend.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text  = "@${friend.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(32.dp))

                // ── Dynamic State-based UI ─────────────────────────────────
                when (uiState.wakeUpState) {
                    WakeUpState.IDLE -> {
                        QalbiButton(
                            text      = "Wake Up ${friend.displayName.split(" ").first()}",
                            onClick   = viewModel::requestWakeUp,
                            isLoading = uiState.isSubmitting,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }

                    WakeUpState.PENDING -> {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(54.dp),
                            color       = SubtleGold,
                            strokeWidth = 3.dp,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text      = "Waiting for ${friend.displayName} to respond…",
                            style     = MaterialTheme.typography.titleMedium,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text      = "Request is active in Firestore. Valid for 15 minutes.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(28.dp))

                        QalbiOutlinedButton(
                            text      = "Cancel Request",
                            onClick   = viewModel::cancelActiveRequest,
                            isLoading = uiState.isCancelling,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }

                    WakeUpState.ACKNOWLEDGED -> {
                        Icon(
                            imageVector        = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint               = SubtleGold,
                            modifier           = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = "${friend.displayName} is awake! ☀️❤️",
                            style     = MaterialTheme.typography.headlineSmall,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text      = "Your loved one acknowledged the wake-up request.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(32.dp))

                        QalbiButton(
                            text     = "Done",
                            onClick  = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WakeUpState.DECLINED -> {
                        Icon(
                            imageVector        = Icons.Filled.Close,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = "${friend.displayName} declined the request",
                            style     = MaterialTheme.typography.titleMedium,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        QalbiOutlinedButton(
                            text     = "Try Again Later",
                            onClick  = viewModel::resetWakeUp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WakeUpState.CANCELLED -> {
                        Text(
                            text      = "Wake-up request was cancelled.",
                            style     = MaterialTheme.typography.titleMedium,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        QalbiOutlinedButton(
                            text     = "Send New Request",
                            onClick  = viewModel::resetWakeUp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WakeUpState.EXPIRED -> {
                        Icon(
                            imageVector        = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = "Request expired (15-minute limit)",
                            style     = MaterialTheme.typography.titleMedium,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        QalbiOutlinedButton(
                            text     = "Send Another Request",
                            onClick  = viewModel::resetWakeUp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    WakeUpState.SENT_DEMO,
                    WakeUpState.UNAVAILABLE -> {
                        QalbiOutlinedButton(
                            text     = "Try Again",
                            onClick  = viewModel::resetWakeUp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // ── Confirmation dialog ────────────────────────────────────────────
        if (uiState.showConfirmDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelWakeUp,
                icon = {
                    Icon(
                        imageVector        = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint               = SubtleGold,
                    )
                },
                title = { Text("Send Wake-Up Request?") },
                text  = {
                    Text(
                        "Send an authentic wake-up request to ${uiState.friend?.displayName}? " +
                        "This will notify their device and await their response.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmWakeUp) {
                        Text("Send Request", color = SubtleGold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelWakeUp) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
