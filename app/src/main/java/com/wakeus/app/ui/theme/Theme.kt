package com.wakeus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Dark colour scheme ──────────────────────────────────────────────────────
private val QalbiDarkColorScheme = darkColorScheme(
    primary           = SubtleGold,
    onPrimary         = MidnightBlue,
    primaryContainer  = SubtleGoldDark,
    onPrimaryContainer = WarmIvory,

    secondary           = MutedRose,
    onSecondary         = MidnightBlue,
    secondaryContainer  = MutedRoseDark,
    onSecondaryContainer = WarmIvory,

    tertiary           = MutedRoseLight,
    onTertiary         = MidnightBlue,
    tertiaryContainer  = MutedRoseDark,
    onTertiaryContainer = WarmIvory,

    background  = MidnightBlue,
    onBackground = WarmIvory,

    surface        = MidnightBlueSurface,
    onSurface      = WarmIvory,
    surfaceVariant  = MidnightBlueLight,
    onSurfaceVariant = IvoryMuted,

    outline       = DividerColor,
    outlineVariant = DividerColor,

    error   = ErrorRed,
    onError = WarmIvory,
    errorContainer  = MutedRoseDark,
    onErrorContainer = WarmIvory,

    scrim = ScrimColor,
)

// ─── Light colour scheme ─────────────────────────────────────────────────────
// QALBI is primarily dark-themed; light scheme is provided for completeness.
private val QalbiLightColorScheme = lightColorScheme(
    primary           = SubtleGoldDark,
    onPrimary         = WarmIvory,
    primaryContainer  = SubtleGoldLight,
    onPrimaryContainer = MidnightBlue,

    secondary           = MutedRoseDark,
    onSecondary         = WarmIvory,
    secondaryContainer  = MutedRoseLight,
    onSecondaryContainer = MidnightBlue,

    background   = Color(0xFFF8F2EA),
    onBackground = MidnightBlue,

    surface      = Color(0xFFFFFFFF),
    onSurface    = MidnightBlue,

    error   = ErrorRed,
    onError = WarmIvory,
)

@Composable
fun QalbiTheme(
    darkTheme: Boolean = true, // QALBI defaults to dark
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) QalbiDarkColorScheme else QalbiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = QalbiTypography,
        content     = content,
    )
}
