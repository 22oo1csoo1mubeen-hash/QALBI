package com.wakeus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.wakeus.app.navigation.AppNavigation
import com.wakeus.app.ui.theme.QalbiTheme

/**
 * Single Activity entry point for QALBI.
 * Installs the core-splashscreen API so the system window splash matches
 * the app's design before the Compose tree is ready.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate so the system
        // window splash is shown correctly on Android 12+.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout so content can draw behind system bars.
        enableEdgeToEdge()

        setContent {
            QalbiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}
