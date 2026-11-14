package com.wakeus.app.ui.friends

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.wakeus.app.viewmodel.ConnectFriendViewModel

private val FriendWineTitle   = Color(0xFF701831)
private val FriendWineAccent  = Color(0xFF80243C)
private val FriendWineSub     = Color(0xFF5C4046)
private val FriendCardBg      = Color(0xFFFFF7F4)
private val FriendCardBorder  = Color(0xFFF3C2C9)
private val FriendDividerLine = Color(0xFFE8D0CE)
private val FriendHeartAccent = Color(0xFFC76F80)
private val FriendCanvasBase  = Color(0xFFFCF5EE)

/**
 * Redesigned Connect to a Friend screen for QALBI.
 * Includes both Generate Code and Enter Code flows using the romantic design system.
 */
@Composable
fun ConnectFriendScreen(
    onBack: () -> Unit,
    viewModel: ConnectFriendViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Configure system bars for readable dark icons over the light romantic canvas
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

    LaunchedEffect(uiState.connectionError) {
        uiState.connectionError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.connectionResult) {
        uiState.connectionResult?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.generateError) {
        uiState.generateError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FriendCanvasBase)
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
                // Top Navigation Bar
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
                            tint = FriendWineTitle,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Connect to a Friend",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = FriendWineTitle,
                        ),
                    )
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Card 1: Share a Connection Code ─────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(FriendCardBg)
                            .border(BorderStroke(1.2.dp, FriendCardBorder), RoundedCornerShape(22.dp))
                            .padding(20.dp),
                    ) {
                        Column {
                            Text(
                                text = "Share a Connection Code",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = FriendWineTitle,
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Generate an 8-character single-use code and share it with someone you love. Codes expire in 10 minutes.",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    color = FriendWineSub,
                                ),
                            )

                            Spacer(Modifier.height(18.dp))

                            // Display Active Code
                            uiState.activeCode?.let { code ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFFFBEBE6))
                                        .border(BorderStroke(1.dp, Color(0xFFF0CCD2)), RoundedCornerShape(18.dp))
                                        .padding(vertical = 18.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = code.code,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 30.sp,
                                                letterSpacing = 6.sp,
                                                color = FriendWineTitle,
                                            ),
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = null,
                                                tint = FriendWineAccent,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "Valid for 10 minutes (single-use)",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 12.5.sp,
                                                    color = FriendWineSub,
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    QalbiRomanticButton(
                                        text = "Copy Code",
                                        onClick = {
                                            clipboard.setText(AnnotatedString(code.code))
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    OutlinedButton(
                                        onClick = viewModel::revokeActiveCode,
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .height(56.dp),
                                        shape = CircleShape,
                                        border = BorderStroke(1.5.dp, FriendWineAccent),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = FriendWineAccent,
                                        ),
                                    ) {
                                        if (uiState.isRevoking) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = FriendWineAccent,
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Text(
                                                text = "Cancel",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    color = FriendWineAccent,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState.activeCode == null) {
                                QalbiRomanticButton(
                                    text = "Generate Code",
                                    onClick = viewModel::generateCode,
                                    isLoading = uiState.isGenerating,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Decorative Divider ──────────────────────────────────
                    Row(
                        modifier = Modifier.width(160.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = FriendDividerLine,
                        )
                        Text(
                            text = "♥",
                            fontSize = 10.sp,
                            color = FriendHeartAccent,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = FriendDividerLine,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Card 2: Enter a Friend's Code ───────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(FriendCardBg)
                            .border(BorderStroke(1.2.dp, FriendCardBorder), RoundedCornerShape(22.dp))
                            .padding(20.dp),
                    ) {
                        Column {
                            Text(
                                text = "Enter a Friend's Code",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = FriendWineTitle,
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Enter the 8-character pairing code shared with you to connect both accounts immediately.",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp,
                                    color = FriendWineSub,
                                ),
                            )

                            Spacer(Modifier.height(18.dp))

                            // Code input field
                            QalbiRomanticTextField(
                                value = uiState.enteredCode,
                                onValueChange = viewModel::onEnteredCodeChange,
                                placeholder = "Pairing Code (e.g. 7X9K2M4P)",
                                leadingVector = Icons.Outlined.Lock,
                                error = uiState.enteredCodeError,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done,
                                ),
                            )

                            Spacer(Modifier.height(18.dp))

                            // Prospective friend preview before final connection
                            uiState.previewFriend?.let { prospective ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFFFDE7EC))
                                        .border(BorderStroke(1.2.dp, Color(0xFFE27B91)), RoundedCornerShape(18.dp))
                                        .padding(16.dp),
                                ) {
                                    Column {
                                        Text(
                                            text = "Connect with this friend?",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = FriendWineTitle,
                                            ),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(FriendWineAccent),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = prospective.creatorDisplayName.take(2).uppercase(),
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Color.White,
                                                    ),
                                                )
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = prospective.creatorDisplayName,
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.Serif,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = FriendWineTitle,
                                                    ),
                                                )
                                                Text(
                                                    text = "@${prospective.creatorUsername}",
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontSize = 13.sp,
                                                        color = FriendWineSub,
                                                    ),
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(14.dp))

                                        Row {
                                            QalbiRomanticButton(
                                                text = "Confirm & Connect",
                                                onClick = viewModel::submitCode,
                                                isLoading = uiState.isConnecting,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            OutlinedButton(
                                                onClick = viewModel::cancelPreview,
                                                modifier = Modifier
                                                    .weight(0.7f)
                                                    .height(56.dp),
                                                shape = CircleShape,
                                                border = BorderStroke(1.5.dp, FriendWineAccent),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = FriendWineAccent,
                                                ),
                                            ) {
                                                Text(
                                                    text = "Cancel",
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp,
                                                        color = FriendWineAccent,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                            }

                            if (uiState.previewFriend == null) {
                                QalbiRomanticButton(
                                    text = "Connect",
                                    onClick = viewModel::submitCode,
                                    isLoading = uiState.isConnecting || uiState.isInspecting,
                                )
                            }

                            // Success feedback banner
                            uiState.connectionResult?.let { resultMsg ->
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .border(BorderStroke(1.dp, Color(0xFFC8E6C9)), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = resultMsg,
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.5.sp,
                                                color = Color(0xFF1B5E20),
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        )
                                    }
                                }
                            }

                            // Error feedback banner
                            uiState.connectionError?.let { errorMsg ->
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFDE7EC))
                                        .border(BorderStroke(1.dp, Color(0xFFF3C2C9)), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = FriendWineAccent,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = errorMsg,
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.5.sp,
                                                color = FriendWineTitle,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(36.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConnectFriendScreenPreview() {
    QalbiTheme {
        ConnectFriendScreen(onBack = {})
    }
}
