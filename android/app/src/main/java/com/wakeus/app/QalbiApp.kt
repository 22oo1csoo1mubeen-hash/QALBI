package com.wakeus.app

import android.app.Application

/**
 * QALBI Application class.
 * Currently used for application-level initialization.
 * In later phases this will bootstrap Firebase and DI containers.
 */
class QalbiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create the alarm notification channel before the first incoming message.
        com.wakeus.app.data.fcm.NotificationHelper.createNotificationChannel(this)
        // Initialize real-time network connectivity monitor
        com.wakeus.app.data.network.NetworkMonitor.initialize(this)
    }
}
