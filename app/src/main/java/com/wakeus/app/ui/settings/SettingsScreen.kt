package com.wakeus.app.ui.settings

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.wakeus.app.R
import com.wakeus.app.data.alarm.AlarmPreferences
import com.wakeus.app.data.alarm.AlarmSoundPlayer
import com.wakeus.app.data.alarm.AlarmVibrator

private val ScreenBgFallback = Color(0xFFFBF6F3)
private val DeepWineTitle = Color(0xFF4A1424)
private val DeepWinePrimary = Color(0xFF8B253E)
private val DarkWineText = Color(0xFF301019)
private val MutedWineText = Color(0xFF7C5560)
private val ChevronTint = Color(0xFFB88C95)
private val CardBg = Color(0xFFFFF9FA)
private val CardBorderColor = Color(0xFFF3D4DC)
private val CardDividerColor = Color(0xFFF6E7EB)
private val IconContainerBg = Color(0xFFFDECEF)
private val SwitchWineTrack = Color(0xFF80243C)
private val SwitchOffTrack = Color(0xFFE5D2D6)
private val LogoutGradientTop = Color(0xFFBF3455)
private val LogoutGradientBottom = Color(0xFF781830)

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPreviewingSound) {
                AlarmSoundPlayer.stop()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgFallback)
    ) {
        // Mandatory shared background asset
        Image(
            painter = painterResource(id = R.drawable.overall_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar matching settings-reference.png
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepWinePrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = "Settings",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DeepWineTitle,
                )
            }

            // Scrollable Settings Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(10.dp))

                // ── SECTION 1: Alarm & Audio ─────────────────────────────────────────
                SettingsSectionHeader(
                    icon = Icons.Filled.MusicNote,
                    title = "Alarm & Audio",
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        // Row 1: Acoustic Chime (QALBI Original)
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(
                                icon = if (isPreviewingSound) Icons.Filled.NotificationsOff else Icons.Filled.MusicNote
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Acoustic Chime (QALBI Original)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isPreviewingSound) "Playing chime preview… Tap to stop" else "Tap to test bundled alarm chime",
                                    fontSize = 12.sp,
                                    color = if (isPreviewingSound) DeepWinePrimary else MutedWineText,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = ChevronTint,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                            color = CardDividerColor,
                            thickness = 1.dp,
                        )

                        // Row 2: Gentle Volume Ramp-Up
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.AutoMirrored.Outlined.VolumeUp)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gentle Volume Ramp-Up",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Starts at 15% volume and smoothly escalates over 15 seconds",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                            Switch(
                                checked = gentleVolumeRamp,
                                onCheckedChange = {
                                    gentleVolumeRamp = it
                                    AlarmPreferences.setGentleVolumeRamp(context, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SwitchWineTrack,
                                    checkedBorderColor = Color.Transparent,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = SwitchOffTrack,
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                            color = CardDividerColor,
                            thickness = 1.dp,
                        )

                        // Row 3: Alarm Vibration
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.Outlined.Vibration)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alarm Vibration",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Rhythmic haptic pulsation during incoming wake-up calls",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = {
                                    vibrationEnabled = it
                                    AlarmPreferences.setVibrationEnabled(context, it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SwitchWineTrack,
                                    checkedBorderColor = Color.Transparent,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = SwitchOffTrack,
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                            color = CardDividerColor,
                            thickness = 1.dp,
                        )

                        // Row 4: Vibration Pattern (tap to preview)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.Outlined.GraphicEq)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Vibration Pattern",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = DarkWineText,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "(tap to preview)",
                                        fontSize = 12.sp,
                                        color = MutedWineText,
                                    )
                                }
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
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (isSelected) SwitchWineTrack else Color(0xFFFCEDF0)
                                                )
                                                .clickable {
                                                    vibrationPattern = id
                                                    AlarmPreferences.setVibrationPattern(context, id)
                                                    AlarmVibrator.preview(context, id)
                                                }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isSelected) Color.White else DeepWinePrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                            color = CardDividerColor,
                            thickness = 1.dp,
                        )

                        // Row 5: Alarm Auto-Dismiss
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.Outlined.AlarmOff)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alarm Auto-Dismiss",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "15 minutes maximum validity before auto-expiry",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ── SECTION 2: Notifications & Lockscreen ─────────────────────────────
                SettingsSectionHeader(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications & Lockscreen",
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsIconBadge(icon = Icons.Filled.NotificationsActive)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Full-Screen Wake Alert",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = DarkWineText,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Rings over keyguard lockscreen using foreground media playback",
                                fontSize = 12.sp,
                                color = MutedWineText,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null,
                            tint = ChevronTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ── SECTION 3: Privacy & Architecture ─────────────────────────────────
                SettingsSectionHeader(
                    icon = Icons.Outlined.Shield,
                    title = "Privacy & Architecture",
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        // Row 1: Private Friend Pairing
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.Outlined.Lock)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Private Friend Pairing",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "No mobile numbers required. Single-use 8-character Crockford Base32 codes.",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = ChevronTint,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                            color = CardDividerColor,
                            thickness = 1.dp,
                        )

                        // Row 2: Zero-Cost Cloud Architecture
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.Outlined.Cloud)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Zero-Cost Cloud Architecture",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Operates strictly under the free Firebase Spark tier. ₹0 budget.",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = ChevronTint,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                            color = CardDividerColor,
                            thickness = 1.dp,
                        )

                        // Row 3: About QALBI
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsIconBadge(icon = Icons.Outlined.Info)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "About QALBI",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = DarkWineText,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "v1.0.0 (Release Candidate)",
                                    fontSize = 12.sp,
                                    color = MutedWineText,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = ChevronTint,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Log Out Button matching settings-reference.png
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(27.dp),
                            ambientColor = Color(0x35661226),
                            spotColor = Color(0x45661226),
                        ),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(LogoutGradientTop, LogoutGradientBottom),
                            ),
                            shape = RoundedCornerShape(27.dp),
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Log Out",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepWinePrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = DeepWineTitle,
        )
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IconContainerBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepWinePrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
