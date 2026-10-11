package com.wakeus.app.ui.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.QalbiRomanticButton
import com.wakeus.app.ui.components.QalbiRomanticTextField
import com.wakeus.app.ui.theme.QalbiTheme
import com.wakeus.app.viewmodel.LoginViewModel

private val LoginWineTitle       = Color(0xFF701831)
private val LoginWineText        = Color(0xFF6B5358)
private val LoginDividerLine     = Color(0xFFE8D0CE)
private val LoginHeartAccent     = Color(0xFFC76F80)
private val LoginCanvasBase      = Color(0xFFFCF5EE)

/**
 * Redesigned Login Screen for QALBI.
 * Faithfully replicates `login-reference.png` using shared `overall_background.png`.
 *
 * @param onLoggedIn Callback on successful authentication.
 * @param onNavigateToRegister Callback to navigate to registration.
 * @param onBack Callback for top back navigation.
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Configure system bars for dark icons over the light romantic canvas
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LoginCanvasBase)
                .padding(innerPadding),
        ) {
            // ── 1. Shared Romantic Background Canvas ────────────────────────
            Image(
                painter = painterResource(id = R.drawable.overall_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )

            // ── 2. Foreground Content ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Top Navigation Bar: Back Arrow + Left-Aligned Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = LoginWineTitle,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.log_in),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = LoginWineTitle,
                        ),
                    )
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(18.dp))

                    // Brand Heart Logo with Sparkle
                    Image(
                        painter = painterResource(id = R.drawable.ic_qalbi_heart_logo),
                        contentDescription = stringResource(R.string.cd_heart_logo),
                        modifier = Modifier.size(width = 66.dp, height = 56.dp),
                        contentScale = ContentScale.Fit,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Heading: Welcome back
                    Text(
                        text = "Welcome back",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = LoginWineTitle,
                            letterSpacing = (-0.2).sp,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(6.dp))

                    // Supporting Text
                    Text(
                        text = "Sign in using your registered email address\nor username.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = LoginWineText,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(18.dp))

                    // Delicate Divider with Centered Heart
                    Row(
                        modifier = Modifier.width(160.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = LoginDividerLine,
                        )
                        Text(
                            text = "♥",
                            fontSize = 10.sp,
                            color = LoginHeartAccent,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = LoginDividerLine,
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // Form Field 1: Email or Username
                    QalbiRomanticTextField(
                        value = uiState.emailOrUsername,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "Email or Username",
                        leadingVector = Icons.Outlined.Person,
                        error = uiState.emailError,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    Spacer(Modifier.height(14.dp))

                    // Form Field 2: Password
                    QalbiRomanticTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = stringResource(R.string.password_hint),
                        leadingVector = Icons.Outlined.Lock,
                        isPassword = true,
                        showPassword = uiState.showPassword,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        error = uiState.passwordError,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                        ),
                    )

                    Spacer(Modifier.height(30.dp))

                    // Primary Action: Log In
                    QalbiRomanticButton(
                        text = stringResource(R.string.log_in),
                        onClick = viewModel::submit,
                        isLoading = uiState.isLoading,
                    )

                    Spacer(Modifier.height(22.dp))

                    // Footer Navigation Link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToRegister)
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.5.sp,
                                color = LoginWineText,
                            ),
                        )
                        Text(
                            text = "Sign Up",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = LoginWineTitle,
                            ),
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    QalbiTheme {
        LoginScreen(
            onLoggedIn = {},
            onNavigateToRegister = {},
            onBack = {},
        )
    }
}
