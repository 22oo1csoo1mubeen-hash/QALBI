package com.wakeus.app.data.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.wakeus.app.data.fcm.NotificationHelper

/**
 * Android Foreground Service responsible for continuous alarm playback, vibration,
 * and lockscreen full-screen notification presentation.
 */
class WakeUpAlarmService : Service() {

    companion object {
        const val TAG = "WakeUpAlarmService"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_ALARM = "com.wakeus.app.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.wakeus.app.action.STOP_ALARM"
        const val ACTION_ACKNOWLEDGE_ALARM = "com.wakeus.app.action.ACKNOWLEDGE_ALARM"
        const val ACTION_DECLINE_ALARM = "com.wakeus.app.action.DECLINE_ALARM"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val requestId = intent.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID) ?: ""
        val senderDisplayName = intent.getStringExtra("senderDisplayName") ?: "Your Friend"
        val senderUsername = intent.getStringExtra("senderUsername") ?: "friend"

        Log.d(TAG, "onStartCommand action=$action for requestId=$requestId")

        when (action) {
            ACTION_START_ALARM -> {
                // 1. Immediately present foreground notification (mandatory within 5s on Android 8+)
                val notification = NotificationHelper.buildForegroundAlarmNotification(
                    context = applicationContext,
                    requestId = requestId,
                    senderDisplayName = senderDisplayName,
                    senderUsername = senderUsername
                )
                startForeground(NOTIFICATION_ID, notification)

                // 2. Start looping audio & vibration hardware via WakeUpAlarmManager
                WakeUpAlarmManager.startLocalPlayback(applicationContext)
            }

            ACTION_ACKNOWLEDGE_ALARM -> {
                Log.d(TAG, "Notification action I'M AWAKE received.")
                if (requestId.isNotBlank()) {
                    WakeUpAlarmManager.acknowledgeAlarm(applicationContext, requestId)
                } else {
                    stopAlarm()
                }
            }

            ACTION_DECLINE_ALARM -> {
                Log.d(TAG, "Notification action DECLINE received.")
                if (requestId.isNotBlank()) {
                    WakeUpAlarmManager.declineAlarm(applicationContext, requestId)
                } else {
                    stopAlarm()
                }
            }

            ACTION_STOP_ALARM -> {
                stopAlarm()
            }
        }

        return START_NOT_STICKY
    }

    private fun stopAlarm() {
        Log.d(TAG, "Stopping WakeUpAlarmService and removing foreground notification.")
        WakeUpAlarmManager.stopHardware(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WakeUpAlarmService onDestroy triggered.")
        WakeUpAlarmManager.stopHardware(applicationContext)
    }
}
