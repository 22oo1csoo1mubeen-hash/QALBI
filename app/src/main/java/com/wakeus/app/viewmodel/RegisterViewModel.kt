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

/**
 * Registration method selected by the user.
 */
enum class RegistrationMethod {
    EMAIL,
    USERNAME,
}

// ─── UI state ────────────────────────────────────────────────────────────────
data class RegisterUiState(
    val registrationMethod: RegistrationMethod = RegistrationMethod.EMAIL,
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

class RegisterViewModel(
    private val repository: QalbiRepository = QalbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun setRegistrationMethod(method: RegistrationMethod) = _uiState.update {
        it.copy(
            registrationMethod = method,
            emailError = null,
            error = null
        )
    }

    fun onDisplayNameChange(value: String) = _uiState.update {
        it.copy(displayName = value, displayNameError = null, error = null)
    }

    fun onUsernameChange(value: String) = _uiState.update {
        it.copy(username = value, usernameError = null, error = null)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = null, error = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value, passwordError = null, confirmPasswordError = null, error = null)
    }

    fun onConfirmPasswordChange(value: String) = _uiState.update {
        it.copy(confirmPassword = value, confirmPasswordError = null, error = null)
    }

    fun togglePasswordVisibility() = _uiState.update {
        it.copy(showPassword = !it.showPassword)
    }

    fun toggleConfirmPasswordVisibility() = _uiState.update {
        it.copy(showConfirmPassword = !it.showConfirmPassword)
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /** Validates all fields and performs real or preview registration. */
    fun submit() {
        val state = _uiState.value
        // Prevent duplicate submissions while in flight
        if (state.isLoading) return

        val displayNameError = validateDisplayName(state.displayName)
        val usernameError = validateUsername(state.username)
        val emailError = if (state.registrationMethod == RegistrationMethod.EMAIL) {
            validateEmail(state.email)
        } else null
        val passwordError = validatePassword(state.password)
        val confirmError = validateConfirmPassword(state.password, state.confirmPassword)

        if (listOfNotNull(displayNameError, usernameError, emailError, passwordError, confirmError).isNotEmpty()) {
            _uiState.update {
                it.copy(
                    displayNameError     = displayNameError,
                    usernameError        = usernameError,
                    emailError           = emailError,
                    passwordError        = passwordError,
                    confirmPasswordError = confirmError,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (state.registrationMethod == RegistrationMethod.EMAIL) {
                repository.registerWithEmail(
                    displayName = state.displayName.trim(),
                    username    = state.username.trim(),
                    email       = state.email.trim(),
                    password    = state.password,
                )
            } else {
                repository.registerWithUsername(
                    displayName = state.displayName.trim(),
                    username    = state.username.trim(),
                    password    = state.password,
                )
            }

            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isLoading = false, successUser = user) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Registration failed.") }
                },
            )
        }
    }

    // ─── Validators ──────────────────────────────────────────────────────────
    fun validateDisplayName(v: String): String? = when {
        v.isBlank() -> "Display name is required"
        v.trim().length < 2 -> "Display name must be at least 2 characters"
        v.trim().length > 50 -> "Display name cannot exceed 50 characters"
        else -> null
    }

    fun validateUsername(v: String): String? {
        val trimmed = v.trim()
        return when {
            trimmed.isBlank() -> "Username is required"
            trimmed.length < 3 -> "Username must be at least 3 characters"
            trimmed.length > 20 -> "Username cannot exceed 20 characters"
            !trimmed.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                "Only letters, numbers, and underscores allowed"
            else -> null
        }
    }

    fun validateEmail(v: String): String? {
        val trimmed = v.trim()
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return when {
            trimmed.isBlank() -> "Email is required"
            !emailRegex.matches(trimmed) -> "Enter a valid email address"
            else -> null
        }
    }

    fun validatePassword(v: String): String? = when {
        v.isBlank() -> "Password is required"
        v.length < 8 -> "Password must be at least 8 characters"
        else -> null
    }

    fun validateConfirmPassword(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "Please confirm your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }
}
