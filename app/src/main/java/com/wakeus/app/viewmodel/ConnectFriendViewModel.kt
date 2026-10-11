package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.model.ConnectionCode
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectFriendUiState(
    // Generate section
    val isGenerating: Boolean = false,
    val generatedCode: ConnectionCode? = null,
    // Enter code section
    val enteredCode: String = "",
    val enteredCodeError: String? = null,
    val isConnecting: Boolean = false,
    val connectionResult: String? = null,
    val connectionError: String? = null,
)

class ConnectFriendViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectFriendUiState())
    val uiState: StateFlow<ConnectFriendUiState> = _uiState.asStateFlow()

    fun generateCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generatedCode = null) }
            val code = QalbiRepository.generateConnectionCode()
            _uiState.update { it.copy(isGenerating = false, generatedCode = code) }
        }
    }

    fun onEnteredCodeChange(value: String) = _uiState.update {
        it.copy(
            enteredCode = value.uppercase().take(8),
            enteredCodeError = null,
            connectionResult = null,
            connectionError = null,
        )
    }

    fun submitCode() {
        val code = _uiState.value.enteredCode.trim()
        if (code.length < 6) {
            _uiState.update { it.copy(enteredCodeError = "Code must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionError = null, connectionResult = null) }
            val result = QalbiRepository.submitConnectionCode(code)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update { it.copy(isConnecting = false, connectionResult = msg) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isConnecting = false, connectionError = e.message) }
                },
            )
        }
    }
}
