package com.wakeus.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.ui.components.EmptyState
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold

@Composable
fun HomeScreen(
    onConnectFriend: () -> Unit,
    onFriendSelected: (String) -> Unit,
    onConnectedFriends: () -> Unit,
    onProfile: () -> Unit,
    onIncomingRequest: (String) -> Unit = {},
) {
    val authState by QalbiRepository.authState.collectAsState()
    val user = (authState as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: QalbiRepository.demoUser
    val firstName = user.displayName.trim().split(" ").firstOrNull()?.ifEmpty { "there" } ?: "there"
    val friends by QalbiRepository.friends.collectAsState(initial = emptyList())
    val incomingRequests by QalbiRepository.incomingWakeUpRequests.collectAsState(initial = emptyList())
    val isOnline by com.wakeus.app.data.network.NetworkMonitor.isOnline.collectAsState()

    Scaffold(
        containerColor = MidnightBlue,
        topBar         = {
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                // App logo mark
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint               = SubtleGold,
                        modifier           = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text  = "QALBI",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                // Profile icon
                IconButton(onClick = onProfile) {
                    Icon(
                        imageVector        = Icons.Filled.AccountCircle,
                        contentDescription = "Profile",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // Offline network banner
            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Filled.WifiOff,
                            contentDescription = "Offline",
                            tint               = MaterialTheme.colorScheme.onErrorContainer,
                            modifier           = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = "No internet connection. Requests and status will sync once reconnected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Personalized greeting
            Text(
                text  = "Good morning, $firstName \uD83C\uDF05",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = if (friends.isNotEmpty()) "You have ${friends.size} connected friend${if (friends.size > 1) "s" else ""}." else "Connect with a loved one to start waking each other.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Incoming Wake-Up Alert (if ringing or active requests) ───────
            val alarmState by com.wakeus.app.data.alarm.WakeUpAlarmManager.alarmState.collectAsState()
            val activeReq = incomingRequests.firstOrNull() ?: if (alarmState.isRinging && alarmState.requestId != null) {
                // If alarm is ringing for a request, construct fallback summary
                com.wakeus.app.data.model.WakeUpRequest(
                    requestId = alarmState.requestId ?: "",
                    senderUid = alarmState.senderUid ?: "",
                    senderDisplayName = alarmState.senderDisplayName ?: "Your friend",
                    senderUsername = alarmState.senderUsername ?: "friend",
                    receiverUid = user.id,
                    receiverDisplayName = user.displayName,
                    receiverUsername = user.username,
                    connectionId = "",
                )
            } else null

            activeReq?.let { req ->
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (alarmState.isRinging) MutedRose.copy(alpha = 0.28f) else SubtleGold.copy(alpha = 0.18f))
                        .clickable { onIncomingRequest(req.requestId) }
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = if (alarmState.isRinging) Icons.Filled.Alarm else Icons.Filled.Favorite,
                            contentDescription = null,
                            tint               = SubtleGold,
                            modifier           = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = "${req.senderDisplayName} is waking you up! ☀️",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text  = if (alarmState.isRinging) "🔔 ALARM RINGING — Tap to respond: I'm Awake!" else "Tap here to answer: I'm Awake!",
                                style = MaterialTheme.typography.bodySmall,
                                color = SubtleGold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Connect CTA card ─────────────────────────────────────────────
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Filled.GroupAdd,
                        contentDescription = null,
                        tint               = SubtleGold,
                        modifier           = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "Connect to a Friend",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text  = "Share a temporary code to pair without exchanging phone numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                QalbiButton(text = "Connect to a Friend", onClick = onConnectFriend)
            }

            Spacer(Modifier.height(24.dp))

            // ── Connected Friends section ─────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "Connected Friends",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (friends.isNotEmpty()) {
                    Text(
                        text     = "See all",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onConnectedFriends),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (friends.isEmpty()) {
                QalbiCard(modifier = Modifier.fillMaxWidth(), innerPadding = 0.dp) {
                    EmptyState(
                        icon  = Icons.Filled.WifiOff,
                        title = "No connected friends yet",
                        body  = "Exchange a 10-minute pairing code with a friend to establish an instant connection.",
                    )
                }
            } else {
                friends.take(3).forEach { friend ->
                    HomeFriendItem(
                        friend  = friend,
                        onClick = { onFriendSelected(friend.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── How it works note ─────────────────────────────────────────────
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = "How connection codes work",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "Generate an 8-character code and share it privately. When your friend enters the code within 10 minutes, both accounts connect automatically. Codes are single-use, private, and secure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HomeFriendItem(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QalbiCard(
        modifier     = modifier.clickable(onClick = onClick),
        innerPadding = 16.dp,
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val avatarColor = androidx.compose.ui.graphics.Color(
                com.wakeus.app.data.model.AvatarPreset.getColor(friend.avatarStyle)
            )

            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = friend.initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MidnightBlue,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = friend.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = "@${friend.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = friend.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtleGold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}
