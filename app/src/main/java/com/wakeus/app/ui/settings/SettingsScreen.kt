package com.wakeus.app.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wakeus.app.R
import com.wakeus.app.data.alarm.AlarmPreferences
import com.wakeus.app.data.alarm.AlarmSoundPlayer
import com.wakeus.app.data.alarm.AlarmVibrator
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var vibrationEnabled by remember { mutableStateOf(AlarmPreferences.isVibrationEnabled(context)) }
    var vibrationPattern by remember { mutableStateOf(AlarmPreferences.getVibrationPattern(context)) }
    var gentleVolumeRamp by remember { mutableStateOf(AlarmPreferences.isGentleVolumeRamp(context)) }
    var isPreviewingSound by remember { mutableStateOf(false) }

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
            Spacer(Modifier.height(12.dp))

            // ── Alarm & Audio section ─────────────────────────────────────────
            SettingsSectionHeader(title = "Alarm & Audio")

            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                // Sound Preview Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPreviewingSound) {
                                AlarmSoundPlayer.stop()
                                isPreviewingSound = false
                            } else {
                                val started = AlarmSoundPlayer.preview(context) {
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
                            text = if (isPreviewingSound) "Playing chime preview… Tap to stop" else "Tap to test bundled alarm chime",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPreviewingSound) SubtleGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )

                // Gentle Volume Ramp-Up Toggle
                SettingsRowToggle(
                    icon    = Icons.AutoMirrored.Filled.VolumeUp,
                    title   = "Gentle Volume Ramp-Up",
                    subtitle = "Starts at 15% volume and smoothly escalates over 15 seconds",
                    checked = gentleVolumeRamp,
                    onCheckedChange = {
                        gentleVolumeRamp = it
                        AlarmPreferences.setGentleVolumeRamp(context, it)
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )

                // Vibration Toggle
                SettingsRowToggle(
                    icon    = Icons.Filled.Vibration,
                    title   = "Alarm Vibration",
                    subtitle = "Rhythmic haptic pulsation during incoming wake-up calls",
                    checked = vibrationEnabled,
                    onCheckedChange = {
                        vibrationEnabled = it
                        AlarmPreferences.setVibrationEnabled(context, it)
                    },
                )

                if (vibrationEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Vibration Pattern (tap to preview)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            AlarmPreferences.PATTERN_CADENCE to "Cadence",
                            AlarmPreferences.PATTERN_HEARTBEAT to "Heartbeat",
                            AlarmPreferences.PATTERN_GENTLE to "Gentle",
                        ).forEach { (id, label) ->
                            val isSelected = vibrationPattern.equals(id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SubtleGold else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        vibrationPattern = id
                                        AlarmPreferences.setVibrationPattern(context, id)
                                        AlarmVibrator.preview(context, id)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MidnightBlue else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )

                SettingsRowPlaceholder(
                    icon  = Icons.Filled.AlarmOff,
                    title = "Alarm Auto-Dismiss",
                    value = "15 minutes maximum validity before auto-expiry",
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Notifications section ─────────────────────────────────────────
            SettingsSectionHeader(title = "Notifications & Lockscreen")
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.NotificationsActive,
                    title = "Full-Screen Wake Alert",
                    value = "Rings over keyguard lockscreen using foreground media playback",
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Privacy & About section ───────────────────────────────────────
            SettingsSectionHeader(title = "Privacy & Architecture")
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.Lock,
                    title = "Private Friend Pairing",
                    value = "No mobile numbers required. Single-use 8-character Crockford Base32 codes.",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.CloudDone,
                    title = "Zero-Cost Cloud Architecture",
                    value = "Operates strictly under the free Firebase Spark tier. ₹0 budget.",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color    = MaterialTheme.colorScheme.outline,
                )
                SettingsRowPlaceholder(
                    icon  = Icons.Filled.Info,
                    title = stringResource(R.string.about_qalbi),
                    value = "v1.0.0 (Release Candidate)",
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
    subtitle: String? = null,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text  = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
