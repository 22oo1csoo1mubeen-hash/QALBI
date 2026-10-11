package com.wakeus.app.ui.welcome

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.wakeus.app.R
import com.wakeus.app.ui.theme.QalbiTheme

// ─── Landing Page Palette ───────────────────────────────────────────────────
private val LandingWineDeep       = Color(0xFF6B1D2F)
private val LandingWineHeadline   = Color(0xFF5B1424)
private val LandingWineBody       = Color(0xFF452B33)
private val LandingWineFeature    = Color(0xFF382329)
private val LandingBadgeBg        = Color(0xFFFBE4DE)
private val LandingPrimaryTop     = Color(0xFF982641)
private val LandingPrimaryBottom  = Color(0xFF6F182F)
private val LandingSecondaryBorder = Color(0xFF861B35)
private val LandingSecondaryText  = Color(0xFF7A1830)
private val LandingDividerLine    = Color(0xFFE8D0CE)
private val LandingHeartAccent    = Color(0xFFC76F80)
private val LandingFooterText     = Color(0xFF7A656B)
private val LandingFooterAccent   = Color(0xFFC5485E)
private val LandingCanvasBase     = Color(0xFFFCF5EE)

/**
 * Redesigned Landing / Welcome Screen for QALBI.
 * Faithfully replicates `landing-reference.png` using bundled `landing_background.png`.
 *
 * @param onGetStarted Callback for primary CTA (navigates to registration).
 * @param onLogIn Callback for secondary CTA (navigates to login).
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
) {
    // Configure system bars for readable dark icons over the light background canvas
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LandingCanvasBase)
    ) {
        // ── 1. Bundled Background Illustration ──────────────────────────────
        Image(
            painter = painterResource(id = R.drawable.landing_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )

        // ── 2. Foreground UI Content ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))

            // ── Logo & Wordmark ─────────────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.ic_qalbi_heart_logo),
                contentDescription = stringResource(R.string.cd_heart_logo),
                modifier = Modifier.size(width = 56.dp, height = 48.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.landing_wordmark),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    letterSpacing = 6.sp,
                    color = LandingWineDeep,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── Title & Tagline ─────────────────────────────────────────────
            Text(
                text = stringResource(R.string.landing_headline),
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    color = LandingWineHeadline,
                    letterSpacing = (-0.2).sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_landing_header_accent),
                contentDescription = null,
                modifier = Modifier
                    .width(140.dp)
                    .height(20.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.landing_subtitle),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = LandingWineBody,
                ),
                textAlign = TextAlign.Center,
            )

            // ── Middle Illustration Spacer ──────────────────────────────────
            // Accommodates the balcony illustration and glowing heart line
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 180.dp)
            )

            // ── Privacy & Trust Highlights ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LandingPrivacyRow(
                    iconResId = R.drawable.ic_phone_off,
                    text = stringResource(R.string.landing_privacy_no_phone),
                    accessibilityNote = stringResource(R.string.no_number_required),
                )
                LandingPrivacyRow(
                    iconResId = R.drawable.ic_lock_closed,
                    text = stringResource(R.string.landing_privacy_codes),
                    accessibilityNote = stringResource(R.string.privacy_note),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Action Buttons ──────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primary Action: Get Started
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x30661226),
                            spotColor = Color(0x40661226),
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(LandingPrimaryTop, LandingPrimaryBottom),
                                ),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Left watermark heart symbol
                        Image(
                            painter = painterResource(id = R.drawable.ic_qalbi_heart_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 14.dp)
                                .size(36.dp)
                                .alpha(0.18f),
                            colorFilter = ColorFilter.tint(Color.White),
                        )

                        // Button Label
                        Text(
                            text = stringResource(R.string.get_started),
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                color = Color.White,
                            ),
                            textAlign = TextAlign.Center,
                        )

                        // Right forward arrow
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_forward_slender),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 22.dp)
                                .size(20.dp),
                        )
                    }
                }

                // Secondary Action: Log In
                OutlinedButton(
                    onClick = onLogIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, LandingSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = LandingSecondaryText,
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.log_in),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = LandingSecondaryText,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── Footer Note ─────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                // Divider line with centered heart
                Row(
                    modifier = Modifier.width(160.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = LandingDividerLine,
                    )
                    Text(
                        text = "♥",
                        fontSize = 10.sp,
                        color = LandingHeartAccent,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = LandingDividerLine,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Poetic footer message
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.landing_footer_prefix),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = LandingFooterText,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.landing_footer_accent),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            color = LandingFooterAccent,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LandingPrivacyRow(
    @DrawableRes iconResId: Int,
    text: String,
    accessibilityNote: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$text. $accessibilityNote"
            },
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = LandingBadgeBg,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = LandingWineFeature,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    QalbiTheme {
        WelcomeScreen(
            onGetStarted = {},
            onLogIn = {},
        )
    }
}
