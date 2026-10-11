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
        // Phase 1: no third-party SDKs to initialize.
    }
}
