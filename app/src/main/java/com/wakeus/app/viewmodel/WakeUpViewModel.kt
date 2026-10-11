package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.data.wakeup.DuplicateWakeUpRequestException
import com.wakeus.app.data.wakeup.FriendBlockedException
import com.wakeus.app.data.wakeup.NoActiveConnectionException
import com.wakeus.app.data.wakeup.SelfWakeUpException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WakeUpUiState(
    val friend: Friend? = null,
    val wakeUpState: WakeUpState = WakeUpState.IDLE,
    val showConfirmDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCancelling: Boolean = false,
    val activeRequest: WakeUpRequest? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

class WakeUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WakeUpUiState())
    val uiState: StateFlow<WakeUpUiState> = _uiState.asStateFlow()

    private var requestObserverJob: Job? = null

    fun loadFriend(friendId: String) {
        if (friendId == QalbiRepository.demoFriend.id) {
            _uiState.update { it.copy(friend = QalbiRepository.demoFriend) }
            return
        }

        viewModelScope.launch {
            val profileResult = QalbiRepository.userRepository.getPublicProfile(friendId)
            val profile = profileResult.getOrNull()
            if (profile != null) {
                val currentUid = (QalbiRepository.authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user?.id ?: ""
                val connId = if (currentUid.isNotBlank()) com.wakeus.app.data.model.Connection.createDeterministicId(currentUid, friendId) else ""
                val friend = Friend(
                    id = profile.uid,
                    displayName = profile.displayName.ifBlank { "@${profile.username}" },
                    username = profile.username,
                    connectionId = connId,
                    status = "Connected",
                    isDemo = false,
                )
                _uiState.update { it.copy(friend = friend) }
            } else {
                val friend = QalbiRepository.getFriendById(friendId)
                _uiState.update { it.copy(friend = friend) }
            }
        }
    }

    fun requestWakeUp() {
        _uiState.update { it.copy(showConfirmDialog = true, errorMessage = null) }
    }

    fun cancelWakeUp() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmWakeUp() {
        val friend = _uiState.value.friend ?: return
        if (_uiState.value.isSubmitting) return

        _uiState.update {
            it.copy(
                showConfirmDialog = false,
                isSubmitting = true,
                errorMessage = null,
                statusMessage = "Sending request to ${friend.displayName}...",
            )
        }

        viewModelScope.launch {
            val result = QalbiRepository.sendWakeUp(friend)
            result.fold(
                onSuccess = { request ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            activeRequest = request,
                            wakeUpState = WakeUpState.PENDING,
                            statusMessage = "Request created in Firestore. Waiting for ${friend.displayName} to respond...",
                        )
                    }
                    observeRequestStatus(request.requestId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapWakeUpError(error),
                            statusMessage = null,
                        )
                    }
                }
            )
        }
    }

    private fun observeRequestStatus(requestId: String) {
        requestObserverJob?.cancel()
        requestObserverJob = viewModelScope.launch {
            QalbiRepository.observeRequest(requestId).collect { updated ->
                if (updated != null) {
                    val displayName = _uiState.value.friend?.displayName ?: "Friend"
                    val (state, message) = when (updated.status) {
                        WakeUpRequestStatus.PENDING -> {
                            if (updated.isExpired) {
                                WakeUpState.EXPIRED to "Wake-up request has expired (15 min limit)."
                            } else {
                                WakeUpState.PENDING to "Waiting for $displayName to respond..."
                            }
                        }
                        WakeUpRequestStatus.ACKNOWLEDGED -> WakeUpState.ACKNOWLEDGED to "$displayName is awake! \u2600\uFE0F\u2764\uFE0F"
                        WakeUpRequestStatus.DECLINED -> WakeUpState.DECLINED to "$displayName declined the request."
                        WakeUpRequestStatus.CANCELLED -> WakeUpState.CANCELLED to "You cancelled this request."
                        WakeUpRequestStatus.EXPIRED -> WakeUpState.EXPIRED to "Wake-up request has expired."
                    }

                    _uiState.update {
                        it.copy(
                            activeRequest = updated,
                            wakeUpState = state,
                            statusMessage = message,
                        )
                    }
                }
            }
        }
    }

    fun cancelActiveRequest() {
        val requestId = _uiState.value.activeRequest?.requestId ?: return
        if (_uiState.value.isCancelling) return

        _uiState.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            val result = QalbiRepository.cancelWakeUp(requestId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            wakeUpState = WakeUpState.CANCELLED,
                            statusMessage = "Request cancelled.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            errorMessage = error.message ?: "Failed to cancel request.",
                        )
                    }
                }
            )
        }
    }

    fun resetWakeUp() {
        requestObserverJob?.cancel()
        _uiState.update {
            it.copy(
                wakeUpState = WakeUpState.IDLE,
                activeRequest = null,
                errorMessage = null,
                statusMessage = null,
            )
        }
    }

    private fun mapWakeUpError(error: Throwable): String {
        return when (error) {
            is DuplicateWakeUpRequestException -> "A wake-up request for this friend is already pending."
            is NoActiveConnectionException -> "You must be connected with this friend to send wake-up requests."
            is FriendBlockedException -> "Cannot send requests to a blocked user."
            is SelfWakeUpException -> "You cannot send a wake-up request to yourself."
            else -> error.message ?: "Failed to send wake-up request. Please check your network connection."
        }
    }

    override fun onCleared() {
        super.onCleared()
        requestObserverJob?.cancel()
    }
}
