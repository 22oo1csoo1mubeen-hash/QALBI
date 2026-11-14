package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.auth.AuthState
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.ScheduledAlarm
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateScheduleUiState(
    val isCreating: Boolean = false,
    val error: String? = null,
    val created: Boolean = false,
)

class CreateScheduleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateScheduleUiState())
    val uiState: StateFlow<CreateScheduleUiState> = _uiState.asStateFlow()
    private var pendingFingerprint: String? = null
    private var pendingScheduleId: String? = null

    fun create(friend: Friend, scheduledAt: Long, timeZoneId: String, message: String) {
        if (_uiState.value.isCreating) return
        val sender = (QalbiRepository.authState.value as? AuthState.Authenticated)?.user
        if (sender == null) {
            _uiState.update { it.copy(error = "Please sign in again before scheduling.") }
            return
        }
        val fingerprint = listOf(friend.id, scheduledAt, timeZoneId, message.trim()).joinToString("|")
        if (fingerprint != pendingFingerprint) {
            pendingFingerprint = fingerprint
            pendingScheduleId = UUID.randomUUID().toString()
        }
        val scheduleId = pendingScheduleId ?: UUID.randomUUID().toString().also { pendingScheduleId = it }
        _uiState.update { CreateScheduleUiState(isCreating = true) }
        viewModelScope.launch {
            QalbiRepository.scheduledAlarmRepository.create(sender, friend, scheduledAt, timeZoneId, message, scheduleId)
                .fold(
                    onSuccess = {
                        pendingFingerprint = null
                        pendingScheduleId = null
                        _uiState.value = CreateScheduleUiState(created = true)
                    },
                    onFailure = { error ->
                        _uiState.value = CreateScheduleUiState(
                            error = error.message ?: "Could not create the schedule. Check your connection and try again.",
                        )
                    },
                )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class ScheduledAlarmsUiState(
    val schedules: List<ScheduledAlarm> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val cancellingId: String? = null,
    val editingId: String? = null,
)

class ScheduledAlarmsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduledAlarmsUiState())
    val uiState: StateFlow<ScheduledAlarmsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            QalbiRepository.authState.collectLatest { auth ->
                val user = (auth as? AuthState.Authenticated)?.user
                if (user == null) {
                    _uiState.value = ScheduledAlarmsUiState(isLoading = false)
                } else {
                    QalbiRepository.scheduledAlarmRepository.observeMine(user.id).collectLatest { schedules ->
                        _uiState.update { it.copy(schedules = schedules, isLoading = false, error = null) }
                    }
                }
            }
        }
    }

    fun cancel(schedule: ScheduledAlarm) {
        if (_uiState.value.cancellingId != null || _uiState.value.editingId != null) return
        _uiState.update { it.copy(cancellingId = schedule.scheduleId, error = null) }
        viewModelScope.launch {
            val result = QalbiRepository.scheduledAlarmRepository.cancel(schedule)
            _uiState.update {
                it.copy(
                    cancellingId = null,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun edit(schedule: ScheduledAlarm, scheduledAt: Long, timeZoneId: String, message: String) {
        if (_uiState.value.cancellingId != null || _uiState.value.editingId != null) return
        _uiState.update { it.copy(editingId = schedule.scheduleId, error = null) }
        viewModelScope.launch {
            val result = QalbiRepository.scheduledAlarmRepository.edit(schedule, scheduledAt, timeZoneId, message)
            _uiState.update {
                it.copy(
                    editingId = null,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }
}
