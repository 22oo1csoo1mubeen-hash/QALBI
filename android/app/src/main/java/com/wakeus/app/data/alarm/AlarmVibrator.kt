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

    // Default alarm vibration rhythm: 0ms delay, 800ms vibrate, 400ms pause, 800ms vibrate...
    private val DEFAULT_VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 400, 800)

    private var activeVibrator: Vibrator? = null
    private var isVibrating = false

    @Synchronized
    fun start(context: Context): Boolean {
        if (isVibrating) {
            Log.d(TAG, "Vibration is already active. Skipping duplicate start.")
            return true
        }

        val appContext = context.applicationContext

        // Check user preferences
        if (!AlarmPreferences.isVibrationEnabled(appContext)) {
            Log.d(TAG, "Alarm vibration disabled in settings. Skipping.")
            return false
        }

        val vibrator = getVibrator(appContext)

        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.d(TAG, "Device does not possess vibration hardware. Skipping.")
            return false
        }

        val patternName = AlarmPreferences.getVibrationPattern(appContext)
        val pattern = AlarmPreferences.getPatternWaveform(patternName)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 0 specifies the index at which to repeat (continuous loop)
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }

            activeVibrator = vibrator
            isVibrating = true
            Log.d(TAG, "Alarm vibration successfully started with pattern: $patternName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm vibration", e)
            false
        }
    }

    /**
     * Briefly vibrates to preview a chosen pattern in Settings.
     */
    @Synchronized
    fun preview(context: Context, patternName: String) {
        val appContext = context.applicationContext
        val vibrator = getVibrator(appContext) ?: return
        if (!vibrator.hasVibrator()) return

        stop()
        val pattern = AlarmPreferences.getPatternWaveform(patternName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // -1 indicates no repeat (one-shot waveform)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preview vibration pattern", e)
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
