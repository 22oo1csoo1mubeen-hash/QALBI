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

// ─── UI state ────────────────────────────────────────────────────────────────
data class RegisterUiState(
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,
    val isLoading: Boolean = false,
    val successUser: User? = null,
    val error: String? = null,
    // Field-level errors
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
)

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(value: String) = _uiState.update {
        it.copy(displayName = value, displayNameError = null)
    }

    fun onUsernameChange(value: String) = _uiState.update {
        it.copy(username = value, usernameError = null)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value, passwordError = null, confirmPasswordError = null)
    }

    fun onConfirmPasswordChange(value: String) = _uiState.update {
        it.copy(confirmPassword = value, confirmPasswordError = null)
    }

    fun togglePasswordVisibility() = _uiState.update {
        it.copy(showPassword = !it.showPassword)
    }

    fun toggleConfirmPasswordVisibility() = _uiState.update {
        it.copy(showConfirmPassword = !it.showConfirmPassword)
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /** Validates all fields and, if valid, performs a mock registration. */
    fun submit() {
        val state = _uiState.value
        val displayNameError = validateDisplayName(state.displayName)
        val usernameError = validateUsername(state.username)
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)
        val confirmError = validateConfirmPassword(state.password, state.confirmPassword)

        if (listOf(displayNameError, usernameError, emailError, passwordError, confirmError).any { it != null }) {
            _uiState.update {
                it.copy(
                    displayNameError    = displayNameError,
                    usernameError       = usernameError,
                    emailError          = emailError,
                    passwordError       = passwordError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = QalbiRepository.mockRegister(
                displayName = state.displayName,
                username    = state.username,
                email       = state.email,
                password    = state.password,
            )
            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false, successUser = user) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    // ─── Validators ──────────────────────────────────────────────────────────
    private fun validateDisplayName(v: String) =
        if (v.isBlank()) "Display name is required" else null

    private fun validateUsername(v: String): String? = when {
        v.isBlank() -> "Username is required"
        v.length < 3 -> "Username must be at least 3 characters"
        !v.matches(Regex("^[a-z0-9._]+$")) ->
            "Only lowercase letters, digits, dots, and underscores allowed"
        else -> null
    }

    private fun validateEmail(v: String): String? = when {
        v.isBlank() -> "Email is required"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches() -> "Enter a valid email address"
        else -> null
    }

    private fun validatePassword(v: String): String? = when {
        v.isBlank() -> "Password is required"
        v.length < 8 -> "Password must be at least 8 characters"
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "Please confirm your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }
}
