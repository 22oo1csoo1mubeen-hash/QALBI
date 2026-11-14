package com.wakeus.app.navigation

import android.animation.ValueAnimator
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.ui.auth.ForgotPasswordScreen
import com.wakeus.app.ui.auth.LoginScreen
import com.wakeus.app.ui.auth.RegisterScreen
import com.wakeus.app.ui.components.QalbiBottomNavigationBar
import com.wakeus.app.ui.components.QalbiNavTab
import com.wakeus.app.ui.components.LocalFallingPetalClock
import com.wakeus.app.ui.components.FallingPetalsOverlay
import com.wakeus.app.ui.friends.ConnectFriendScreen
import com.wakeus.app.ui.friends.ConnectedFriendsScreen
import com.wakeus.app.ui.friends.FriendDetailScreen
import com.wakeus.app.ui.home.HomeScreen
import com.wakeus.app.ui.profile.ProfileScreen
import com.wakeus.app.ui.secret.SecretRomanticScreen
import com.wakeus.app.ui.secret.BabyLoveLoadingScreen
import com.wakeus.app.ui.secret.BabyLoveScreen
import com.wakeus.app.ui.settings.SettingsScreen
import com.wakeus.app.ui.splash.SplashScreen
import com.wakeus.app.ui.wakeup.IncomingRequestScreen
import com.wakeus.app.ui.wakeup.WakeUpEmptyTabScreen
import com.wakeus.app.ui.wakeup.WakeUpScreen
import com.wakeus.app.ui.wakeup.ScheduleAlarmScreen
import com.wakeus.app.ui.wakeup.ScheduledAlarmsScreen
import com.wakeus.app.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle

