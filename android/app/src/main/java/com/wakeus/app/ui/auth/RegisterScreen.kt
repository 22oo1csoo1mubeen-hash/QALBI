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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.wakeus.app.viewmodel.RegisterViewModel
import com.wakeus.app.viewmodel.RegistrationMethod

private val RegWineTitle       = Color(0xFF701831)
private val RegWineSub         = Color(0xFF6B5358)
private val RegSegmentBg       = Color(0xFFFBEBE6)
private val RegSegmentSelected = Color(0xFF80243C)
private val RegDividerLine     = Color(0xFFE8D0CE)
private val RegHeartAccent     = Color(0xFFC76F80)
private val RegFooterSlogan    = Color(0xFF8B7177)
private val RegCanvasBase      = Color(0xFFFCF5EE)

/**
 * Redesigned Registration Screen for QALBI.
 * Faithfully replicates `registration-reference.png` using shared `overall_background.png`.
 *
 * @param onRegistered Callback on successful registration.
 * @param onNavigateToLogin Callback to navigate to login.
 * @param onBack Callback for top back navigation.
 */
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(),
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
        if (uiState.successUser != null) onRegistered()
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
                .background(RegCanvasBase)
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
                            tint = RegWineTitle,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.create_account),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = RegWineTitle,
                        ),
                    )
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(Modifier.height(14.dp))

                    // Left-Aligned Heart Icon
                    Icon(
                        painter = painterResource(id = R.drawable.ic_heart_outline_simple),
                        contentDescription = null,
                        tint = RegWineTitle,
                        modifier = Modifier.size(26.dp),
                    )

                    Spacer(Modifier.height(10.dp))

                    // Heading: A little closer starts here.
                    Text(
                        text = "A little closer starts here.",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = RegWineTitle,
                            letterSpacing = (-0.2).sp,
                        ),
                    )

                    Spacer(Modifier.height(6.dp))

                    // Supporting Text
                    Text(
                        text = "Create your QALBI account.",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = RegWineSub,
                        ),
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Segmented Account-Mode Selector ─────────────────────
                    RegistrationMethodSegmentedControl(
                        selectedMethod = uiState.registrationMethod,
                        onMethodSelected = viewModel::setRegistrationMethod,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(18.dp))

                    // Username-only explanation hint
                    if (uiState.registrationMethod == RegistrationMethod.USERNAME) {
                        Text(
                            text = "No email required. Your unique username is your login identifier. (Password recovery via email will not be available.)",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = RegWineSub,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    // ── Form Fields ─────────────────────────────────────────

                    // 1. Display Name
                    QalbiRomanticTextField(
                        value = uiState.displayName,
                        onValueChange = viewModel::onDisplayNameChange,
                        placeholder = stringResource(R.string.display_name_hint),
                        leadingVector = Icons.Outlined.Person,
                        error = uiState.displayNameError,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    // 2. Username
                    QalbiRomanticTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        placeholder = stringResource(R.string.username_hint),
                        leadingPainter = painterResource(id = R.drawable.ic_at_symbol),
                        error = uiState.usernameError,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = if (uiState.registrationMethod == RegistrationMethod.EMAIL) ImeAction.Next else ImeAction.Next,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    // 3. Email (only when EMAIL method is selected)
                    if (uiState.registrationMethod == RegistrationMethod.EMAIL) {
                        QalbiRomanticTextField(
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            placeholder = stringResource(R.string.email_hint),
                            leadingVector = Icons.Outlined.MailOutline,
                            error = uiState.emailError,
                            enabled = !uiState.isLoading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // 4. Password
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
                            imeAction = ImeAction.Next,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    // 5. Confirm Password
                    QalbiRomanticTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        placeholder = stringResource(R.string.confirm_password_hint),
                        leadingVector = Icons.Outlined.Lock,
                        isPassword = true,
                        showPassword = uiState.showConfirmPassword,
                        onTogglePasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                        error = uiState.confirmPasswordError,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                        ),
                    )

                    Spacer(Modifier.height(26.dp))

                    // ── Primary Action: Create account ──────────────────────
                    QalbiRomanticButton(
                        text = stringResource(R.string.create_account),
                        onClick = viewModel::submit,
                        isLoading = uiState.isLoading,
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Footer Navigation Link ──────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToLogin)
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            text = "Already have an account? ",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.5.sp,
                                color = RegWineSub,
                            ),
                        )
                        Text(
                            text = "Log in",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = RegWineTitle,
                            ),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Bottom Decorative Element & Slogan ──────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.width(160.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = RegDividerLine,
                            )
                            Text(
                                text = "♥",
                                fontSize = 10.sp,
                                color = RegHeartAccent,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = RegDividerLine,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Made with care, for the people who matter.",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.5.sp,
                                color = RegFooterSlogan,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

/**
 * Segmented control for choosing between "Email & username" and "Username only".
 * Styled with pill shape, smooth selection pill, and wine accents.
 */
@Composable
private fun RegistrationMethodSegmentedControl(
    selectedMethod: RegistrationMethod,
    onMethodSelected: (RegistrationMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(RegSegmentBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val emailSelected = selectedMethod == RegistrationMethod.EMAIL
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(
                    if (emailSelected) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF8E253E), RegSegmentSelected),
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    },
                )
                .clickable { onMethodSelected(RegistrationMethod.EMAIL) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Email & username",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (emailSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (emailSelected) Color.White else RegSegmentSelected,
                ),
            )
        }

        val usernameSelected = selectedMethod == RegistrationMethod.USERNAME
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(
                    if (usernameSelected) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF8E253E), RegSegmentSelected),
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    },
                )
                .clickable { onMethodSelected(RegistrationMethod.USERNAME) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Username only",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (usernameSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (usernameSelected) Color.White else RegSegmentSelected,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    QalbiTheme {
        RegisterScreen(
            onRegistered = {},
            onNavigateToLogin = {},
            onBack = {},
        )
    }
}
