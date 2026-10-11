package com.wakeus.app.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.data.model.Friend
import com.wakeus.app.ui.components.EmptyState
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold
import com.wakeus.app.viewmodel.FriendsViewModel

@Composable
fun ConnectedFriendsScreen(
    onBack: () -> Unit,
    onFriendSelected: (String) -> Unit,
    viewModel: FriendsViewModel = viewModel(),
) {
    val friends by viewModel.friends.collectAsState()

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
            Spacer(Modifier.height(16.dp))

            if (friends.isEmpty()) {
                QalbiCard(modifier = Modifier.fillMaxWidth(), innerPadding = 0.dp) {
                    EmptyState(
                        icon  = Icons.Filled.GroupOff,
                        title = "No connected friends",
                        body  = "Exchange an 8-character connection code with a friend to pair and see them here.",
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
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MutedRose),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = friend.initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
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
