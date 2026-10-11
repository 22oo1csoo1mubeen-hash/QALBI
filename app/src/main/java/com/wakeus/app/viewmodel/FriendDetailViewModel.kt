package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.model.Connection
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendDetailUiState(
    val friend: Friend? = null,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val showBlockDialog: Boolean = false,
    val actionError: String? = null,
    val isDisconnectedOrBlocked: Boolean = false,
)

class FriendDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FriendDetailUiState())
    val uiState: StateFlow<FriendDetailUiState> = _uiState.asStateFlow()

    fun loadFriend(friendId: String) {
        if (friendId == QalbiRepository.demoFriend.id) {
            _uiState.update { it.copy(friend = QalbiRepository.demoFriend) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val profileResult = QalbiRepository.userRepository.getPublicProfile(friendId)
            val profile = profileResult.getOrNull()
            if (profile != null) {
                val currentUid = (QalbiRepository.authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user?.id ?: ""
                val connectionId = if (currentUid.isNotBlank()) Connection.createDeterministicId(currentUid, friendId) else ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        friend = Friend(
                            id = profile.uid,
                            displayName = profile.displayName.ifBlank { "@${profile.username}" },
                            username = profile.username,
                            connectionId = connectionId,
                            status = "Connected",
                            isDemo = false,
                        )
                    )
                }
            } else {
                val fallback = QalbiRepository.getFriendById(friendId)
                _uiState.update { it.copy(isLoading = false, friend = fallback) }
            }
        }
    }

    fun promptDisconnect() = _uiState.update { it.copy(showDisconnectDialog = true) }
    fun dismissDisconnect() = _uiState.update { it.copy(showDisconnectDialog = false) }

    fun promptBlock() = _uiState.update { it.copy(showBlockDialog = true) }
    fun dismissBlock() = _uiState.update { it.copy(showBlockDialog = false) }

    fun confirmDisconnect(onDone: () -> Unit) {
        val friend = _uiState.value.friend ?: return
        val connectionId = friend.connectionId
        _uiState.update { it.copy(showDisconnectDialog = false, isActionInProgress = true, actionError = null) }

        viewModelScope.launch {
            val result = QalbiRepository.disconnectFriend(connectionId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionInProgress = false, isDisconnectedOrBlocked = true) }
                    onDone()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isActionInProgress = false, actionError = error.message ?: "Failed to disconnect.")
                    }
                }
            )
        }
    }

    fun confirmBlock(onDone: () -> Unit) {
        val friend = _uiState.value.friend ?: return
        _uiState.update { it.copy(showBlockDialog = false, isActionInProgress = true, actionError = null) }

        viewModelScope.launch {
            val result = QalbiRepository.blockUser(friend.id, friend.connectionId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionInProgress = false, isDisconnectedOrBlocked = true) }
                    onDone()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isActionInProgress = false, actionError = error.message ?: "Failed to block user.")
                    }
                }
            )
        }
    }
}
