package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadRequest(requestId: String) {
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
            }
        }
    }

    fun acknowledge(onDone: () -> Unit = {}) {
        val requestId = _uiState.value.request?.requestId ?: return
        if (_uiState.value.isResponding) return

        _uiState.update { it.copy(isResponding = true, errorMessage = null) }
        viewModelScope.launch {
            val result = QalbiRepository.acknowledgeWakeUp(requestId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isResponding = false,
                            actionSuccessMessage = "You let them know you're awake! \u2600\uFE0F",
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

    fun decline(onDone: () -> Unit = {}) {
        val requestId = _uiState.value.request?.requestId ?: return
        if (_uiState.value.isResponding) return

        _uiState.update { it.copy(isResponding = true, errorMessage = null) }
        viewModelScope.launch {
            val result = QalbiRepository.declineWakeUp(requestId)
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