/**
 * Central navigation graph for the QALBI app.
 * Provides a persistent shared bottom navigation scaffold across primary destinations
 * (Home, Friends, Settings) and handles all screen routing.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    initialRequestId: String? = null,
    initialScheduledRequestId: String? = null,
    initialScheduledFriendId: String? = null,
    initialScheduledNavigationToken: Long = 0L,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navigationScope = rememberCoroutineScope()

    LaunchedEffect(initialRequestId) {
        if (!initialRequestId.isNullOrBlank()) {
            navController.navigate(Screen.IncomingRequest.withId(initialRequestId)) {
                launchSingleTop = true
            }
        }
    }

    // Global real-time observer: immediately ring alarm and navigate to incoming screen on any wake-up call
    val incomingRequests by QalbiRepository.incomingWakeUpRequests.collectAsState(initial = emptyList())
    val activeIncoming = incomingRequests.firstOrNull { it.status == com.wakeus.app.data.model.WakeUpRequestStatus.PENDING && !it.isExpired }

    LaunchedEffect(activeIncoming?.requestId) {
        activeIncoming?.let { req ->
            com.wakeus.app.data.alarm.WakeUpAlarmManager.startAlarm(
                context = context,
                requestId = req.requestId,
                senderUid = req.senderUid,
                senderDisplayName = req.senderDisplayName,
                senderUsername = req.senderUsername,
                expiresAt = req.expiresAt,
                customMessage = req.customMessage,
            )
            navController.navigate(Screen.IncomingRequest.withId(req.requestId)) {
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val authState by QalbiRepository.authState.collectAsState()
    val senderUid = (authState as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user?.id.orEmpty()
    val scheduledOutgoingFlow = remember(senderUid) {
        if (senderUid.isBlank()) flowOf(emptyList())
        else QalbiRepository.wakeUpRepository.observeScheduledOutgoingRequests(senderUid)
    }
    val scheduledOutgoingRequests by scheduledOutgoingFlow.collectAsState(initial = emptyList())
    val activeScheduledOutgoing = scheduledOutgoingRequests.firstOrNull {
        it.status in setOf(
            com.wakeus.app.data.model.WakeUpRequestStatus.PENDING,
            com.wakeus.app.data.model.WakeUpRequestStatus.ACKNOWLEDGED,
            com.wakeus.app.data.model.WakeUpRequestStatus.DECLINED,
            com.wakeus.app.data.model.WakeUpRequestStatus.EXPIRED,
        ) && it.createdAt >= System.currentTimeMillis() - 16 * 60 * 1000L &&
            it.fcmDispatched && it.receiverUid.isNotBlank()
    }
    val displayedRequestId = navBackStackEntry?.arguments?.getString("requestId")
    var handledScheduledNavigationToken by remember { mutableLongStateOf(0L) }

    LaunchedEffect(initialScheduledRequestId, initialScheduledFriendId, initialScheduledNavigationToken, currentRoute, authState) {
        if (handledScheduledNavigationToken != initialScheduledNavigationToken &&
            !initialScheduledRequestId.isNullOrBlank() && !initialScheduledFriendId.isNullOrBlank() &&
            authState is com.wakeus.app.data.auth.AuthState.Authenticated &&
            currentRoute != Screen.Splash.route
        ) {
            handledScheduledNavigationToken = initialScheduledNavigationToken
            navController.navigate(Screen.WakeUp.withRequest(initialScheduledFriendId, initialScheduledRequestId)) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(activeScheduledOutgoing?.requestId) {
        val request = activeScheduledOutgoing ?: return@LaunchedEffect
        if (displayedRequestId != request.requestId) {
            navController.navigate(Screen.WakeUp.withRequest(request.receiverUid, request.requestId)) {
                launchSingleTop = true
            }
        }
    }

    val petalFrameNanos = remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var animationsEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                animationsEnabled = ValueAnimator.areAnimatorsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isPetalScreen = currentRoute in setOf(
        Screen.Home.route,
        Screen.BabyLove.route,
        Screen.ConnectFriend.route,
        Screen.ConnectedFriends.route,
        Screen.FriendDetail.route,
        Screen.WakeUp.route,
        Screen.WakeUpTab.route,
        Screen.ScheduleAlarm.route,
        Screen.ScheduledAlarms.route,
        Screen.IncomingRequest.route,
        Screen.Profile.route,
        Screen.Settings.route,
    )
    LaunchedEffect(isPetalScreen, animationsEnabled, lifecycleOwner) {
        if (!isPetalScreen || !animationsEnabled) {
            petalFrameNanos.longValue = 0L
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val elapsedBeforePause = petalFrameNanos.longValue
            val segmentStart = withFrameNanos { it }
            while (true) {
                val frame = withFrameNanos { it }
                petalFrameNanos.longValue = elapsedBeforePause + frame - segmentStart
            }
        }
    }

    val friendsList by QalbiRepository.friends.collectAsState(initial = emptyList())

    val (isBottomBarVisible, selectedTab) = when {
        currentRoute == Screen.Home.route -> true to QalbiNavTab.HOME
        currentRoute == Screen.ConnectedFriends.route -> true to QalbiNavTab.FRIENDS
        currentRoute == Screen.WakeUpTab.route || currentRoute == Screen.WakeUp.route || currentRoute?.startsWith("wake_up") == true || currentRoute?.startsWith("schedule_alarm") == true || currentRoute == Screen.ScheduledAlarms.route -> true to QalbiNavTab.WAKE_UP
        currentRoute == Screen.Settings.route -> true to QalbiNavTab.SETTINGS
        else -> false to QalbiNavTab.HOME
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (isBottomBarVisible) {
                QalbiBottomNavigationBar(
                    selectedTab = selectedTab,
                    onHome = {
                        if (currentRoute != Screen.Home.route) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onFriends = {
                        if (currentRoute != Screen.ConnectedFriends.route) {
                            navController.navigate(Screen.ConnectedFriends.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onWakeUp = {
                        val isWakeUpActive = currentRoute == Screen.WakeUpTab.route || currentRoute == Screen.WakeUp.route || currentRoute?.startsWith("wake_up") == true
                        if (!isWakeUpActive) {
                            if (friendsList.isNotEmpty()) {
                                navController.navigate(Screen.WakeUp.withId(friendsList.first().id)) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(Screen.WakeUpTab.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                    onSettings = {
                        if (currentRoute != Screen.Settings.route) {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            CompositionLocalProvider(LocalFallingPetalClock provides petalFrameNanos) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                ) {

                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashComplete = {
                            val currentAuthState = QalbiRepository.authState.value
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
                        onRegistered = {
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
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        },
                        onForgotPassword = {
                            navController.navigate(Screen.ForgotPassword.route)
                        },
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToLogin = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onConnectFriend    = { navController.navigate(Screen.ConnectFriend.route) },
                        onFriendSelected   = { id -> navController.navigate(Screen.FriendDetail.withId(id)) },
                        onConnectedFriends = { navController.navigate(Screen.ConnectedFriends.route) },
                        onProfile          = { navController.navigate(Screen.Profile.route) },
                        onIncomingRequest  = { id -> navController.navigate(Screen.IncomingRequest.withId(id)) },
                        onOpenSecretRomantic = {
                            navController.navigate(Screen.SecretRomantic.route)
                        },
                        onOpenBabyLove = {
                            navController.navigate(Screen.BabyLoveLoading.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(Screen.ConnectFriend.route) {
                    ConnectFriendScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Screen.ConnectedFriends.route) {
                    ConnectedFriendsScreen(
                        onBack           = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        },
                        onFriendSelected = { id -> navController.navigate(Screen.FriendDetail.withId(id)) },
                        onConnectFriend  = { navController.navigate(Screen.ConnectFriend.route) },
                    )
                }

                composable(
                    route = Screen.FriendDetail.route,
                    arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                    FriendDetailScreen(
                        friendId = friendId,
                        onBack   = { navController.popBackStack() },
                        onWakeUp = { id -> navController.navigate(Screen.WakeUp.withId(id)) },
                        onScheduleAlarm = { id -> navController.navigate(Screen.ScheduleAlarm.withId(id)) },
                    )
                }

                composable(Screen.WakeUpTab.route) {
                    val friends by QalbiRepository.friends.collectAsState(initial = emptyList())
                    if (friends.isNotEmpty()) {
                        WakeUpScreen(
                            friendId = friends.first().id,
                            onManageSchedules = { navController.navigate(Screen.ScheduledAlarms.route) },
                            onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            },
                        )
                    } else {
                        WakeUpEmptyTabScreen(
                            onConnectFriend = { navController.navigate(Screen.ConnectFriend.route) },
                        )
                    }
                }

                composable(
                    route = Screen.WakeUp.route,
                    arguments = listOf(
                        navArgument("friendId") { type = NavType.StringType },
                        navArgument("requestId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                    val requestId = backStackEntry.arguments?.getString("requestId")
                    WakeUpScreen(
                        friendId = friendId,
                        requestId = requestId,
                        onManageSchedules = { navController.navigate(Screen.ScheduledAlarms.route) },
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        },
                    )
                }

                composable(
                    route = Screen.ScheduleAlarm.route,
                    arguments = listOf(navArgument("friendId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                    ScheduleAlarmScreen(friendId = friendId, onBack = { navController.popBackStack() })
                }

                composable(Screen.ScheduledAlarms.route) {
                    ScheduledAlarmsScreen(onBack = { navController.popBackStack() })
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
                        onSettings = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Profile.route) { inclusive = false }
                            }
                        },
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
                        onBack   = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        },
                        onLogout = {
                            navigationScope.launch {
                                QalbiRepository.logout()
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                    )
                }

                composable(Screen.SecretRomantic.route) {
                    SecretRomanticScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = Screen.BabyLoveLoading.route,
                    enterTransition = { fadeIn(tween(400)) },
                    exitTransition = { fadeOut(tween(400)) },
                    popEnterTransition = { fadeIn(tween(300)) },
                    popExitTransition = { fadeOut(tween(300)) },
                ) {
                    BabyLoveLoadingScreen(
                        onComplete = {
                            navController.navigate(Screen.BabyLove.route) {
                                popUpTo(Screen.BabyLoveLoading.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(
                    route = Screen.BabyLove.route,
                    enterTransition = { fadeIn(tween(850)) },
                    exitTransition = { fadeOut(tween(450)) },
                    popEnterTransition = { fadeIn(tween(450)) },
                    popExitTransition = { fadeOut(tween(450)) },
                ) {
                    BabyLoveScreen(
                        onBack = {
                            if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                                navController.navigate(Screen.Home.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                    )
                }
            }
            if (isPetalScreen && currentRoute != Screen.BabyLove.route) {
                FallingPetalsOverlay()
            }
            }
        }
    }
}
