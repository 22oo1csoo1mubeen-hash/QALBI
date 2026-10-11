package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.model.User
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val emailOrUsername: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val successUser: User? = null,
    val error: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
)

class LoginViewModel(
    private val repository: QalbiRepository = QalbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(emailOrUsername = value, emailError = null, error = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value, passwordError = null, error = null)
    }

    fun togglePasswordVisibility() = _uiState.update {
        it.copy(showPassword = !it.showPassword)
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun submit() {
        val state = _uiState.value
        // Prevent duplicate submissions
        if (state.isLoading) return

        val trimmedIdentifier = state.emailOrUsername.trim()
        val emailError = when {
            trimmedIdentifier.isBlank() -> "Email or username is required"
            trimmedIdentifier.length < 3 -> "Enter a valid email or username (at least 3 characters)"
            else -> null
        }
        val passwordError = if (state.password.isBlank()) "Password is required" else null

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.login(trimmedIdentifier, state.password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false, successUser = user) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Login failed. Please verify your credentials."
                        )
                    }
                },
            )
        }
    }
}
