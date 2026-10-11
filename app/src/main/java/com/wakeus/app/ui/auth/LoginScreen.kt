package com.wakeus.app.ui.auth

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiTextField
import com.wakeus.app.ui.components.QalbiTextButton
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successUser) {
        if (uiState.successUser != null) onLoggedIn()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MidnightBlue,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = { QalbiTopBar(title = stringResource(R.string.log_in), onBack = onBack) },
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
            Spacer(Modifier.height(16.dp))

            Text(
                text  = "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = "Sign in using your registered email address or username.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            QalbiTextField(
                value         = uiState.emailOrUsername,
                onValueChange = viewModel::onEmailChange,
                label         = "Email or Username",
                error         = uiState.emailError,
                modifier      = Modifier.fillMaxWidth(),
                enabled       = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(12.dp))

            QalbiTextField(
                value                      = uiState.password,
                onValueChange              = viewModel::onPasswordChange,
                label                      = stringResource(R.string.password_hint),
                error                      = uiState.passwordError,
                isPassword                 = true,
                showPassword               = uiState.showPassword,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                modifier                   = Modifier.fillMaxWidth(),
                keyboardOptions            = KeyboardOptions(imeAction = ImeAction.Done),
                enabled                    = !uiState.isLoading,
            )
            Spacer(Modifier.height(32.dp))

            QalbiButton(
                text      = stringResource(R.string.log_in),
                onClick   = viewModel::submit,
                isLoading = uiState.isLoading,
            )
            Spacer(Modifier.height(16.dp))

            QalbiTextButton(
                text    = stringResource(R.string.dont_have_account),
                onClick = onNavigateToRegister,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
