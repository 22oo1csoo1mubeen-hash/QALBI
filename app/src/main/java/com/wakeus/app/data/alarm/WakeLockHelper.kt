package com.wakeus.app.data.alarm

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Safe, guarded WakeLock utility.
 * Always enforces an upper-bound timeout to avoid battery draining if an alarm is abandoned.
 */
object WakeLockHelper {

    private const val TAG = "WakeLockHelper"
    private const val WAKE_LOCK_TAG = "qalbi:wake_up_alarm_lock"
    private const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes max timeout

    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        release() // Ensure prior lock is cleared

        try {
            val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            )?.apply {
                setReferenceCounted(false)
                acquire(timeoutMs)
            }
            Log.d(TAG, "Acquired WakeLock with ${timeoutMs / 1000}s timeout.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock", e)
        }
    }

    @Synchronized
    fun release() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Released WakeLock.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
    }
}
