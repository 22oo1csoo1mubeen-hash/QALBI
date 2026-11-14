package com.wakeus.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.wakeus.app.data.auth.AuthState
import com.wakeus.app.data.fcm.NotificationHelper
import com.wakeus.app.data.repository.QalbiRepository
import com.wakeus.app.navigation.AppNavigation
import com.wakeus.app.ui.theme.QalbiTheme
import kotlinx.coroutines.launch

/**
 * Single Activity entry point for QALBI.
 * Installs the core-splashscreen API, requests notification permissions,
 * registers FCM tokens on authentication, and handles wake-up deep links.
 */
class MainActivity : ComponentActivity() {

    private var pendingRequestId by mutableStateOf<String?>(null)
    private var pendingScheduledRequestId by mutableStateOf<String?>(null)
    private var pendingScheduledFriendId by mutableStateOf<String?>(null)
    private var pendingScheduledNavigationToken by mutableLongStateOf(0L)

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Notification permission state updated
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        configureLockScreenPresentation()
        requestNotificationPermission()
        pendingRequestId = extractRequestId(intent)
        pendingScheduledRequestId = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_REQUEST_ID)
        pendingScheduledFriendId = intent.getStringExtra(NotificationHelper.EXTRA_FRIEND_ID)
        if (!pendingScheduledRequestId.isNullOrBlank()) pendingScheduledNavigationToken = System.currentTimeMillis()

        // Sync the token when the authenticated session becomes available. Failed uploads
        // are retried by WorkManager and do not hold a permanent listener service open.
        lifecycleScope.launch {
            QalbiRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    QalbiRepository.fcmTokenRepository.registerAndScheduleRetry(
                        this@MainActivity,
                        state.user.id,
                    )
                }
            }
        }

        setContent {
            QalbiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        initialRequestId = pendingRequestId,
                        initialScheduledRequestId = pendingScheduledRequestId,
                        initialScheduledFriendId = pendingScheduledFriendId,
                        initialScheduledNavigationToken = pendingScheduledNavigationToken,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockScreenPresentation()
        val newRequestId = extractRequestId(intent)
        if (!newRequestId.isNullOrBlank()) {
            pendingRequestId = newRequestId
        }
        intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_REQUEST_ID)?.let {
            pendingScheduledRequestId = it
        }
        intent.getStringExtra(NotificationHelper.EXTRA_FRIEND_ID)?.let {
            pendingScheduledFriendId = it
        }
        if (!intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_REQUEST_ID).isNullOrBlank()) {
            pendingScheduledNavigationToken = System.currentTimeMillis()
        }
    }

    private fun configureLockScreenPresentation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun extractRequestId(intent: Intent?): String? {
        if (intent == null) return null
        return intent.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID)
            ?: intent.getStringExtra("requestId")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
