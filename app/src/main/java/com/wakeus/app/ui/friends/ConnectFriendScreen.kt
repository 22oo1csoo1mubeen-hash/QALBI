package com.wakeus.app.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiOutlinedButton
import com.wakeus.app.ui.components.QalbiTextField
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.MutedRose
import com.wakeus.app.ui.theme.SubtleGold
import com.wakeus.app.viewmodel.ConnectFriendViewModel

@Composable
fun ConnectFriendScreen(
    onBack: () -> Unit,
    viewModel: ConnectFriendViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

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
        containerColor = MidnightBlue,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = { QalbiTopBar(title = "Connect to a Friend", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Section 1: Generate a Code ───────────────────────────────────
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = "Share a Connection Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Generate an 8-character single-use code and share it with someone you love. Codes expire in 10 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                // Display active generated code
                uiState.activeCode?.let { code ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = code.code,
                                style      = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color      = SubtleGold,
                                textAlign  = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text  = "Valid for 10 minutes (single-use)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        QalbiButton(
                            text     = "Copy Code",
                            onClick  = {
                                clipboard.setText(AnnotatedString(code.code))
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        QalbiOutlinedButton(
                            text     = "Cancel",
                            onClick  = viewModel::revokeActiveCode,
                            modifier = Modifier.weight(1f),
                            isLoading = uiState.isRevoking,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                if (uiState.activeCode == null) {
                    QalbiButton(
                        text      = "Generate Code",
                        onClick   = viewModel::generateCode,
                        isLoading = uiState.isGenerating,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(24.dp))

            // ── Section 2: Enter a Code ──────────────────────────────────────
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = "Enter a Friend's Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Enter the 8-character pairing code shared with you to connect both accounts immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                QalbiTextField(
                    value         = uiState.enteredCode,
                    onValueChange = viewModel::onEnteredCodeChange,
                    label         = "Pairing Code (e.g. 7X9K2M4P)",
                    error         = uiState.enteredCodeError,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction      = ImeAction.Done,
                    ),
                )
                Spacer(Modifier.height(16.dp))

                // Prospective friend preview before final redemption
                uiState.previewFriend?.let { prospective ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(14.dp),
                    ) {
                        Column {
                            Text(
                                text  = "Connect with this friend?",
                                style = MaterialTheme.typography.labelMedium,
                                color = SubtleGold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MutedRose),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text  = prospective.creatorDisplayName.take(2).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text  = prospective.creatorDisplayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text  = "@${prospective.creatorUsername}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row {
                                QalbiButton(
                                    text      = "Confirm & Connect",
                                    onClick   = viewModel::submitCode,
                                    isLoading = uiState.isConnecting,
                                    modifier  = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                QalbiOutlinedButton(
                                    text     = "Cancel",
                                    onClick  = viewModel::cancelPreview,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (uiState.previewFriend == null) {
                    QalbiButton(
                        text      = "Connect",
                        onClick   = viewModel::submitCode,
                        isLoading = uiState.isConnecting || uiState.isInspecting,
                    )
                }

                // Success feedback
                uiState.connectionResult?.let { resultMsg ->
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            .padding(12.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.secondary,
                            modifier           = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = resultMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
