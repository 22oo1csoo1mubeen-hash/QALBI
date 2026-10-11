package com.wakeus.app.ui.home

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.wakeus.app.R
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.ui.theme.QalbiTheme

// ─── Home Screen Palette Tokens ─────────────────────────────────────────────
private val HomeWineTitle       = Color(0xFF701831)
private val HomeWineAccent      = Color(0xFF80243C)
private val HomeWineSub         = Color(0xFF5C4046)
private val HomeCardBg          = Color(0xFFFFF7F4)
private val HomeCardBorder      = Color(0xFFF3C2C9)
private val HomeCardGradientTop = Color(0xFF982641)
private val HomeCardGradientBtm = Color(0xFF68182E)
private val HomeBadgeBg         = Color(0xFFFDECEF)
private val HomeNavBg           = Color(0xFFFFF8F5)
private val HomeNavActiveBg     = Color(0xFFFDE7EC)
private val HomeNavInactive     = Color(0xFF8C6E76)
private val HomeCanvasBase      = Color(0xFFFCF5EE)

/**
 * Redesigned Main / Home screen for QALBI.
 * Faithfully replicates `main-page-reference.png` using bundled `main_page_background.png`.
 */
@Composable
fun HomeScreen(
    onConnectFriend: () -> Unit = {},
    onFriendSelected: (String) -> Unit = {},
    onConnectedFriends: () -> Unit = {},
    onProfile: () -> Unit = {},
    onIncomingRequest: (String) -> Unit = {},
) {
    val authState by QalbiRepository.authState.collectAsState()
    val user = (authState as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: QalbiRepository.demoUser
    val firstName = user.displayName.trim().split(" ").firstOrNull()?.ifEmpty { "there" } ?: "there"
    val friends by QalbiRepository.friends.collectAsState(initial = emptyList())
    val incomingRequests by QalbiRepository.incomingWakeUpRequests.collectAsState(initial = emptyList())
    val isOnline by com.wakeus.app.data.network.NetworkMonitor.isOnline.collectAsState()

    // Configure system bars for readable dark icons over the light morning background
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

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            HomeBottomNavigationBar(
                onHome = {},
                onFriends = onConnectedFriends,
                onWakeUp = {
                    if (friends.isNotEmpty()) onFriendSelected(friends.first().id) else onConnectFriend()
                },
                onSettings = onProfile,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeCanvasBase)
                .padding(innerPadding),
        ) {
            // ── 1. Bundled Background Illustration ──────────────────────────
            Image(
                painter = painterResource(id = R.drawable.main_page_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )

            // ── 2. Foreground Content ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Top Header: Branding + Profile Avatar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Logo Emblem + Wordmark
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_heart_outline_simple),
                            contentDescription = null,
                            tint = HomeWineAccent,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "QALBI",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp,
                                color = HomeWineAccent,
                            ),
                        )
                    }

                    // Profile Action Button
                    IconButton(
                        onClick = onProfile,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HomeWineAccent),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = stringResource(R.string.profile),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(14.dp))

                    // Offline Warning Banner
                    if (!isOnline) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFBEBE6))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.WifiOff,
                                    contentDescription = "Offline",
                                    tint = HomeWineAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Offline. Requests and pairing will sync upon reconnection.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        color = HomeWineSub,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Welcome Section ─────────────────────────────────────
                    Text(
                        text = "Good morning,",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            color = HomeWineTitle,
                            letterSpacing = (-0.3).sp,
                        ),
                    )
                    Text(
                        text = "$firstName \uD83C\uDF05",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            color = HomeWineTitle,
                            letterSpacing = (-0.3).sp,
                        ),
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "A new day, a little closer\nto the people you love. \u2764",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = HomeWineSub,
                        ),
                    )

                    // ── Incoming Wake-Up Alert (if ringing or active requests) ──
                    val alarmState by com.wakeus.app.data.alarm.WakeUpAlarmManager.alarmState.collectAsState()
                    val activeReq = incomingRequests.firstOrNull() ?: if (alarmState.isRinging && alarmState.requestId != null) {
                        com.wakeus.app.data.model.WakeUpRequest(
                            requestId = alarmState.requestId ?: "",
                            senderUid = alarmState.senderUid ?: "",
                            senderDisplayName = alarmState.senderDisplayName ?: "Your friend",
                            senderUsername = alarmState.senderUsername ?: "friend",
                            receiverUid = user.id,
                            receiverDisplayName = user.displayName,
                            receiverUsername = user.username,
                            connectionId = "",
                        )
                    } else null

                    activeReq?.let { req ->
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFFDE7EC))
                                .border(1.2.dp, Color(0xFFE27B91), RoundedCornerShape(18.dp))
                                .clickable { onIncomingRequest(req.requestId) }
                                .padding(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (alarmState.isRinging) Icons.Filled.Alarm else Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = HomeWineAccent,
                                    modifier = Modifier.size(30.dp),
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${req.senderDisplayName} is waking you up! ☀️",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = HomeWineTitle,
                                        ),
                                    )
                                    Text(
                                        text = if (alarmState.isRinging) "🔔 ALARM RINGING — Tap to answer: I'm Awake!" else "Tap here to respond: I'm Awake!",
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            color = HomeWineAccent,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Primary Action Card: Connect to a Friend ────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = Color(0x30661226),
                                spotColor = Color(0x40661226),
                            )
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(HomeCardGradientTop, HomeCardGradientBtm),
                                ),
                                shape = RoundedCornerShape(22.dp),
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(onClick = onConnectFriend)
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rounded square icon badge
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x28FFFFFF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GroupAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            // Text details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connect to a Friend",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.5.sp,
                                        color = Color.White,
                                    ),
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Share a temporary code to pair\nwithout exchanging phone numbers.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        color = Color(0xEBFFFFFF),
                                    ),
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            // Arrow button circle
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(HomeBadgeBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_forward_slender),
                                    contentDescription = null,
                                    tint = HomeWineAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Section: Connected Friends ──────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Connected Friends",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = HomeWineTitle,
                            ),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onConnectedFriends),
                        ) {
                            Text(
                                text = "View all",
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = HomeWineAccent,
                                ),
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = HomeWineAccent,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Empty State or List of Friends
                    if (friends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(HomeCardBg)
                                .border(BorderStroke(1.2.dp, HomeCardBorder), RoundedCornerShape(20.dp))
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Overlapping hearts illustration
                                Image(
                                    painter = painterResource(id = R.drawable.ic_empty_friends_hearts),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(48.dp),
                                    contentScale = ContentScale.Fit,
                                )

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = "No connected friends yet",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = HomeWineTitle,
                                    ),
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Exchange a 10-minute pairing code with\na friend to establish an instant connection.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        color = HomeWineSub,
                                    ),
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(18.dp))

                                // Outlined Pill CTA
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(BorderStroke(1.2.dp, Color(0xFFF0BFC8)), CircleShape)
                                        .background(Color.Transparent)
                                        .clickable(onClick = onConnectFriend)
                                        .padding(horizontal = 22.dp, vertical = 11.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Connect to a Friend",
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.5.sp,
                                                color = HomeWineTitle,
                                            ),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Filled.GroupAdd,
                                            contentDescription = null,
                                            tint = HomeWineTitle,
                                            modifier = Modifier.size(17.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Display connected friends with romantic styling
                        friends.take(3).forEach { friend ->
                            HomeFriendCard(
                                friend = friend,
                                onClick = { onFriendSelected(friend.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Card: How connection codes work ─────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(HomeCardBg)
                            .border(BorderStroke(1.2.dp, HomeCardBorder), RoundedCornerShape(20.dp))
                            .clickable(onClick = onConnectFriend)
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Shield icon badge
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(HomeBadgeBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock_closed),
                                    contentDescription = null,
                                    tint = HomeWineAccent,
                                    modifier = Modifier.size(22.dp),
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "How connection codes work",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                        color = HomeWineTitle,
                                    ),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Generate an 8-character code and share it privately. When your friend enters the code within 10 minutes, both accounts connect automatically. Codes are single-use, private, and secure.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        color = HomeWineSub,
                                    ),
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = null,
                                tint = Color(0xFF9E7D84),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Romantic-themed connected friend row item.
 */
@Composable
private fun HomeFriendCard(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HomeCardBg)
            .border(BorderStroke(1.2.dp, HomeCardBorder), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDE7EC)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.initials,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = HomeWineAccent,
                    ),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.displayName,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = HomeWineTitle,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@${friend.username}",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = HomeWineSub,
                    ),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                tint = HomeWineAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Floating romantic bottom navigation bar matching `main-page-reference.png`.
 */
@Composable
private fun HomeBottomNavigationBar(
    onHome: () -> Unit,
    onFriends: () -> Unit,
    onWakeUp: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color(0x30661226),
                    spotColor = Color(0x40661226),
                )
                .clip(RoundedCornerShape(32.dp))
                .background(HomeNavBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Home (Active)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onHome),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(HomeNavActiveBg)
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = HomeWineAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Home",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeWineAccent,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(HomeWineAccent),
                    )
                }

                // 2. Friends
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(onClick = onFriends)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Friends",
                        tint = HomeNavInactive,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Friends",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeNavInactive,
                        ),
                    )
                }

                // 3. Wake Up
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(onClick = onWakeUp)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = "Wake Up",
                        tint = HomeNavInactive,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Wake Up",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeNavInactive,
                        ),
                    )
                }

                // 4. Settings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(onClick = onSettings)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = HomeNavInactive,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Settings",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeNavInactive,
                        ),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    QalbiTheme {
        HomeScreen()
    }
}
