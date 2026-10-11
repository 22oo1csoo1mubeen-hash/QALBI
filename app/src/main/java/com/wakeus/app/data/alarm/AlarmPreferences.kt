package com.wakeus.app.data.alarm

import android.content.Context
import android.content.SharedPreferences

/**
 * Persisted local preferences for alarm audio and vibration characteristics.
 * Zero-cost SharedPreferences storage with immediate in-memory fallbacks.
 */
object AlarmPreferences {

    private const val PREFS_NAME = "qalbi_alarm_preferences"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_VIBRATION_PATTERN = "vibration_pattern"
    private const val KEY_GENTLE_VOLUME_RAMP = "gentle_volume_ramp"

    const val PATTERN_CADENCE = "CADENCE"
    const val PATTERN_HEARTBEAT = "HEARTBEAT"
    const val PATTERN_GENTLE = "GENTLE"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun getVibrationPattern(context: Context): String {
        return getPrefs(context).getString(KEY_VIBRATION_PATTERN, PATTERN_CADENCE) ?: PATTERN_CADENCE
    }

    fun setVibrationPattern(context: Context, pattern: String) {
        getPrefs(context).edit().putString(KEY_VIBRATION_PATTERN, pattern).apply()
    }

    fun isGentleVolumeRamp(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_GENTLE_VOLUME_RAMP, false)
    }

    fun setGentleVolumeRamp(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_GENTLE_VOLUME_RAMP, enabled).apply()
    }

    /**
     * Translates a vibration pattern name to an Android vibration waveform timings array.
     */
    fun getPatternWaveform(pattern: String): LongArray {
        return when (pattern.uppercase()) {
            PATTERN_HEARTBEAT -> longArrayOf(0, 250, 150, 250, 750)
            PATTERN_GENTLE -> longArrayOf(0, 400, 800, 400, 800)
            else -> longArrayOf(0, 800, 400, 800, 400, 800) // CADENCE
        }
    }
}
