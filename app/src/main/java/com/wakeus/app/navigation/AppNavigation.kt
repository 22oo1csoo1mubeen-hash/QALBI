package com.wakeus.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wakeus.app.ui.auth.LoginScreen
import com.wakeus.app.ui.auth.RegisterScreen
import com.wakeus.app.ui.friends.ConnectFriendScreen
import com.wakeus.app.ui.friends.ConnectedFriendsScreen
import com.wakeus.app.ui.home.HomeScreen
import com.wakeus.app.ui.profile.ProfileScreen
import com.wakeus.app.ui.settings.SettingsScreen
import com.wakeus.app.ui.splash.SplashScreen
import com.wakeus.app.ui.wakeup.IncomingRequestScreen
import com.wakeus.app.ui.wakeup.WakeUpScreen
import com.wakeus.app.ui.welcome.WelcomeScreen

/**
 * Central navigation graph for the QALBI app.
 * All screen composables are wired here. The graph starts at Splash and
 * manages the complete navigation flow.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    initialRequestId: String? = null,
) {
    LaunchedEffect(initialRequestId) {
        if (!initialRequestId.isNullOrBlank()) {
            navController.navigate(Screen.IncomingRequest.withId(initialRequestId))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val currentAuthState = com.wakeus.app.data.repository.QalbiRepository.authState.value
                    if (currentAuthState is com.wakeus.app.data.auth.AuthState.Authenticated) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Screen.Register.route) },
                onLogIn      = { navController.navigate(Screen.Login.route) },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered  = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onConnectFriend    = { navController.navigate(Screen.ConnectFriend.route) },
                onFriendSelected   = { id -> navController.navigate(Screen.FriendDetail.withId(id)) },
                onConnectedFriends = { navController.navigate(Screen.ConnectedFriends.route) },
                onProfile          = { navController.navigate(Screen.Profile.route) },
                onIncomingRequest  = { id -> navController.navigate(Screen.IncomingRequest.withId(id)) },
            )
        }

        composable(Screen.ConnectFriend.route) {
            ConnectFriendScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ConnectedFriends.route) {
            ConnectedFriendsScreen(
                onBack           = { navController.popBackStack() },
                onFriendSelected = { id -> navController.navigate(Screen.FriendDetail.withId(id)) },
                onConnectFriend  = { navController.navigate(Screen.ConnectFriend.route) },
            )
        }

        composable(
            route = Screen.FriendDetail.route,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            com.wakeus.app.ui.friends.FriendDetailScreen(
                friendId = friendId,
                onBack   = { navController.popBackStack() },
                onWakeUp = { id -> navController.navigate(Screen.WakeUp.withId(id)) },
            )
        }

        composable(
            route = Screen.WakeUp.route,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            WakeUpScreen(
                friendId = friendId,
                onBack   = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.IncomingRequest.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            IncomingRequestScreen(
                requestId = requestId,
                onBack    = { navController.popBackStack() },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSettings = { navController.navigate(Screen.Settings.route) },
                onLogout   = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack   = { navController.popBackStack() },
                onLogout = {
                    com.wakeus.app.data.repository.QalbiRepository.logout()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
