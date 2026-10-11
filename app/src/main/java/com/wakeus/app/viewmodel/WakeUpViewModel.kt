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
        if (friendId == QalbiRepository.demoFriend.id) {
            _uiState.update { it.copy(friend = QalbiRepository.demoFriend) }
            return
        }

        viewModelScope.launch {
            val profileResult = QalbiRepository.userRepository.getPublicProfile(friendId)
            val profile = profileResult.getOrNull()
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        friend = Friend(
                            id = profile.uid,
                            displayName = profile.displayName.ifBlank { "@${profile.username}" },
                            username = profile.username,
                            status = "Connected",
                            isDemo = false,
                        )
                    )
                }
            } else {
                val friend = QalbiRepository.getFriendById(friendId)
                _uiState.update { it.copy(friend = friend) }
            }
        }
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
