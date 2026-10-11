package com.wakeus.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wakeus.app.R
import com.wakeus.app.ui.components.DemoBadge
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.SubtleGold

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    // Phase 1: settings state is in-memory only — not persisted.
    var vibrationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MidnightBlue,
        topBar         = { QalbiTopBar(title = stringResource(R.string.settings), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            DemoBadge()
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Settings do not persist in Phase 1. These are UI previews only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // ── Alarm Settings section ────────────────────────────────────────
            val context = androidx.compose.ui.platform.LocalContext.current
            var isPreviewingSound by remember { mutableStateOf(false) }

            SettingsSectionHeader(title = "Alarm & Audio")

            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPreviewingSound) {
                                com.wakeus.app.data.alarm.AlarmSoundPlayer.stop()
                                isPreviewingSound = false
                            } else {
                                val started = com.wakeus.app.data.alarm.AlarmSoundPlayer.preview(context) {
                                    isPreviewingSound = false
                                }
                                isPreviewingSound = started
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isPreviewingSound) Icons.Filled.NotificationsOff else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = SubtleGold,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acoustic Chime (QALBI Original)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isPreviewingSound) "Playing preview… Tap to stop" else "Tap to preview bundled alarm sound",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPreviewingSound) SubtleGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )
                SettingsRowToggle(
                    icon    = Icons.Filled.Vibration,
                    title   = "Vibration",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.AlarmOff,
                    title = "Alarm Duration",
                    value = "15 minutes maximum validity",
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Notifications section ─────────────────────────────────────────
            SettingsSectionHeader(title = "Notifications & Lockscreen")
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.MusicNote,
                    title = "Full-Screen Wake Alert",
                    value = "Shows over lockscreen when alarm is triggered",
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Privacy & About section ───────────────────────────────────────
            SettingsSectionHeader(title = "Privacy & About")
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.Lock,
                    title = "Privacy",
                    value = "No personal data leaves this device in Phase 1",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.Info,
                    title = stringResource(R.string.about_qalbi),
                    value = stringResource(R.string.version),
                )
            }

            Spacer(Modifier.height(28.dp))

            QalbiButton(
                text           = stringResource(R.string.logout),
                onClick        = onLogout,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = SubtleGold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SettingsRowPlaceholder(
    icon: ImageVector,
    title: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRowToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleSmall,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor  = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
