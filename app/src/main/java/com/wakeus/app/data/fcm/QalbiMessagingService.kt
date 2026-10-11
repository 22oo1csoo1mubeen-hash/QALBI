package com.wakeus.app.data.fcm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service listening for incoming Firebase Cloud Messaging events.
 */
class QalbiMessagingService : FirebaseMessagingService() {

    private val fcmTokenRepository = FcmTokenRepository()
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }

        if (currentUser != null) {
            serviceScope.launch {
                fcmTokenRepository.registerCurrentDeviceToken(currentUser.uid)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extract payload from data map
        val data = remoteMessage.data
        val requestId = data["requestId"] ?: remoteMessage.notification?.tag
        val senderDisplayName = data["senderDisplayName"] ?: remoteMessage.notification?.title ?: "Your friend"
        val senderUsername = data["senderUsername"] ?: "friend"
        val receiverUid = data["receiverUid"]

        // 2. Validate recipient matching if authenticated
        val currentUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }

        if (receiverUid != null && currentUid != null && receiverUid != currentUid) {
            // Message targeted at a different account on this device; ignore
            return
        }

        val senderUid = data["senderUid"] ?: ""
        val expiresAt = data["expiresAt"]?.toLongOrNull() ?: (System.currentTimeMillis() + 15 * 60 * 1000L)

        if (!requestId.isNullOrBlank()) {
            com.wakeus.app.data.alarm.WakeUpAlarmManager.startAlarm(
                context = applicationContext,
                requestId = requestId,
                senderUid = senderUid,
                senderDisplayName = senderDisplayName,
                senderUsername = senderUsername,
                expiresAt = expiresAt
            )
        }
    }
}
