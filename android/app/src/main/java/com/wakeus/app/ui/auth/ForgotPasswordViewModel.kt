package com.wakeus.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecoveryTab {
    EMAIL, USERNAME
}

enum class UsernameRecoveryOption {
    REMEMBER_OLD_PASSWORD, FORGOT_OLD_PASSWORD
}

enum class ForgotPasswordStep {
    REQUEST,
    RESET_FORM,
    SUCCESS
}

data class ForgotPasswordUiState(
    // Step navigation
    val step: ForgotPasswordStep = ForgotPasswordStep.REQUEST,
    val selectedTab: RecoveryTab = RecoveryTab.EMAIL,

    // Step 1: Identifier
    val identifier: String = "",
    val identifierError: String? = null,
    val maskedEmail: String? = null,

    // Step 2: In-App Password Reset Form
    val resetCode: String = "",
    val resetCodeError: String? = null,
    val newPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,

    // Optional: "I know current password" mode
    val isChangePasswordMode: Boolean = false,
    val currentPassword: String = "",
    val currentPasswordError: String? = null,
    val showCurrentPassword: Boolean = false,

    // Legacy fields for backward compatibility
    val email: String = "",
    val emailError: String? = null,
    val username: String = "",
    val usernameError: String? = null,
    val usernameOption: UsernameRecoveryOption = UsernameRecoveryOption.FORGOT_OLD_PASSWORD,

    // Status
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

class ForgotPasswordViewModel(
    private val repository: QalbiRepository = QalbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun setStep(step: ForgotPasswordStep) {
        _uiState.update { it.copy(step = step, errorMessage = null) }
    }

    fun setTab(tab: RecoveryTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun toggleChangePasswordMode() {
        _uiState.update {
            it.copy(
                isChangePasswordMode = !it.isChangePasswordMode,
                errorMessage = null,
                currentPasswordError = null,
                newPasswordError = null,
                confirmPasswordError = null,
            )
        }
    }

    fun onIdentifierChange(value: String) {
        _uiState.update {
            it.copy(
                identifier = value,
                email = value,
                username = value,
                identifierError = null,
                emailError = null,
                usernameError = null,
                errorMessage = null,
            )
        }
    }

    fun onResetCodeChange(value: String) {
        _uiState.update {
            it.copy(
                resetCode = value,
                resetCodeError = null,
                errorMessage = null,
            )
        }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                newPassword = password,
                newPasswordError = null,
                errorMessage = null,
            )
        }
    }

    fun onConfirmPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                confirmPassword = password,
                confirmPasswordError = null,
                errorMessage = null,
            )
        }
    }

    fun onCurrentPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                currentPassword = password,
                currentPasswordError = null,
                errorMessage = null,
            )
        }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(showNewPassword = !it.showNewPassword) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(showConfirmPassword = !it.showConfirmPassword) }
    }

    fun toggleCurrentPasswordVisibility() {
        _uiState.update { it.copy(showCurrentPassword = !it.showCurrentPassword) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ─── Step 1: Request Reset Instructions ─────────────────────────────────

    fun requestResetInstructions() {
        val state = _uiState.value
        val raw = state.identifier.ifBlank { state.email.ifBlank { state.username } }.trim()

        if (raw.isBlank()) {
            _uiState.update { it.copy(identifierError = "Please enter your registered email or username.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repository.sendPasswordResetInstructions(raw)
            result.fold(
                onSuccess = { maskedEmail ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            maskedEmail = maskedEmail,
                            step = ForgotPasswordStep.RESET_FORM,
                            successMessage = "Reset instructions sent to $maskedEmail. Check your inbox and spam folder, then enter the code or link below.",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to send reset instructions. Please try again.",
                        )
                    }
                }
            )
        }
    }

    // ─── Step 2: Submit In-App Password Reset ────────────────────────────────

    fun submitPasswordReset() {
        val state = _uiState.value
        val code = state.resetCode.trim()
        val newPass = state.newPassword
        val confirmPass = state.confirmPassword

        var hasError = false
        var codeErr: String? = null
        var newErr: String? = null
        var confirmErr: String? = null

        if (code.isBlank()) {
            codeErr = "Please paste the reset code or link from your email."
            hasError = true
        }
        if (newPass.isBlank()) {
            newErr = "New password is required."
            hasError = true
        } else if (newPass.length < 8) {
            newErr = "New password must be at least 8 characters."
            hasError = true
        }
        if (confirmPass.isBlank()) {
            confirmErr = "Please confirm your new password."
            hasError = true
        } else if (newPass != confirmPass) {
            confirmErr = "Passwords do not match."
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    resetCodeError = codeErr,
                    newPasswordError = newErr,
                    confirmPasswordError = confirmErr,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repository.confirmPasswordReset(code, newPass)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = ForgotPasswordStep.SUCCESS,
                            successMessage = "Password reset successfully! You can now log in with your new password.",
                            resetCode = "",
                            newPassword = "",
                            confirmPassword = "",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to reset password. The code may be expired or invalid.",
                        )
                    }
                }
            )
        }
    }

    // ─── Alternative: Direct Change When Current Password Known ──────────────

    fun submitCurrentPasswordChange() {
        val state = _uiState.value
        val usernameOrEmail = state.identifier.ifBlank { state.username.ifBlank { state.email } }.trim()
        val currentPass = state.currentPassword
        val newPass = state.newPassword
        val confirmPass = state.confirmPassword

        var hasError = false
        var idErr: String? = null
        var currentErr: String? = null
        var newErr: String? = null
        var confirmErr: String? = null

        if (usernameOrEmail.isBlank()) {
            idErr = "Please enter your username or email."
            hasError = true
        }
        if (currentPass.isBlank()) {
            currentErr = "Please enter your current password."
            hasError = true
        }
        if (newPass.length < 8) {
            newErr = "New password must be at least 8 characters."
            hasError = true
        }
        if (newPass != confirmPass) {
            confirmErr = "Passwords do not match."
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    identifierError = idErr,
                    usernameError = idErr,
                    currentPasswordError = currentErr,
                    newPasswordError = newErr,
                    confirmPasswordError = confirmErr,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = repository.changePasswordForUsernameAccount(usernameOrEmail, currentPass, newPass)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = ForgotPasswordStep.SUCCESS,
                            successMessage = "Password updated successfully! You can now log in with your new password.",
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to update password. Please check your current password."
                        )
                    }
                }
            )
        }
    }

    // ─── Legacy compatibility methods ────────────────────────────────────────

    fun onEmailChange(newEmail: String) = onIdentifierChange(newEmail)
    fun onUsernameChange(newUsername: String) = onIdentifierChange(newUsername)
    fun submitEmailReset() = requestResetInstructions()
    fun submitUsernamePasswordChange() = submitCurrentPasswordChange()
    fun setUsernameOption(option: UsernameRecoveryOption) {
        _uiState.update { it.copy(usernameOption = option) }
    }
}
