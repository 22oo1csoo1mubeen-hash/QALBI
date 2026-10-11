package com.wakeus.app.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeus.app.R
import com.wakeus.app.ui.components.QalbiButton
import com.wakeus.app.ui.components.QalbiOutlinedButton
import com.wakeus.app.ui.theme.MidnightBlue
import com.wakeus.app.ui.theme.SubtleGold

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Logo & Branding ──────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.cd_heart_logo),
                modifier           = Modifier.size(64.dp),
                tint               = SubtleGold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text      = "QALBI",
                style     = MaterialTheme.typography.displaySmall,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = stringResource(R.string.tagline),
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── App Description ──────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = "Wake the people you love — gently, remotely, and without exchanging numbers.",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            // Privacy features row
            PrivacyFeatureRow(
                icon = Icons.Filled.PhoneDisabled,
                text = stringResource(R.string.no_number_required),
            )
            Spacer(Modifier.height(12.dp))
            PrivacyFeatureRow(
                icon = Icons.Filled.Lock,
                text = "Connections use secure invitation codes",
            )
        }

        Spacer(Modifier.height(48.dp))

        // ── Actions ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            QalbiButton(
                text    = stringResource(R.string.get_started),
                onClick = onGetStarted,
            )
            Spacer(Modifier.height(12.dp))
            QalbiOutlinedButton(
                text    = stringResource(R.string.log_in),
                onClick = onLogIn,
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text(
                text      = stringResource(R.string.privacy_note),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PrivacyFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = SubtleGold,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
