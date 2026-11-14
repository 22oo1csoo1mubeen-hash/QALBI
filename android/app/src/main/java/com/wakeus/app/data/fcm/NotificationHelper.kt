package com.wakeus.app.data.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wakeus.app.MainActivity
import com.wakeus.app.data.alarm.WakeUpAlarmService

object NotificationHelper {

    const val CHANNEL_ID = "qalbi_wake_up_channel"
    const val CHANNEL_NAME = "Wake-Up Requests"
    const val CHANNEL_DESCRIPTION = "High-priority alerts when connected friends request to wake you up."
    const val STATUS_CHANNEL_ID = "qalbi_schedule_updates"
    const val STATUS_CHANNEL_NAME = "Scheduled Wake-Up Updates"
    const val EXTRA_SCHEDULED_REQUEST_ID = "extra_scheduled_request_id"
    const val EXTRA_FRIEND_ID = "extra_friend_id"

    const val EXTRA_REQUEST_ID = "extra_request_id"
    const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    const val ACTION_NAVIGATE_WAKEUP = "com.wakeus.app.ACTION_NAVIGATE_WAKEUP"

    /**
     * Initializes the high-priority notification channel with alarm audio attributes.
     * Safe to call on every app startup.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(
                NotificationChannel(
                    STATUS_CHANNEL_ID,
                    STATUS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Updates when a scheduled wake-up is sent or acknowledged."
                    enableVibration(true)
                },
            )
        }
    }

    /** Posts sender-side status updates without using the recipient's alarm-ringing channel. */
    fun showScheduledAlarmStatusNotification(
        context: Context,
        requestId: String,
        friendId: String,
        friendDisplayName: String,
        acknowledged: Boolean,
    ) {
        if (requestId.isBlank() || friendId.isBlank()) return
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SCHEDULED_REQUEST_ID, requestId)
            putExtra(EXTRA_FRIEND_ID, friendId)
        }
        val notificationKey = if (acknowledged) "${requestId}_acknowledged" else "${requestId}_sent"
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (acknowledged) "${friendDisplayName} acknowledged your wake-up" else "Scheduled wake-up sent"
        val text = if (acknowledged) {
            "$friendDisplayName responded to your scheduled wake-up."
        } else {
            "Your scheduled wake-up was sent to $friendDisplayName. Waiting for their response."
        }
        val notification = NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationKey.hashCode(), notification)
        } catch (_: SecurityException) {
            // Android 13+ can block status notifications when notification permission is denied.
        }
    }

    /**
     * Builds the persistent foreground alarm notification with full-screen intent and action buttons.
     */
    fun buildForegroundAlarmNotification(
        context: Context,
        requestId: String,
        senderDisplayName: String,
        senderUsername: String,
        customMessage: String = "",
    ): Notification {
        createNotificationChannel(context)

        // 1. Content and Full-Screen Intent (opens incoming alarm screen over lockscreen)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = ACTION_NAVIGATE_WAKEUP
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_NAVIGATE_TO, "incoming_request")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action PendingIntent: I'M AWAKE!
        val awakeIntent = Intent(context, WakeUpAlarmService::class.java).apply {
            action = WakeUpAlarmService.ACTION_ACKNOWLEDGE_ALARM
            putExtra(EXTRA_REQUEST_ID, requestId)
        }
        val awakePendingIntent = PendingIntent.getService(
            context,
            (requestId + "_awake").hashCode(),
            awakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action PendingIntent: DECLINE
        val declineIntent = Intent(context, WakeUpAlarmService::class.java).apply {
            action = WakeUpAlarmService.ACTION_DECLINE_ALARM
            putExtra(EXTRA_REQUEST_ID, requestId)
        }
        val declinePendingIntent = PendingIntent.getService(
            context,
            (requestId + "_decline").hashCode(),
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeCustomMessage = customMessage
            .trim()
            .take(160)
        val notificationText = safeCustomMessage.ifBlank {
            "@$senderUsername is waking you up! Tap to respond."
        }
        val expandedNotificationText = if (safeCustomMessage.isBlank()) {
            notificationText
        } else {
            "@$senderUsername is waking you up!\n\n\"$safeCustomMessage\"\n\nTap to respond."
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("☀️ Wake Up from $senderDisplayName")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedNotificationText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "☀️ I'M AWAKE!",
                awakePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "DECLINE",
                declinePendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Posts a high-priority system notification for an incoming wake-up request.
     */
    fun showWakeUpNotification(
        context: Context,
        requestId: String,
        senderDisplayName: String,
        senderUsername: String,
        customMessage: String = "",
    ) {
        createNotificationChannel(context)

        val notification = buildForegroundAlarmNotification(
            context = context,
            requestId = requestId,
            senderDisplayName = senderDisplayName,
            senderUsername = senderUsername,
            customMessage = customMessage,
        )

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(requestId.hashCode(), notification)
            AlarmDiagnostics.record(context, AlarmDiagnosticEvent.ALARM_NOTIFICATION_SUBMITTED, requestId)
        } catch (_: SecurityException) {
            // Permission missing or revoked on Android 13+
            AlarmDiagnostics.record(context, AlarmDiagnosticEvent.ALARM_NOTIFICATION_BLOCKED, requestId)
        }
    }

    /**
     * Dismisses the notification for a specific wake-up request.
     */
    fun cancelNotification(context: Context, requestId: String) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(requestId.hashCode())
        manager.cancel(WakeUpAlarmService.NOTIFICATION_ID)
    }
}
