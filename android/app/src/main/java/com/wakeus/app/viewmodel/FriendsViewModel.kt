package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val isLoading: Boolean = false,
    val actionError: String? = null,
    val actionSuccess: String? = null,
)

class FriendsViewModel : ViewModel() {

    /**
     * Live StateFlow of real connected friends from Firestore.
     */
    val friends: StateFlow<List<Friend>> = QalbiRepository.friends
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    fun disconnectFriend(connectionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, actionError = null, actionSuccess = null) }
            val result = QalbiRepository.disconnectFriend(connectionId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = "Friend disconnected.") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, actionError = error.message ?: "Failed to disconnect.") }
                }
            )
        }
    }

    fun blockUser(blockedUid: String, connectionId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, actionError = null, actionSuccess = null) }
            val result = QalbiRepository.blockUser(blockedUid, connectionId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, actionSuccess = "User blocked.") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, actionError = error.message ?: "Failed to block user.") }
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(actionError = null, actionSuccess = null) }
    }
}
