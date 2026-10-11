# QALBI — Production ProGuard & R8 Optimization Rules
# Ensures safe reflection, Coroutines stability, and data model preservation

# Preserve Line Numbers and Source File for intelligible crash reports
-keepattributes SourceFile,LineNumberTable

# ── Data Models ──────────────────────────────────────────────────────────────
# Keep data models used with Firestore serialization and mapping
-keep class com.wakeus.app.data.model.** { *; }
-keepclassmembers class com.wakeus.app.data.model.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Firebase Components ───────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Android Foreground Service & Receivers ────────────────────────────────────
-keep class com.wakeus.app.data.alarm.WakeUpAlarmService { *; }
-keep class com.wakeus.app.data.fcm.QalbiMessagingService { *; }
