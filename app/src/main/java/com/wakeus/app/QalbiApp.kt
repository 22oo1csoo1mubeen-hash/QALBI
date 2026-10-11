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
        // Phase 4: Create high-priority notification channels for wake-up requests
        com.wakeus.app.data.fcm.NotificationHelper.createNotificationChannel(this)
    }
}
