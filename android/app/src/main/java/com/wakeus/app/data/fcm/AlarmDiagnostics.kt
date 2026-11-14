package com.wakeus.app.data.fcm

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/** Small bounded local event log. Entries contain event names and request IDs only. */
object AlarmDiagnostics {
    private const val PREFS = "alarm_reliability_diagnostics"
    private const val EVENTS_KEY = "events"
    private const val MAX_EVENTS = 80

    fun record(context: Context, event: AlarmDiagnosticEvent, requestId: String? = null) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = try { JSONArray(prefs.getString(EVENTS_KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray()
        val first = (previous.length() - MAX_EVENTS + 1).coerceAtLeast(0)
        for (index in first until previous.length()) next.put(previous.getJSONObject(index))
        next.put(JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("event", event.name)
            requestId?.take(80)?.let { put("requestId", it) }
        })
        prefs.edit().putString(EVENTS_KEY, next.toString()).apply()
        if (event == AlarmDiagnosticEvent.FCM_TOKEN_REGISTERED || event == AlarmDiagnosticEvent.FCM_TOKEN_REGISTRATION_FAILED) {
            prefs.edit()
                .putString("token_status", if (event == AlarmDiagnosticEvent.FCM_TOKEN_REGISTERED) "registered" else "needs_attention")
                .apply()
            if (event == AlarmDiagnosticEvent.FCM_TOKEN_REGISTERED) {
                prefs.edit().putLong("token_registered_at", System.currentTimeMillis()).apply()
            }
        }
    }

    fun tokenStatus(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("token_status", "unknown") ?: "unknown"

    fun tokenRegisteredAt(context: Context): Long? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getLong("token_registered_at", 0L).takeIf { it > 0L }

    fun lastEventTime(context: Context, event: AlarmDiagnosticEvent): Long? = events(context)
        .lastOrNull { it.optString("event") == event.name }
        ?.optLong("timestamp")
        ?.takeIf { it > 0L }

    fun lastFailure(context: Context): String? = events(context)
        .lastOrNull {
            val name = it.optString("event")
            name.endsWith("FAILED") || name.endsWith("BLOCKED") || name.endsWith("REJECTED")
        }
        ?.optString("event")

    fun exportReport(context: Context, checks: List<Pair<String, String>>): String = buildString {
        appendLine("QALBI Background Alarm Reliability Report")
        appendLine("Generated: ${System.currentTimeMillis()}")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
        appendLine("App version: ${runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "Unable to verify"}")
        appendLine("FCM registration: ${tokenStatus(context)}")
        appendLine("Last successful registration: ${tokenRegisteredAt(context)?.toString() ?: "Not recorded"}")
        appendLine("\nDevice checks:")
        checks.forEach { (name, status) -> appendLine("- $name: $status") }
        appendLine("\nRecent local events (FCM acceptance is not proof of device delivery):")
        events(context).forEach { item ->
            val id = item.optString("requestId").takeIf { it.isNotBlank() }?.let { " requestId=$it" } ?: ""
            appendLine("- ${item.optLong("timestamp")} ${item.optString("event")}$id")
        }
        appendLine("\nThis report contains no FCM token, authentication credential, password, or message content.")
    }

    private fun events(context: Context): List<JSONObject> {
        val json = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(EVENTS_KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

enum class AlarmDiagnosticEvent {
    FCM_TOKEN_REGISTERED,
    FCM_TOKEN_REGISTRATION_FAILED,
    FCM_MESSAGE_RECEIVED,
    FCM_MESSAGE_REJECTED,
    ALARM_SERVICE_START_REQUESTED,
    ALARM_SERVICE_STARTED,
    ALARM_SERVICE_START_FAILED,
    ALARM_NOTIFICATION_SUBMITTED,
    ALARM_NOTIFICATION_BLOCKED,
    ALARM_PLAYBACK_STARTED,
    ALARM_PLAYBACK_FAILED,
    ALARM_ACKNOWLEDGED,
}
