package com.wakeus.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Robust, zero-cost network connectivity monitor.
 * Employs ConnectivityManager.NetworkCallback to observe real-time online/offline transitions.
 * Ensures graceful UI state degradation during network loss.
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var isInitialized = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Initializes the network callback with the application context.
     * Safe to invoke multiple times (subsequent calls are idempotent).
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val appContext = context.applicationContext
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager == null) {
                Log.w(TAG, "ConnectivityManager is unavailable on this device. Defaulting to online.")
                _isOnline.value = true
                isInitialized = true
                return
            }

            // Inspect current active network state immediately
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet || (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

            // Register continuous network callback
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Network connection established.")
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "Network connection lost.")
                    _isOnline.value = false
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasCap = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isOnline.value = hasCap
                }
            }

            connectivityManager.registerNetworkCallback(request, callback)
            networkCallback = callback
            isInitialized = true
            Log.d(TAG, "NetworkMonitor successfully initialized. Current online state: ${_isOnline.value}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize NetworkMonitor. Defaulting to online.", e)
            _isOnline.value = true
            isInitialized = true
        }
    }

    /**
     * Helper for automated unit testing to simulate connectivity changes.
     */
    fun setOnlineForTesting(online: Boolean) {
        _isOnline.value = online
    }
}
