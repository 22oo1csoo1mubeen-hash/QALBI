package com.wakeus.app.navigation

/**
 * Sealed class representing every navigation destination in the app.
 * Adding a new screen requires only a new object here and a composable
 * in AppNavigation.kt — no string constants scattered around the codebase.
 */
sealed class Screen(val route: String) {
    object Splash           : Screen("splash")
    object Welcome          : Screen("welcome")
    object Register         : Screen("register")
    object Login            : Screen("login")
    object Home             : Screen("home")
    object ConnectFriend    : Screen("connect_friend")
    object ConnectedFriends : Screen("connected_friends")
    object FriendDetail     : Screen("friend_detail/{friendId}") {
        fun withId(friendId: String) = "friend_detail/$friendId"
    }
    object WakeUp           : Screen("wake_up/{friendId}") {
        fun withId(friendId: String) = "wake_up/$friendId"
    }
    object Profile          : Screen("profile")
    object Settings         : Screen("settings")
}
