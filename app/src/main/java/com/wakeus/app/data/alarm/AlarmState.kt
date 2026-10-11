package com.wakeus.app.data.alarm

/**
 * Explicit, strictly-validated lifecycle states for local alarm playback on the receiver device.
 * Completely decoupled from remote server state (WakeUpRequestStatus).
 */
enum class LocalAlarmStatus {
    IDLE,
    RECEIVED,
    STARTING,
    RINGING,
    ACKNOWLEDGING,
    STOPPING,
    ACKNOWLEDGED,
    DECLINED,
    CANCELLED,
    EXPIRED,
    FAILED;

    val isTerminal: Boolean
        get() = this in listOf(ACKNOWLEDGED, DECLINED, CANCELLED, EXPIRED, FAILED, IDLE)

    val isRinging: Boolean
        get() = this == RINGING

    /**
     * Validates whether a state transition from `this` to `next` is permissible.
     * Prevents invalid state jumps and prevents terminal states from reviving to RINGING.
     */
    fun canTransitionTo(next: LocalAlarmStatus): Boolean {
        if (this == next) return true
        return when (this) {
            IDLE -> next in listOf(RECEIVED, STARTING, FAILED)
            RECEIVED -> next in listOf(STARTING, STOPPING, CANCELLED, EXPIRED, FAILED, IDLE)
            STARTING -> next in listOf(RINGING, STOPPING, CANCELLED, EXPIRED, FAILED, IDLE)
            RINGING -> next in listOf(ACKNOWLEDGING, STOPPING, DECLINED, CANCELLED, EXPIRED, FAILED, IDLE)
            ACKNOWLEDGING -> next in listOf(ACKNOWLEDGED, STOPPING, FAILED, RINGING)
            STOPPING -> next in listOf(IDLE, ACKNOWLEDGED, DECLINED, CANCELLED, EXPIRED, FAILED)
            ACKNOWLEDGED, DECLINED, CANCELLED, EXPIRED, FAILED -> next == IDLE
        }
    }
}

/**
 * Immutable snapshot of active local alarm execution state.
 */
data class AlarmState(
    val status: LocalAlarmStatus = LocalAlarmStatus.IDLE,
    val requestId: String? = null,
    val senderUid: String? = null,
    val senderDisplayName: String? = null,
    val senderUsername: String? = null,
    val startedAt: Long = 0L,
    val isAudioPlaying: Boolean = false,
    val isVibrating: Boolean = false,
    val errorMessage: String? = null,
) {
    val isRinging: Boolean get() = status == LocalAlarmStatus.RINGING
    val isAcknowledging: Boolean get() = status == LocalAlarmStatus.ACKNOWLEDGING
}
