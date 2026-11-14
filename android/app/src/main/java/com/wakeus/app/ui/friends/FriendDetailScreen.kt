package com.wakeus.app.ui.friends

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeus.app.R
import com.wakeus.app.data.model.AvatarPreset
import com.wakeus.app.ui.components.QalbiRomanticButton
import com.wakeus.app.viewmodel.FriendDetailViewModel

private val ScreenBgFallback = Color(0xFFFBF6F3)
private val DeepWine = Color(0xFF80243C)
private val DarkWineText = Color(0xFF331119)
private val MutedWineText = Color(0xFF7A5862)
private val CardBg = Color(0xFFFFF9F9)
private val CardBorderColor = Color(0xFFF3D5DB)

@Composable
fun FriendDetailScreen(
    friendId: String,
    onBack: () -> Unit,
    onWakeUp: (String) -> Unit,
    viewModel: FriendDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    LaunchedEffect(friendId) {
        viewModel.loadFriend(friendId)
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Disconnect Confirmation Dialog
    if (uiState.showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDisconnect,
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFFFFF9F9),
            title = {
                Text(
                    text = "Disconnect Friend?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = DarkWineText,
                )
            },
            text = {
                Text(
                    text = "This will remove the connection between both of you. You will no longer be able to send wake-up requests to each other unless you pair again.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MutedWineText,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDisconnect(onBack) }) {
                    Text("Disconnect", color = Color(0xFFB00020), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDisconnect) {
                    Text("Cancel", color = MutedWineText)
                }
            },
        )
    }

    // Block Confirmation Dialog
    if (uiState.showBlockDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBlock,
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFFFFF9F9),
            title = {
                Text(
                    text = "Block User?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = DarkWineText,
                )
            },
            text = {
                Text(
                    text = "Blocking will sever this connection immediately and prevent this user from pairing or connecting with you in the future.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MutedWineText,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmBlock(onBack) }) {
                    Text("Block", color = Color(0xFFB00020), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissBlock) {
                    Text("Cancel", color = MutedWineText)
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgFallback)
    ) {
        Image(
            painter = painterResource(id = R.drawable.overall_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepWine,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Friend Profile",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = DeepWine,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))

                val friend = uiState.friend
                if (friend != null) {
                    // Large Initials Avatar with romantic gradient
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(DeepWine, Color(0xFFA03350))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = friend.initials.ifBlank { "?" },
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = friend.displayName,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = DarkWineText,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "@${friend.username}",
                        fontSize = 14.sp,
                        color = MutedWineText,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Connection details card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = DeepWine,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Connection Status",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = DeepWine,
                                )
                                Spacer(Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFFCEDF0),
                                ) {
                                    Text(
                                        text = "Connected \uD83D\uDD17",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepWine,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Paired via secure single-use code. No phone number or personal details shared across distance.",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MutedWineText,
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Primary Action: Wake Up
                    QalbiRomanticButton(
                        text = "Wake Up Friend",
                        onClick = { onWakeUp(friend.id) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Alarm,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Disconnect Button
                    OutlinedButton(
                        onClick = viewModel::promptDisconnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFE5A6B3)),
                        enabled = !uiState.isActionInProgress,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = DeepWine,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isActionInProgress) "Disconnecting..." else "Disconnect Friend",
                            color = DeepWine,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Block Action
                    TextButton(
                        onClick = viewModel::promptBlock,
                        enabled = !uiState.isActionInProgress,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = Color(0xFFB00020),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Block User",
                            color = Color(0xFFB00020),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                } else {
                    Spacer(Modifier.height(60.dp))
                    CircularProgressIndicator(
                        color = DeepWine,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Loading friend details...",
                        fontSize = 14.sp,
                        color = MutedWineText,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
