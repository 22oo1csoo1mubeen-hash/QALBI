package com.wakeus.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import com.wakeus.app.data.model.AvatarPreset
import com.wakeus.app.ui.theme.SubtleGold
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiOutlinedButton
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    val user = uiState.user
    val initials = (user?.displayName ?: "U")
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "U" }

    Scaffold(
        containerColor = MidnightBlue,
        topBar         = { QalbiTopBar(title = stringResource(R.string.profile), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            if (uiState.isLoading && user == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val avatarColor = androidx.compose.ui.graphics.Color(
                    com.wakeus.app.data.model.AvatarPreset.getColor(user?.avatarStyle)
                )

                // Avatar
                Box(
                    modifier         = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = initials,
                        style = MaterialTheme.typography.displaySmall,
                        color = MidnightBlue,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = user?.displayName ?: "User",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text  = "@${user?.username ?: "username"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(14.dp))

                // Avatar palette selector
                Text(
                    text  = "Avatar Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = SubtleGold,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    AvatarPreset.PRESETS.forEach { preset ->
                        val isSelected = (user?.avatarStyle ?: "ROSE").equals(preset.id, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(preset.colorHex))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.5.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable {
                                    viewModel.updateAvatarStyle(preset.id)
                                }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Profile details card
                QalbiCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileRow(label = "Display Name", value = user?.displayName ?: "—")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color    = MaterialTheme.colorScheme.outline,
                    )
                    ProfileRow(label = "Username", value = "@${user?.username ?: "—"}")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color    = MaterialTheme.colorScheme.outline,
                    )
                    ProfileRow(
                        label = "Account Identifier",
                        value = user?.email ?: "Username-only account (@${user?.username})"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color    = MaterialTheme.colorScheme.outline,
                    )
                    ProfileRow(
                        label = "Account Type",
                        value = user?.accountType?.name ?: "STANDARD"
                    )
                }

                Spacer(Modifier.height(24.dp))

                QalbiOutlinedButton(
                    text    = stringResource(R.string.settings),
                    onClick = onSettings,
                )
                Spacer(Modifier.height(12.dp))
                QalbiButton(
                    text    = stringResource(R.string.logout),
                    onClick = viewModel::logout,
                    isLoading = uiState.isLoading,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
