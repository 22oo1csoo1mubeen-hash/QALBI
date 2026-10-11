package com.wakeus.app.ui.profile

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.viewmodel.ProfileViewModel

private val ScreenBgFallback = Color(0xFFFBF6F3)
private val DeepWineTitle = Color(0xFF4A1424)
private val DeepWinePrimary = Color(0xFF8B253E)
private val DarkWineText = Color(0xFF3B1320)
private val MutedWineText = Color(0xFF6B3B48)
private val LabelWineText = Color(0xFF7C5560)
private val ValueDarkText = Color(0xFF301019)
private val CardBg = Color(0xFFFFF9FA)
private val CardBorderColor = Color(0xFFF3D4DC)
private val CardDividerColor = Color(0xFFF6E7EB)
private val IconContainerBg = Color(0xFFFDECEF)
private val SettingsBorderColor = Color(0xFFC75570)
private val LogoutGradientTop = Color(0xFFBF3455)
private val LogoutGradientBottom = Color(0xFF781830)

@Composable
fun ProfileScreen(
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    val user = uiState.user
    val displayName = user?.displayName.orEmpty().ifBlank { "User" }
    val initial = displayName.firstOrNull()?.toString()?.uppercase() ?: "U"
    val username = user?.username.orEmpty().ifBlank { "username" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgFallback)
    ) {
        // Mandatory shared background asset
        Image(
            painter = painterResource(id = R.drawable.overall_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar matching profile-reference.png
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepWinePrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = "Profile",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = DeepWineTitle,
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                if (uiState.isLoading && user == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = DeepWinePrimary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                        )
                    }
                } else {
                    // Avatar & Identity Section
                    Box(
                        modifier = Modifier.size(116.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Outer subtle ring/halo
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .clip(CircleShape)
                                .background(Color(0x20E57388))
                                .border(1.5.dp, Color(0x60F2A9B8), CircleShape),
                        )

                        // Main gradient avatar circle
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFEB7288), Color(0xFF912440)),
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initial,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 42.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Romantic heart badge nestled on bottom-right of avatar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 4.dp, bottom = 4.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD3395F))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = displayName,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = DarkWineText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "@$username",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = MutedWineText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Profile Details Card matching profile-reference.png
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        ) {
                            // Row 1: Display Name
                            ProfileDetailRow(
                                icon = Icons.Outlined.Person,
                                label = "Display Name",
                                value = displayName,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                                color = CardDividerColor,
                                thickness = 1.dp,
                            )

                            // Row 2: Username
                            ProfileDetailRow(
                                drawableRes = R.drawable.ic_at_symbol,
                                label = "Username",
                                value = "@$username",
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                                color = CardDividerColor,
                                thickness = 1.dp,
                            )

                            // Row 3: Account Identifier
                            val accountIdValue = user?.email?.takeIf { it.isNotBlank() }
                                ?: "Username-only account (@$username)"
                            ProfileDetailRow(
                                icon = Icons.Outlined.Shield,
                                label = "Account Identifier",
                                value = accountIdValue,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                                color = CardDividerColor,
                                thickness = 1.dp,
                            )

                            // Row 4: Account Type
                            val accountTypeValue = user?.accountType?.name ?: "USERNAME"
                            ProfileDetailRow(
                                icon = Icons.Outlined.Badge,
                                label = "Account Type",
                                value = accountTypeValue,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Settings Button matching profile-reference.png
                    OutlinedButton(
                        onClick = onSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardBg,
                            contentColor = DeepWinePrimary,
                        ),
                        border = BorderStroke(1.2.dp, SettingsBorderColor),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = DeepWinePrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Settings",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = DeepWinePrimary,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Log Out Button matching profile-reference.png
                    Button(
                        onClick = viewModel::logout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(27.dp),
                                ambientColor = Color(0x35661226),
                                spotColor = Color(0x45661226),
                            ),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !uiState.isLoading,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(LogoutGradientTop, LogoutGradientBottom),
                                    ),
                                    shape = RoundedCornerShape(27.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Log Out",
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                    )
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

@Composable
private fun ProfileDetailRow(
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading rounded squircle icon container
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IconContainerBg),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepWinePrimary,
                    modifier = Modifier.size(20.dp),
                )
            } else if (drawableRes != null) {
                Icon(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    tint = DeepWinePrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = LabelWineText,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ValueDarkText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
