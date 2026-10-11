package com.wakeus.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakeus.app.data.auth.AuthState
import com.wakeus.app.data.model.User
import com.wakeus.app.data.repository.QalbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
)

class ProfileViewModel(
    private val repository: QalbiRepository = QalbiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            repository.authState.collect { authState: AuthState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                user = authState.user,
                                isLoading = false,
                                error = null,
                                isLoggedOut = false
                            )
                        }
                    }
                    is AuthState.Initializing -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.update {
                            it.copy(
                                user = null,
                                isLoading = false,
                                isLoggedOut = true
                            )
                        }
                    }
                    is AuthState.ConfigurationRequired -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = authState.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.logout()
            _uiState.update { it.copy(isLoading = false, user = null, isLoggedOut = true) }
        }
    }
}
