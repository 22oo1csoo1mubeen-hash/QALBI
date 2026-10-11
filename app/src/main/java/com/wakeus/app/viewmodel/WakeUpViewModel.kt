package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WakeUpUiState(
    val friend: Friend? = null,
    val wakeUpState: WakeUpState = WakeUpState.IDLE,
    val showConfirmDialog: Boolean = false,
)

class WakeUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WakeUpUiState())
    val uiState: StateFlow<WakeUpUiState> = _uiState.asStateFlow()

    fun loadFriend(friendId: String) {
        val friend = QalbiRepository.getFriendById(friendId)
        _uiState.update { it.copy(friend = friend) }
    }

    fun requestWakeUp() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun cancelWakeUp() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmWakeUp() {
        val friendId = _uiState.value.friend?.id ?: return
        _uiState.update { it.copy(showConfirmDialog = false, wakeUpState = WakeUpState.PENDING) }
        viewModelScope.launch {
            val state = QalbiRepository.sendWakeUpRequest(friendId)
            _uiState.update { it.copy(wakeUpState = state) }
        }
    }

    fun resetWakeUp() {
        _uiState.update { it.copy(wakeUpState = WakeUpState.IDLE) }
    }
}
