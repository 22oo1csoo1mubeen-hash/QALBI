package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.friends.AlreadyConnectedException
import com.wakeus.app.data.friends.FriendRepository
import com.wakeus.app.data.friends.InvalidPairingCodeException
import com.wakeus.app.data.friends.PairingCodeAlreadyRedeemedException
import com.wakeus.app.data.friends.PairingCodeExpiredException
import com.wakeus.app.data.friends.PairingCodeRevokedException
import com.wakeus.app.data.friends.SelfPairingException
import com.wakeus.app.data.friends.UserBlockedException
import com.wakeus.app.data.model.ConnectionCode
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.PairingCode
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectFriendUiState(
    // Section 1: Generate Code
    val isGenerating: Boolean = false,
    val isRevoking: Boolean = false,
    val activeCode: PairingCode? = null,
    val generateError: String? = null,

    // Section 2: Enter & Redeem Code
    val enteredCode: String = "",
    val enteredCodeError: String? = null,
    val isInspecting: Boolean = false,
    val previewFriend: PairingCode? = null,
    val isConnecting: Boolean = false,
    val connectedFriend: Friend? = null,
    val connectionResult: String? = null,
    val connectionError: String? = null,
) {
    /** Backwards compatibility helper for UI using ConnectionCode */
    val generatedCode: ConnectionCode? get() = activeCode?.let {
        ConnectionCode(it.code, "Expires in ${it.remainingMinutes} minutes")
    }
}

class ConnectFriendViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectFriendUiState())
    val uiState: StateFlow<ConnectFriendUiState> = _uiState.asStateFlow()

    fun generateCode() {
        if (_uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generateError = null) }
            val result = QalbiRepository.createPairingCode()
            result.fold(
                onSuccess = { code ->
                    _uiState.update { it.copy(isGenerating = false, activeCode = code, generateError = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generateError = error.message ?: "Failed to generate pairing code."
                        )
                    }
                }
            )
        }
    }

    fun revokeActiveCode() {
        val code = _uiState.value.activeCode?.code ?: return
        if (_uiState.value.isRevoking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRevoking = true) }
            val result = QalbiRepository.revokePairingCode(code)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isRevoking = false, activeCode = null) }
                },
                onFailure = {
                    _uiState.update { it.copy(isRevoking = false) }
                }
            )
        }
    }

    fun onEnteredCodeChange(value: String) {
        val normalized = FriendRepository.normalizeCode(value).take(FriendRepository.CODE_LENGTH)
        _uiState.update {
            it.copy(
                enteredCode = normalized,
                enteredCodeError = null,
                previewFriend = null,
                connectionResult = null,
                connectionError = null,
            )
        }
    }

    fun inspectCode() {
        val code = _uiState.value.enteredCode.trim()
        if (code.length != FriendRepository.CODE_LENGTH) {
            _uiState.update { it.copy(enteredCodeError = "Code must be exactly ${FriendRepository.CODE_LENGTH} characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isInspecting = true, connectionError = null, previewFriend = null) }
            val result = QalbiRepository.inspectPairingCode(code)
            result.fold(
                onSuccess = { prospective ->
                    _uiState.update { it.copy(isInspecting = false, previewFriend = prospective) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isInspecting = false,
                            connectionError = mapPairingError(error)
                        )
                    }
                }
            )
        }
    }

    fun cancelPreview() {
        _uiState.update { it.copy(previewFriend = null) }
    }

    fun submitCode() {
        val code = _uiState.value.enteredCode.trim()
        if (code.length != FriendRepository.CODE_LENGTH) {
            _uiState.update { it.copy(enteredCodeError = "Code must be exactly ${FriendRepository.CODE_LENGTH} characters.") }
            return
        }

        if (_uiState.value.isConnecting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnecting = true,
                    connectionError = null,
                    connectionResult = null,
                    previewFriend = null,
                )
            }
            val result = QalbiRepository.redeemPairingCode(code)
            result.fold(
                onSuccess = { friend ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            connectedFriend = friend,
                            connectionResult = "Successfully connected with @${friend.username} (${friend.displayName})!",
                            enteredCode = "",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            connectionError = mapPairingError(error)
                        )
                    }
                }
            )
        }
    }

    private fun mapPairingError(error: Throwable): String {
        return when (error) {
            is InvalidPairingCodeException -> "Invalid pairing code. Please check and try again."
            is PairingCodeExpiredException -> "This code has expired (10-minute limit). Ask your friend for a fresh code."
            is PairingCodeRevokedException -> "This code was cancelled by the person who created it."
            is PairingCodeAlreadyRedeemedException -> "This code has already been redeemed and cannot be used again."
            is SelfPairingException -> "You cannot connect with your own pairing code."
            is AlreadyConnectedException -> error.message ?: "You are already connected to this user."
            is UserBlockedException -> "Cannot connect with this user."
            else -> error.message ?: "Failed to connect. Please check your network and try again."
        }
    }
}
