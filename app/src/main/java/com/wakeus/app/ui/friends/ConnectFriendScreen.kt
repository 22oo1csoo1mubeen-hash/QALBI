package com.wakeus.app.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.ui.components.DemoBadge
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiCard
import com.wakeus.app.ui.components.QalbiTextField
import com.wakeus.app.ui.components.QalbiTopBar
import com.wakeus.app.ui.theme.MidnightBlue
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
            Spacer(Modifier.height(8.dp))
            DemoBadge()
            Spacer(Modifier.height(24.dp))

            // ── Section 1: Generate a Code ───────────────────────────────────
            QalbiCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text  = "Generate a Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Share this code with a friend so they can connect with you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                // Display generated code
                uiState.generatedCode?.let { code ->
                    Text(
                        text      = code.code,
                        style     = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color     = SubtleGold,
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text      = code.expiryLabel,
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    QalbiButton(
                        text    = stringResource(R.string.copy_code),
                        onClick = {
                            clipboard.setText(AnnotatedString(code.code))
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                QalbiButton(
                    text      = stringResource(R.string.generate_code),
                    onClick   = viewModel::generateCode,
                    isLoading = uiState.isGenerating,
                )
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
                    text  = stringResource(R.string.phase3_pairing_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                QalbiTextField(
                    value         = uiState.enteredCode,
                    onValueChange = viewModel::onEnteredCodeChange,
                    label         = "Friend's Code",
                    error         = uiState.enteredCodeError,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction      = ImeAction.Done,
                    ),
                )
                Spacer(Modifier.height(16.dp))

                QalbiButton(
                    text      = stringResource(R.string.connect),
                    onClick   = viewModel::submitCode,
                    isLoading = uiState.isConnecting,
                )

                // Result feedback
                uiState.connectionResult?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
