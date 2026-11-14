package com.wakeus.app.data.alarm

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wakeus.app.data.fcm.NotificationHelper
import com.wakeus.app.data.fcm.AlarmDiagnostics
import com.wakeus.app.data.fcm.AlarmDiagnosticEvent
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central singleton orchestrating local alarm execution, state transitions,
 * audio/vibration hardware control, and remote Firestore status synchronization.
 */
object WakeUpAlarmManager {

    private val _alarmState = MutableStateFlow(AlarmState())
    val alarmState: StateFlow<AlarmState> = _alarmState.asStateFlow()

    private val managerScope = CoroutineScope(Dispatchers.Main + Job())
    private var remoteObserverJob: Job? = null

    /**
     * Initiates the incoming alarm sequence for a validated wake-up request.
     */
    @Synchronized
    fun startAlarm(
        context: Context,
        requestId: String,
        senderUid: String,
        senderDisplayName: String,
        senderUsername: String,
        expiresAt: Long = System.currentTimeMillis() + 15 * 60 * 1000L,
    ): Boolean {
        var current = _alarmState.value

        // Duplicate check: already ringing for this request
        if (current.status == LocalAlarmStatus.RINGING && current.requestId == requestId) {
            return true
        }

        // If prior alarm reached a terminal state (ACKNOWLEDGED, DECLINED, CANCELLED, EXPIRED, FAILED),
        // cleanly reset local alarm state machine to IDLE so the new alarm starts fresh
        if (current.status.isTerminal) {
            resetToIdle(context)
            current = _alarmState.value
        }

        // Expiry check: cannot start if expired
        if (System.currentTimeMillis() >= expiresAt) {
            AlarmDiagnostics.record(context, AlarmDiagnosticEvent.FCM_MESSAGE_REJECTED, requestId)
            _alarmState.value = AlarmState(
                status = LocalAlarmStatus.EXPIRED,
                requestId = requestId,
                senderUid = senderUid,
                senderDisplayName = senderDisplayName,
                senderUsername = senderUsername,
            )
            return false
        }

        // Do not re-ring if actively acknowledging this specific request
        if (current.status == LocalAlarmStatus.ACKNOWLEDGING && current.requestId == requestId) {
            return false
        }

        _alarmState.value = AlarmState(
            status = LocalAlarmStatus.STARTING,
            requestId = requestId,
            senderUid = senderUid,
            senderDisplayName = senderDisplayName,
            senderUsername = senderUsername,
            startedAt = System.currentTimeMillis(),
        )

        // 1. Immediately start local audio & vibration with zero latency
        startLocalPlayback(context)

        // 2. Start Foreground Service to guarantee playback continuity across background & lockscreen
        val serviceIntent = Intent(context, WakeUpAlarmService::class.java).apply {
            action = WakeUpAlarmService.ACTION_START_ALARM
            putExtra(NotificationHelper.EXTRA_REQUEST_ID, requestId)
            putExtra("senderUid", senderUid)
            putExtra("senderDisplayName", senderDisplayName)
            putExtra("senderUsername", senderUsername)
            putExtra("expiresAt", expiresAt)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            AlarmDiagnostics.record(context, AlarmDiagnosticEvent.ALARM_SERVICE_START_REQUESTED, requestId)
        } catch (_: Exception) {
            AlarmDiagnostics.record(context, AlarmDiagnosticEvent.ALARM_SERVICE_START_FAILED, requestId)
            // Already playing locally
        }

        // Observe remote Firestore request: if sender cancels or request expires on server, stop immediately
        observeRemoteRequest(context, requestId)

        return true
    }

    /**
     * Called by WakeUpAlarmService once foreground service is active or directly by startAlarm.
     */
    @Synchronized
    fun startLocalPlayback(context: Context) {
        val current = _alarmState.value
        val audioStarted = AlarmSoundPlayer.start(context)
        val vibrationStarted = AlarmVibrator.start(context)
        AlarmDiagnostics.record(
            context,
            if (audioStarted) AlarmDiagnosticEvent.ALARM_PLAYBACK_STARTED else AlarmDiagnosticEvent.ALARM_PLAYBACK_FAILED,
            current.requestId,
        )
        WakeLockHelper.acquire(context)

        _alarmState.value = current.copy(
            status = LocalAlarmStatus.RINGING,
            isAudioPlaying = audioStarted,
            isVibrating = vibrationStarted,
        )
    }

