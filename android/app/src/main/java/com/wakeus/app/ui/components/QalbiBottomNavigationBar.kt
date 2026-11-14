package com.wakeus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class QalbiNavTab {
    HOME, FRIENDS, WAKE_UP, SETTINGS
}

private val BottomBarNavBg       = Color(0xFFFFF9F8)
private val BottomBarActiveBg    = Color(0xFFFDE7EC)
private val BottomBarWineAccent  = Color(0xFF80243C)
private val BottomBarNavInactive = Color(0xFF8C6E76)

/**
 * Shared romantic floating bottom navigation bar for QALBI.
 * Stays visible across primary app destinations (Home, Friends, Settings, etc.)
 * with active indicators reflecting the current destination.
 */
@Composable
fun QalbiBottomNavigationBar(
    selectedTab: QalbiNavTab,
    onHome: () -> Unit,
    onFriends: () -> Unit,
    onWakeUp: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
                .background(BottomBarNavBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Home
                QalbiBottomNavItem(
                    label = "Home",
                    isActive = selectedTab == QalbiNavTab.HOME,
                    activeIcon = Icons.Filled.Home,
                    inactiveIcon = Icons.Outlined.Home,
                    onClick = onHome,
                )

                // 2. Friends
                QalbiBottomNavItem(
                    label = "Friends",
                    isActive = selectedTab == QalbiNavTab.FRIENDS,
                    activeIcon = Icons.Filled.Favorite,
                    inactiveIcon = Icons.Outlined.FavoriteBorder,
                    onClick = onFriends,
                )

                // 3. Wake Up
                QalbiBottomNavItem(
                    label = "Wake Up",
                    isActive = selectedTab == QalbiNavTab.WAKE_UP,
                    activeIcon = Icons.Filled.Alarm,
                    inactiveIcon = Icons.Outlined.Alarm,
                    onClick = onWakeUp,
                )

                // 4. Settings
                QalbiBottomNavItem(
                    label = "Settings",
                    isActive = selectedTab == QalbiNavTab.SETTINGS,
                    activeIcon = Icons.Filled.Settings,
                    inactiveIcon = Icons.Outlined.Settings,
                    onClick = onSettings,
                )
            }
        }
    }
}

@Composable
private fun QalbiBottomNavItem(
    label: String,
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BottomBarActiveBg)
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = activeIcon,
                    contentDescription = label,
                    tint = BottomBarWineAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BottomBarWineAccent,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(BottomBarWineAccent),
            )
        } else {
            Icon(
                imageVector = inactiveIcon,
                contentDescription = label,
                tint = BottomBarNavInactive,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = BottomBarNavInactive,
                ),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}
