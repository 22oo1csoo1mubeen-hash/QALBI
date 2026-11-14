package com.wakeus.app.ui.settings

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.wakeus.app.data.alarm.WakeUpAlarmService
import com.wakeus.app.data.fcm.AlarmDiagnosticEvent
import com.wakeus.app.data.fcm.AlarmDiagnostics
import com.wakeus.app.data.fcm.NotificationHelper

private val ReliabilityInk = Color(0xFF301019)
private val ReliabilityMuted = Color(0xFF7C5560)
private val ReliabilityAccent = Color(0xFF8B253E)
private val ReliabilityCard = Color(0xFFFFF9FA)
private val ReliabilityBorder = Color(0xFFF3D4DC)

data class ReliabilityCheck(val name: String, val status: String)

object AlarmReliabilityDiagnostics {
    fun collect(context: Context): List<ReliabilityCheck> {
        val app = context.applicationContext
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationsEnabled = runCatching { NotificationManagerCompat.from(app).areNotificationsEnabled() }.getOrNull()
        val channelStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.getNotificationChannel(NotificationHelper.CHANNEL_ID)?.importance?.let { importance ->
                when {
                    importance == NotificationManager.IMPORTANCE_NONE -> "Disabled"
                    importance < NotificationManager.IMPORTANCE_HIGH -> "Needs attention (importance below high)"
                    else -> "Enabled (high importance)"
                }
            } ?: "Unable to verify on this device"
        } else "Enabled (platform defaults)"
        val fullScreen = if (Build.VERSION.SDK_INT >= 34) {
            runCatching { notificationManager?.canUseFullScreenIntent() }.getOrNull()
        } else true
        val battery = runCatching {
            val power = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            power.isIgnoringBatteryOptimizations(app.packageName)
        }.getOrNull()
        val backgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted }.getOrNull()
        } else false
        val backgroundData = runCatching {
            val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            when (connectivity.restrictBackgroundStatus) {
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> "Restricted (Data Saver is limiting background data)"
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> "Not restricted by Data Saver"
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> "Not restricted by Data Saver"
                else -> "Unable to verify on this device"
            }
        }.getOrDefault("Unable to verify on this device")
        val serviceConfigured = runCatching {
            val serviceInfo = app.packageManager.getServiceInfo(ComponentName(app, WakeUpAlarmService::class.java), 0)
            val permissions = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
            val hasBasePermission = Manifest.permission.FOREGROUND_SERVICE in permissions
            val hasPlaybackPermission = Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK in permissions
            val hasPlaybackType = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                (serviceInfo.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0
            hasBasePermission && hasPlaybackPermission && hasPlaybackType
        }.getOrDefault(false)
        val tokenStatus = when (AlarmDiagnostics.tokenStatus(app)) {
            "registered" -> "Registered (last upload succeeded)"
            "needs_attention" -> "Needs attention (upload failed; retry is scheduled)"
            else -> "Unable to verify (no successful upload recorded yet)"
        }

        return listOf(
            ReliabilityCheck("Notifications", yesNo(notificationsEnabled, "Enabled", "Disabled")),
            ReliabilityCheck("Alarm notification channel", channelStatus),
            ReliabilityCheck("Full-screen alarm access", yesNo(fullScreen, "Available", "Not available")),
            ReliabilityCheck("Battery optimization", yesNo(battery, "Unrestricted", "Optimized")),
            ReliabilityCheck("Background app restriction", yesNo(backgroundRestricted?.not(), "Not restricted", "Restricted")),
            ReliabilityCheck("Background data", backgroundData),
            ReliabilityCheck("FCM token registration", tokenStatus),
            ReliabilityCheck("Alarm service configuration", yesNo(serviceConfigured, "Ready", "Needs attention")),
        )
    }

    private fun yesNo(value: Boolean?, yes: String, no: String) = when (value) {
        true -> yes
        false -> no
        null -> "Unable to verify on this device"
    }
}

