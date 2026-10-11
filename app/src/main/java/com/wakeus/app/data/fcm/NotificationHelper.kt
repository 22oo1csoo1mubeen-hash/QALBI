package com.wakeus.app.data.fcm

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
import com.wakeus.app.R

object NotificationHelper {

    const val CHANNEL_ID = "qalbi_wake_up_channel"
    const val CHANNEL_NAME = "Wake-Up Requests"
    const val CHANNEL_DESCRIPTION = "High-priority alerts when connected friends request to wake you up."

    const val EXTRA_REQUEST_ID = "extra_request_id"
    const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    const val ACTION_NAVIGATE_WAKEUP = "com.wakeus.app.ACTION_NAVIGATE_WAKEUP"

    /**
     * Initializes the high-priority notification channel.
     * Safe to call on every app startup.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a high-priority system notification for an incoming wake-up request.
     */
    fun showWakeUpNotification(
        context: Context,
        requestId: String,
        senderDisplayName: String,
        senderUsername: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_NAVIGATE_WAKEUP
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_NAVIGATE_TO, "incoming_request")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("\u2600\uFE0F Wake Up from $senderDisplayName")
            .setContentText("@$senderUsername wants to wake you up! Tap to respond.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(requestId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission missing or revoked on Android 13+
        }
    }

    /**
     * Dismisses the notification for a specific wake-up request.
     */
    fun cancelNotification(context: Context, requestId: String) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(requestId.hashCode())
    }
}
