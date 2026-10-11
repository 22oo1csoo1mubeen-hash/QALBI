package com.wakeus.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.DemoBadge
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiTextField
import com.wakeus.app.ui.components.QalbiTextButton
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate on success
    LaunchedEffect(uiState.successUser) {
        if (uiState.successUser != null) onRegistered()
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MidnightBlue,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = {
            QalbiTopBar(title = stringResource(R.string.create_account), onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Demo notice
            DemoBadge()
            Spacer(Modifier.height(4.dp))
            Text(
                text      = "This creates a local demo account. No data is sent to any server.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Display Name
            QalbiTextField(
                value         = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label         = stringResource(R.string.display_name_hint),
                error         = uiState.displayNameError,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(12.dp))

            // Username
            QalbiTextField(
                value         = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label         = stringResource(R.string.username_hint),
                error         = uiState.usernameError,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction    = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(12.dp))

            // Email
            QalbiTextField(
                value         = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label         = stringResource(R.string.email_hint),
                error         = uiState.emailError,
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(12.dp))

            // Password
            QalbiTextField(
                value                      = uiState.password,
                onValueChange              = viewModel::onPasswordChange,
                label                      = stringResource(R.string.password_hint),
                error                      = uiState.passwordError,
                isPassword                 = true,
                showPassword               = uiState.showPassword,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                modifier                   = Modifier.fillMaxWidth(),
                keyboardOptions            = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(12.dp))

            // Confirm Password
            QalbiTextField(
                value                      = uiState.confirmPassword,
                onValueChange              = viewModel::onConfirmPasswordChange,
                label                      = stringResource(R.string.confirm_password_hint),
                error                      = uiState.confirmPasswordError,
                isPassword                 = true,
                showPassword               = uiState.showConfirmPassword,
                onTogglePasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                modifier                   = Modifier.fillMaxWidth(),
                keyboardOptions            = KeyboardOptions(imeAction = ImeAction.Done),
                enabled                    = !uiState.isLoading,
            )
            Spacer(Modifier.height(28.dp))

            QalbiButton(
                text      = stringResource(R.string.create_account),
                onClick   = viewModel::submit,
                isLoading = uiState.isLoading,
            )
            Spacer(Modifier.height(16.dp))

            QalbiTextButton(
                text    = stringResource(R.string.already_have_account),
                onClick = onNavigateToLogin,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
