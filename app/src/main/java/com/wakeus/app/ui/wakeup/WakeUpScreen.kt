package com.wakeus.app.ui.wakeup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.ui.components.DemoBadge
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

    LaunchedEffect(friendId) {
        viewModel.loadFriend(friendId)
    }

    Scaffold(
        containerColor = MidnightBlue,
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
            DemoBadge()
            Spacer(Modifier.height(8.dp))
            Text(
                text      = stringResource(R.string.phase1_notice),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            val friend = uiState.friend

            if (friend == null) {
                Text(
                    text  = "Friend not found.",
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
                if (friend.isDemo) {
                    Text(
                        text  = "[Demo Profile]",
                        style = MaterialTheme.typography.bodySmall,
                        color = SubtleGold,
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ── State-based UI ─────────────────────────────────────────
                when (uiState.wakeUpState) {
                    WakeUpState.IDLE -> {
                        QalbiButton(
                            text    = stringResource(R.string.wake_up),
                            onClick = viewModel::requestWakeUp,
                        )
                    }
                    WakeUpState.PENDING -> {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(48.dp),
                            color       = SubtleGold,
                            strokeWidth = 3.dp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text  = "Sending simulated wake-up…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WakeUpState.SENT_DEMO -> {
                        Icon(
                            imageVector        = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint               = SubtleGold,
                            modifier           = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text      = "Wake-up simulated (demo only)",
                            style     = MaterialTheme.typography.titleMedium,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text      = "No real notification was sent. This feature is live in Phase 4.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))

                        // "I'm Awake!" preview button
                        QalbiCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text  = "Preview — I'm Awake!",
                                style = MaterialTheme.typography.titleSmall,
                                color = SubtleGold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = "In Phase 4, the recipient sees this response option on their device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        QalbiOutlinedButton(
                            text    = stringResource(R.string.im_awake),
                            onClick = viewModel::resetWakeUp,
                        )
                    }
                    WakeUpState.UNAVAILABLE -> {
                        Icon(
                            imageVector        = Icons.Filled.WifiOff,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text  = "Friend is unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        QalbiOutlinedButton(text = "Try Again", onClick = viewModel::resetWakeUp)
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
                title = { Text("Send Wake-Up?") },
                text  = {
                    Text(
                        "This will simulate a wake-up request to ${uiState.friend?.displayName}. " +
                        "No real notification will be sent in Phase 1.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmWakeUp) {
                        Text("Confirm (Demo)", color = SubtleGold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelWakeUp) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
