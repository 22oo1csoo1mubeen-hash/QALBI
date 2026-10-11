package com.wakeus.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.ui.components.DemoBadge
import com.wakeus.app.ui.components.EmptyState
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.SubtleGold

@Composable
fun HomeScreen(
    onConnectFriend: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onFriendSelected: (String) -> Unit,
    onConnectedFriends: () -> Unit,
    onProfile: () -> Unit,
) {
    val user = QalbiRepository.demoUser

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
            Spacer(Modifier.height(4.dp))

            // Demo badge
            DemoBadge()
            Spacer(Modifier.height(16.dp))

            // Personalized greeting
            Text(
                text  = "Good morning, ${user.displayName.split(" ").first()} \uD83C\uDF05",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "You have no wake-up requests today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                            text  = "Share a code to pair with someone you love.",
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
                Text(
                    text     = "See all",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onConnectedFriends),
                )
            }
            Spacer(Modifier.height(12.dp))

            // Empty state — Phase 1 has no connected friends
            QalbiCard(modifier = Modifier.fillMaxWidth(), innerPadding = 0.dp) {
                EmptyState(
                    icon  = Icons.Filled.WifiOff,
                    title = "No connected friends yet",
                    body  = "Once you and a friend exchange connection codes, they'll appear here.",
                )
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
                    text  = "In Phase 3, you'll generate a unique code and share it with a friend. " +
                            "They enter your code on their device, and you're paired — no phone numbers exchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
