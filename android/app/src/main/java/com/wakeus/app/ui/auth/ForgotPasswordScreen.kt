package com.wakeus.app.ui.auth

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.QalbiRomanticButton
import com.wakeus.app.ui.components.QalbiRomanticTextField

private val WineTitle      = Color(0xFF5B1424)
private val WineText       = Color(0xFF452B33)
private val WineSub        = Color(0xFF7A5862)
private val WineAccent     = Color(0xFF982641)
private val CardBg         = Color(0xFFFFF9FA)
private val CardBorder     = Color(0xFFF3D4DC)
private val DividerLine    = Color(0xFFE8D0CE)
private val HeartAccent    = Color(0xFFC76F80)
private val SuccessBg      = Color(0xFFF1F8F3)
private val SuccessText    = Color(0xFF1E6335)
private val SuccessBorder  = Color(0xFFC7E2D0)
private val InfoBg         = Color(0xFFFDF2F4)
private val InfoBorder     = Color(0xFFF2D1D8)

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFCF5EE))
                .padding(innerPadding),
        ) {
            // Background Artwork
            Image(
                painter = painterResource(id = R.drawable.overall_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (uiState.step == ForgotPasswordStep.RESET_FORM) {
                                viewModel.setStep(ForgotPasswordStep.REQUEST)
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { role = Role.Button },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = WineTitle,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (uiState.step) {
                            ForgotPasswordStep.REQUEST -> "Forgot Password"
                            ForgotPasswordStep.RESET_FORM -> "Reset Password"
                            ForgotPasswordStep.SUCCESS -> "Success"
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = WineTitle,
                        ),
                    )
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Heart Emblem
                    Image(
                        painter = painterResource(id = R.drawable.ic_qalbi_heart_logo),
                        contentDescription = stringResource(R.string.cd_heart_logo),
                        modifier = Modifier.size(width = 62.dp, height = 52.dp),
                        contentScale = ContentScale.Fit,
                    )

                    Spacer(Modifier.height(12.dp))

                    when (uiState.step) {
                        ForgotPasswordStep.REQUEST -> {
                            // ─── STEP 1: REQUEST INSTRUCTIONS ─────────────────
                            RequestInstructionsStep(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateToLogin = onNavigateToLogin,
                            )
                        }

                        ForgotPasswordStep.RESET_FORM -> {
                            // ─── STEP 2: IN-APP RESET FORM ────────────────────
                            ResetPasswordFormStep(
                                uiState = uiState,
                                viewModel = viewModel,
                            )
                        }

                        ForgotPasswordStep.SUCCESS -> {
                            // ─── STEP 3: RESET SUCCESS VIEW ───────────────────
                            ResetSuccessStep(
                                uiState = uiState,
                                onNavigateToLogin = onNavigateToLogin,
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

/**
 * Step 1: User enters email or username and requests reset code/link.
 * Also offers direct password change if they know their current password.
 */
@Composable
private fun RequestInstructionsStep(
    uiState: ForgotPasswordUiState,
    viewModel: ForgotPasswordViewModel,
    onNavigateToLogin: () -> Unit,
) {
    Text(
        text = if (uiState.isChangePasswordMode) "Change Password" else "Account Recovery",
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = WineTitle,
        ),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = if (uiState.isChangePasswordMode) {
            "Verify your current password to set a new password."
        } else {
            "Enter your registered email address or username to receive password reset instructions."
        },
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = WineSub,
        ),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(18.dp))

    // Delicate Divider
    Row(
        modifier = Modifier.width(140.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = DividerLine)
        Text(text = "♥", fontSize = 10.sp, color = HeartAccent, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = DividerLine)
    }

    Spacer(Modifier.height(22.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!uiState.isChangePasswordMode) {
                // Standard Forgot Password Request
                QalbiRomanticTextField(
                    value = uiState.identifier,
                    onValueChange = viewModel::onIdentifierChange,
                    placeholder = "Email or Username",
                    leadingVector = Icons.Outlined.Person,
                    error = uiState.identifierError,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(InfoBg)
                        .border(BorderStroke(1.dp, InfoBorder), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = WineAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "We will send a reset link to your registered email address.",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                color = WineText,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                QalbiRomanticButton(
                    text = "Send Reset Instructions",
                    onClick = viewModel::requestResetInstructions,
                    isLoading = uiState.isLoading,
                )

                Spacer(Modifier.height(14.dp))

                // Toggle for having current password
                Text(
                    text = "Know your current password? Update directly",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = WineTitle,
                    ),
                    modifier = Modifier
                        .clickable { viewModel.toggleChangePasswordMode() }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                )

            } else {
                // "I Know Current Password" Direct Change Form
                QalbiRomanticTextField(
                    value = uiState.identifier,
                    onValueChange = viewModel::onIdentifierChange,
                    placeholder = "Email or Username",
                    leadingVector = Icons.Outlined.Person,
                    error = uiState.identifierError,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                QalbiRomanticTextField(
                    value = uiState.currentPassword,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    placeholder = "Current Password",
                    leadingVector = Icons.Outlined.Lock,
                    isPassword = true,
                    showPassword = uiState.showCurrentPassword,
                    onTogglePasswordVisibility = viewModel::toggleCurrentPasswordVisibility,
                    error = uiState.currentPasswordError,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                QalbiRomanticTextField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    placeholder = "New Password (min 8 characters)",
                    leadingVector = Icons.Outlined.Lock,
                    isPassword = true,
                    showPassword = uiState.showNewPassword,
                    onTogglePasswordVisibility = viewModel::toggleNewPasswordVisibility,
                    error = uiState.newPasswordError,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                QalbiRomanticTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    placeholder = "Confirm New Password",
                    leadingVector = Icons.Outlined.Lock,
                    isPassword = true,
                    showPassword = uiState.showNewPassword,
                    onTogglePasswordVisibility = viewModel::toggleNewPasswordVisibility,
                    error = uiState.confirmPasswordError,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                )

                Spacer(Modifier.height(20.dp))

                QalbiRomanticButton(
                    text = "Update Password",
                    onClick = viewModel::submitCurrentPasswordChange,
                    isLoading = uiState.isLoading,
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Forgot current password? Request reset code",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = WineTitle,
                    ),
                    modifier = Modifier
                        .clickable { viewModel.toggleChangePasswordMode() }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Back to Login link
    Row(
        modifier = Modifier
            .clickable(onClick = onNavigateToLogin)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Remember your password? ",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.5.sp,
                color = WineSub,
            ),
        )
        Text(
            text = "Log In",
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = WineTitle,
            ),
        )
    }
}

/**
 * Step 2: In-App Password Reset UI.
 * Allows entering the code or link from the reset email along with the new password.
 */
@Composable
private fun ResetPasswordFormStep(
    uiState: ForgotPasswordUiState,
    viewModel: ForgotPasswordViewModel,
) {
    Text(
        text = "Set New Password",
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = WineTitle,
        ),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = "Enter the reset code or link from your email and create your new password.",
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = WineSub,
        ),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(16.dp))

    // Information Banner
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SuccessBg)
            .border(BorderStroke(1.dp, SuccessBorder), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SuccessText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Reset Instructions Sent!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = SuccessText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "We sent instructions to ${uiState.maskedEmail ?: uiState.identifier}. You can tap the link in the email or paste the code / link below.",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = SuccessText,
                    ),
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Form Field 1: Reset Code or Link
            QalbiRomanticTextField(
                value = uiState.resetCode,
                onValueChange = viewModel::onResetCodeChange,
                placeholder = "Reset Code or Link (from email)",
                leadingVector = Icons.Outlined.Lock,
                error = uiState.resetCodeError,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Tip: You can paste the code or the entire link received in your email",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.5.sp,
                    color = WineSub,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // Form Field 2: New Password
            QalbiRomanticTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                placeholder = "New Password (min 8 characters)",
                leadingVector = Icons.Outlined.Lock,
                isPassword = true,
                showPassword = uiState.showNewPassword,
                onTogglePasswordVisibility = viewModel::toggleNewPasswordVisibility,
                error = uiState.newPasswordError,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // Form Field 3: Confirm New Password
            QalbiRomanticTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholder = "Confirm New Password",
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

            Spacer(Modifier.height(22.dp))

            // Primary Action: Submit Reset
            QalbiRomanticButton(
                text = "Reset & Update Password",
                onClick = viewModel::submitPasswordReset,
                isLoading = uiState.isLoading,
            )

            Spacer(Modifier.height(16.dp))

            // Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Resend link",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = WineTitle,
                    ),
                    modifier = Modifier
                        .clickable { viewModel.requestResetInstructions() }
                        .padding(4.dp),
                )

                Text(
                    text = "Change email",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = WineSub,
                    ),
                    modifier = Modifier
                        .clickable { viewModel.setStep(ForgotPasswordStep.REQUEST) }
                        .padding(4.dp),
                )
            }
        }
    }
}

/**
 * Step 3: Success View with celebratory romantic presentation and Login action.
 */
@Composable
private fun ResetSuccessStep(
    uiState: ForgotPasswordUiState,
    onNavigateToLogin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SuccessBg)
                    .border(BorderStroke(1.5.dp, SuccessBorder), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessText,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Password Reset Successful!",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = WineTitle,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = uiState.successMessage
                    ?: "Your password has been securely updated. You can now log in with your new credentials.",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = WineText,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(Modifier.height(28.dp))

            QalbiRomanticButton(
                text = "Back to Log In",
                onClick = onNavigateToLogin,
            )
        }
    }
}
