package com.wakeus.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.alarm.AlarmState
import com.wakeus.app.data.alarm.LocalAlarmStatus
import com.wakeus.app.data.alarm.WakeUpAlarmManager
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncomingRequestUiState(
    val request: WakeUpRequest? = null,
    val isLoading: Boolean = false,
    val isResponding: Boolean = false,
    val actionSuccessMessage: String? = null,
    val errorMessage: String? = null,
)

class IncomingRequestViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IncomingRequestUiState())
    val uiState: StateFlow<IncomingRequestUiState> = _uiState.asStateFlow()

    val alarmState: StateFlow<AlarmState> = WakeUpAlarmManager.alarmState

    fun loadRequest(context: Context, requestId: String) {
        if (requestId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            QalbiRepository.observeRequest(requestId).collect { req ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        request = req,
                    )
                }

                // If request is pending and unexpired, ensure alarm is ringing
                if (req != null && req.status == WakeUpRequestStatus.PENDING && !req.isExpired) {
                    if (alarmState.value.status != LocalAlarmStatus.RINGING) {
                        WakeUpAlarmManager.startAlarm(
                            context = context,
                            requestId = req.requestId,
                            senderUid = req.senderUid,
                            senderDisplayName = req.senderDisplayName,
                            senderUsername = req.senderUsername,
                            expiresAt = req.expiresAt
                        )
                    }
                }
            }
        }
    }

    fun acknowledge(context: Context, onDone: () -> Unit = {}) {
        val requestId = _uiState.value.request?.requestId ?: return
        if (_uiState.value.isResponding) return

        _uiState.update { it.copy(isResponding = true, errorMessage = null) }

        WakeUpAlarmManager.acknowledgeAlarm(context, requestId) { result ->
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isResponding = false,
                            actionSuccessMessage = "You let them know you're awake! ☀️",
                        )
                    }
                    onDone()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isResponding = false,
                            errorMessage = error.message ?: "Failed to acknowledge wake-up.",
                        )
                    }
                }
            )
        }
    }

    fun decline(context: Context, onDone: () -> Unit = {}) {
        val requestId = _uiState.value.request?.requestId ?: return
        if (_uiState.value.isResponding) return

        _uiState.update { it.copy(isResponding = true, errorMessage = null) }

        WakeUpAlarmManager.declineAlarm(context, requestId) { result ->
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isResponding = false,
                            actionSuccessMessage = "Request declined.",
                        )
                    }
                    onDone()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isResponding = false,
                            errorMessage = error.message ?: "Failed to decline wake-up.",
                        )
                    }
                }
            )
        }
    }
}
