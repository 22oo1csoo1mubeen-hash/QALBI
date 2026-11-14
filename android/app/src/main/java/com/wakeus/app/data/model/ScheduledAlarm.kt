package com.wakeus.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.ZoneId
import java.util.Date

enum class ScheduledAlarmStatus {
    SCHEDULED,
    PROCESSING,
    SENT,
    ACKNOWLEDGED,
    DECLINED,
    FAILED,
    CANCELLED,
    EXPIRED,
}

data class ScheduledAlarm(
    val scheduleId: String,
    val senderUid: String,
    val senderDisplayName: String,
    val senderUsername: String,
    val receiverUid: String,
    val receiverDisplayName: String,
    val receiverUsername: String,
    val connectionId: String,
    val message: String,
    val scheduledAtMillis: Long,
    val timeZoneId: String,
    val status: ScheduledAlarmStatus,
    val attemptCount: Int,
    val failureCode: String? = null,
) {
    /**
     * Keep a due job visible briefly while the minute-based dispatcher claims it.
     * A sent request can take up to another 15 minutes to complete, so the outer
     * bound is 30 minutes from its scheduled instant (15 minutes late + 15-minute
     * response window). This also hides stale records if a cloud finalization is
     * delayed or was missed.
     */
    fun isActiveAt(nowMillis: Long = System.currentTimeMillis()): Boolean = when (status) {
        ScheduledAlarmStatus.SCHEDULED,
        ScheduledAlarmStatus.PROCESSING -> nowMillis < scheduledAtMillis + DISPATCH_GRACE_MS
        ScheduledAlarmStatus.SENT -> nowMillis < scheduledAtMillis + COMPLETION_GRACE_MS
        else -> false
    }

    val canBeCancelled: Boolean
        get() = status == ScheduledAlarmStatus.SCHEDULED && scheduledAtMillis > System.currentTimeMillis()

    fun formattedSchedule(): String {
        val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        return java.time.Instant.ofEpochMilli(scheduledAtMillis)
            .atZone(zone)
            .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy • h:mm a"))
    }

    companion object {
        private const val DISPATCH_GRACE_MS = 15 * 60 * 1000L
        private const val COMPLETION_GRACE_MS = 30 * 60 * 1000L

        fun fromDocument(document: DocumentSnapshot): ScheduledAlarm? {
            val scheduledAt = document.getTimestamp("scheduledAt")?.toDate()?.time ?: return null
            val status = runCatching {
                ScheduledAlarmStatus.valueOf(document.getString("status") ?: return null)
            }.getOrDefault(ScheduledAlarmStatus.FAILED)
            return ScheduledAlarm(
                scheduleId = document.id,
                senderUid = document.getString("senderUid") ?: return null,
                senderDisplayName = document.getString("senderDisplayName") ?: "",
                senderUsername = document.getString("senderUsername") ?: "",
                receiverUid = document.getString("receiverUid") ?: return null,
                receiverDisplayName = document.getString("receiverDisplayName") ?: "Friend",
                receiverUsername = document.getString("receiverUsername") ?: "",
                connectionId = document.getString("connectionId") ?: "",
                message = document.getString("message").orEmpty(),
                scheduledAtMillis = scheduledAt,
                timeZoneId = document.getString("timeZoneId") ?: "UTC",
                status = status,
                attemptCount = (document.getLong("attemptCount") ?: 0L).toInt(),
                failureCode = document.getString("failureCode"),
            )
        }
    }
}
