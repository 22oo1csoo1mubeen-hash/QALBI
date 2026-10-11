package com.wakeus.app.ui.friends

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
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.ui.components.DemoBadge
import com.wakeus.app.ui.components.EmptyState
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold

@Composable
fun ConnectedFriendsScreen(
    onBack: () -> Unit,
    onFriendSelected: (String) -> Unit,
) {
    // Phase 1: friends list is always empty. Demo friend is accessible
    // only via explicit demo action on the Home screen.
    val friends = emptyList<Friend>()

    Scaffold(
        containerColor = MidnightBlue,
        topBar         = { QalbiTopBar(title = "Connected Friends", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            DemoBadge()
            Spacer(Modifier.height(24.dp))

            if (friends.isEmpty()) {
                QalbiCard(modifier = Modifier.fillMaxWidth(), innerPadding = 0.dp) {
                    EmptyState(
                        icon  = Icons.Filled.GroupOff,
                        title = "No connected friends",
                        body  = "Exchange connection codes with a friend to see them here. " +
                                "Real pairing arrives in Phase 3.",
                    )
                }
            } else {
                friends.forEach { friend ->
                    FriendListItem(
                        friend   = friend,
                        onClick  = { onFriendSelected(friend.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }
            }

            // Demo preview item — clearly labelled
            Spacer(Modifier.height(24.dp))
            Text(
                text  = "Demo Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FriendListItem(
                friend   = QalbiRepository.demoFriend,
                onClick  = { onFriendSelected(QalbiRepository.demoFriend.id) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text      = "↑ Tap to preview the Wake-Up screen with demo data",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FriendListItem(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QalbiCard(
        modifier     = modifier.clickable(onClick = onClick),
        innerPadding = 16.dp,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with initials
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MutedRose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = friend.initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightBlue,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text  = friend.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (friend.isDemo) {
                            Text(
                                text  = "[Demo] ",
                                style = MaterialTheme.typography.labelSmall,
                                color = SubtleGold,
                            )
                        }
                        Text(
                            text  = friend.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