    /**
     * Receiver acknowledges the alarm ("I'm Awake!").
     * Stops audio/vibration immediately, submits update to Firestore, and terminates service.
     */
    fun acknowledgeAlarm(
        context: Context,
        requestId: String,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        // 1. Immediately halt sound and vibration so the user is not disturbed
        stopHardware(context)

        val current = _alarmState.value
        _alarmState.value = current.copy(status = LocalAlarmStatus.ACKNOWLEDGING)

        managerScope.launch {
            val result = QalbiRepository.acknowledgeWakeUp(requestId)
            if (result.isSuccess) {
                AlarmDiagnostics.record(context, AlarmDiagnosticEvent.ALARM_ACKNOWLEDGED, requestId)
                _alarmState.value = current.copy(status = LocalAlarmStatus.ACKNOWLEDGED)
                stopService(context)
                NotificationHelper.cancelNotification(context, requestId)
            } else {
                _alarmState.value = current.copy(
                    status = LocalAlarmStatus.FAILED,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to confirm acknowledgement",
                )
            }
            onComplete(result)
        }
    }

    /**
     * Receiver declines the alarm.
     */
    fun declineAlarm(
        context: Context,
        requestId: String,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        stopHardware(context)
        _alarmState.value = _alarmState.value.copy(status = LocalAlarmStatus.DECLINED)

        managerScope.launch {
            val result = QalbiRepository.declineWakeUp(requestId)
            stopService(context)
            NotificationHelper.cancelNotification(context, requestId)
            onComplete(result)
        }
    }

    /**
     * Called when remote request is detected as CANCELLED by the sender.
     */
    fun handleRemoteCancellation(context: Context, requestId: String) {
        stopHardware(context)
        _alarmState.value = _alarmState.value.copy(status = LocalAlarmStatus.CANCELLED)
        stopService(context)
        NotificationHelper.cancelNotification(context, requestId)
    }

    /**
     * Called when request has reached expiry.
     */
    fun handleExpiry(context: Context, requestId: String) {
        stopHardware(context)
        _alarmState.value = _alarmState.value.copy(status = LocalAlarmStatus.EXPIRED)
        stopService(context)
        NotificationHelper.cancelNotification(context, requestId)
    }

    /**
     * Stops audio, vibration, and wake lock.
     */
    @Synchronized
    fun stopHardware(@Suppress("UNUSED_PARAMETER") context: Context? = null) {
        AlarmSoundPlayer.stop()
        AlarmVibrator.stop()
        WakeLockHelper.release()
        remoteObserverJob?.cancel()
        remoteObserverJob = null
    }

    /**
     * Resets local alarm state machine back to IDLE.
     */
    @Synchronized
    fun resetToIdle(context: Context) {
        stopHardware(context)
        stopService(context)
        _alarmState.value = AlarmState(status = LocalAlarmStatus.IDLE)
    }

    private fun stopService(context: Context) {
        val stopIntent = Intent(context, WakeUpAlarmService::class.java).apply {
            action = WakeUpAlarmService.ACTION_STOP_ALARM
        }
        try {
            context.stopService(stopIntent)
        } catch (_: Exception) {}
    }

    private fun observeRemoteRequest(context: Context, requestId: String) {
        remoteObserverJob?.cancel()
        remoteObserverJob = managerScope.launch {
            QalbiRepository.wakeUpRepository.observeRequest(requestId).collect { request ->
                if (request != null) {
                    when (request.status) {
                        WakeUpRequestStatus.CANCELLED -> {
                            handleRemoteCancellation(context, requestId)
                        }
                        WakeUpRequestStatus.EXPIRED -> {
                            handleExpiry(context, requestId)
                        }
                        WakeUpRequestStatus.ACKNOWLEDGED -> {
                            stopHardware(context)
                            stopService(context)
                            _alarmState.value = _alarmState.value.copy(status = LocalAlarmStatus.ACKNOWLEDGED)
                        }
                        WakeUpRequestStatus.DECLINED -> {
                            stopHardware(context)
                            stopService(context)
                            _alarmState.value = _alarmState.value.copy(status = LocalAlarmStatus.DECLINED)
                        }
                        WakeUpRequestStatus.PENDING -> {
                            if (request.isExpired) {
                                handleExpiry(context, requestId)
                            }
                        }
                    }
                }
            }
        }
    }
}
