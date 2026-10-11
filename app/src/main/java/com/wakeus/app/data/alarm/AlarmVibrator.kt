package com.wakeus.app.data.alarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Lifecycle-aware, safe vibration controller for wake-up alarms.
 * Uses modern VibratorManager on API 31+ with legacy Vibrator fallback.
 * Checks hardware capability and gracefully handles devices without vibration motors.
 */
object AlarmVibrator {

    private const val TAG = "AlarmVibrator"

    // Alarm vibration rhythm: 0ms delay, 800ms vibrate, 400ms pause, 800ms vibrate...
    private val VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 400, 800)

    private var activeVibrator: Vibrator? = null
    private var isVibrating = false

    @Synchronized
    fun start(context: Context): Boolean {
        if (isVibrating) {
            Log.d(TAG, "Vibration is already active. Skipping duplicate start.")
            return true
        }

        val appContext = context.applicationContext
        val vibrator = getVibrator(appContext)

        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.d(TAG, "Device does not possess vibration hardware. Skipping.")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 0 specifies the index at which to repeat (continuous loop)
                val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATION_PATTERN, 0)
            }

            activeVibrator = vibrator
            isVibrating = true
            Log.d(TAG, "Alarm vibration successfully started.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm vibration", e)
            false
        }
    }

    @Synchronized
    fun stop() {
        try {
            activeVibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling vibration", e)
        } finally {
            activeVibrator = null
            isVibrating = false
        }
    }

    val isActive: Boolean
        @Synchronized get() = isVibrating

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
