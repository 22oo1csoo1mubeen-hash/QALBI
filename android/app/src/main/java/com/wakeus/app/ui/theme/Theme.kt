package com.wakeus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── QALBI Romantic Unified Theme ──────────────────────────────────────────
// Entire application uses romantic palette: warm ivory, blush pink, and rich wine.

private val QalbiRomanticColorScheme = lightColorScheme(
    primary             = RomanticWine,
    onPrimary           = Color.White,
    primaryContainer    = RomanticBlush,
    onPrimaryContainer  = RomanticWineDeep,

    secondary           = RomanticRose,
    onSecondary         = Color.White,
    secondaryContainer  = RomanticBlush,
    onSecondaryContainer = RomanticWine,

    tertiary            = RomanticRoseLight,
    onTertiary          = Color.White,
    tertiaryContainer   = RomanticBlush,
    onTertiaryContainer = RomanticWine,

    background          = RomanticWarmBg,
    onBackground        = RomanticDarkText,

    surface             = RomanticCardBg,
    onSurface           = RomanticDarkText,
    surfaceVariant      = RomanticBlush,
    onSurfaceVariant    = RomanticMutedText,

    outline             = RomanticBlushBorder,
    outlineVariant      = RomanticDivider,

    error               = ErrorRed,
    onError             = Color.White,
    errorContainer      = Color(0xFFFDE8E8),
    onErrorContainer    = ErrorRed,

    scrim               = ScrimColor,
)

@Composable
fun QalbiTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = QalbiRomanticColorScheme,
        typography  = QalbiTypography,
        content     = content,
    )
}