@Composable
fun BackgroundAlarmReliabilitySection() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val checks = remember(context, refresh) { AlarmReliabilityDiagnostics.collect(context) }
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val isMotorola = manufacturer.contains("motorola", ignoreCase = true) || Build.BRAND.contains("motorola", ignoreCase = true)

    SettingsSectionHeader(
        title = "Background Alarm Reliability",
        icon = Icons.Outlined.Cloud,
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ReliabilityCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ReliabilityBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Run Alarm Health Check",
                color = ReliabilityInk,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Checks local permissions and configuration. Android cannot confirm that FCM reached this device from settings alone.",
                color = ReliabilityMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            checks.forEach { check ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(check.name, color = ReliabilityInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(check.status, color = statusColor(check.status), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            val lastRegistration = AlarmDiagnostics.tokenRegisteredAt(context)
            Text(
                "Last successful FCM registration: ${lastRegistration?.let(::formatDiagnosticTime) ?: "Not recorded"}",
                color = ReliabilityMuted,
                fontSize = 11.sp,
            )
            Text(
                "Last alarm received: ${AlarmDiagnostics.lastEventTime(context, AlarmDiagnosticEvent.FCM_MESSAGE_RECEIVED)?.let(::formatDiagnosticTime) ?: "Not recorded"}",
                color = ReliabilityMuted,
                fontSize = 11.sp,
            )
            Text(
                "Last alarm service start: ${AlarmDiagnostics.lastEventTime(context, AlarmDiagnosticEvent.ALARM_SERVICE_STARTED)?.let(::formatDiagnosticTime) ?: "Not recorded"}",
                color = ReliabilityMuted,
                fontSize = 11.sp,
            )
            AlarmDiagnostics.lastFailure(context)?.let { failure ->
                Text("Last known failure: $failure", color = Color(0xFF9D253F), fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            ReliabilityButton("Run Alarm Health Check") { refresh++ }
            ReliabilityButton("Open notification settings") {
                openSettings(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
            ReliabilityButton("Open battery optimization settings") {
                openSettings(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            ReliabilityButton("Open app info") { openAppInfo(context) }
            ReliabilityButton("Full-screen alarm access") {
                if (Build.VERSION.SDK_INT >= 34) {
                    openSettings(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(android.net.Uri.parse("package:${context.packageName}")))
                } else {
                    openSettings(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            ReliabilityButton("Export diagnostic report") {
                val report = AlarmDiagnostics.exportReport(context, checks.map { it.name to it.status })
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "QALBI alarm diagnostics")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                val chooser = Intent.createChooser(sendIntent, "Export alarm diagnostics").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(chooser) }
            }
            if (isMotorola) {
                Spacer(Modifier.height(10.dp))
                Text("Motorola background guidance", color = ReliabilityInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "Menu names vary by Android version and Motorola software. Check Settings → Apps → QALBI → App battery usage and allow background use or choose unrestricted use if offered. Confirm background data and notifications are allowed. Test Battery Saver separately; leave global battery features unchanged unless you want to compare behavior.",
                    color = ReliabilityMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Force-stopping QALBI blocks Android background delivery until you launch it again. A powered-off phone cannot receive alarms. Delivery also depends on network access and the device's power policies.",
                color = ReliabilityMuted,
                fontSize = 11.sp,
            )
        }
    }
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun ReliabilityButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDECEF), contentColor = ReliabilityAccent),
    ) {
        Text(label, fontSize = 12.sp)
    }
}

private fun statusColor(value: String): Color = when {
    value.startsWith("Enabled") || value.startsWith("Available") || value.startsWith("Unrestricted") ||
        value.startsWith("Not restricted") || value.startsWith("Registered") || value == "Ready" -> Color(0xFF23764A)
    value.startsWith("Disabled") || value.startsWith("Restricted") || value.startsWith("Not available") ||
        value.startsWith("Needs attention") || value == "Optimized" -> Color(0xFF9D253F)
    else -> ReliabilityMuted
}

private fun openSettings(context: Context, intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppInfo(context)
    }
}

private fun openAppInfo(context: Context) {
    openSettings(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")))
}

private fun formatDiagnosticTime(epochMillis: Long): String = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(epochMillis))
