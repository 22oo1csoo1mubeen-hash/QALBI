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
                val result = fcmTokenRepository.registerToken(currentUser.uid, token)
                if (result.isSuccess) {
                    AlarmDiagnostics.record(applicationContext, AlarmDiagnosticEvent.FCM_TOKEN_REGISTERED)
                } else {
                    AlarmDiagnostics.record(applicationContext, AlarmDiagnosticEvent.FCM_TOKEN_REGISTRATION_FAILED)
                    FcmTokenRegistrationWorker.enqueue(applicationContext, currentUser.uid)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Extract payload from data map
        val data = remoteMessage.data
        val requestId = data["requestId"]
        AlarmDiagnostics.record(applicationContext, AlarmDiagnosticEvent.FCM_MESSAGE_RECEIVED, requestId)

        // 2. Validate recipient matching if authenticated
        val currentUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }

        val result = IncomingAlarmPayloadValidator.validate(data, currentUid, System.currentTimeMillis())
        val payload = (result as? IncomingAlarmPayloadResult.Valid)?.payload
        if (payload == null) {
            AlarmDiagnostics.record(applicationContext, AlarmDiagnosticEvent.FCM_MESSAGE_REJECTED, requestId)
            return
        }

        com.wakeus.app.data.alarm.WakeUpAlarmManager.startAlarm(
            context = applicationContext,
            requestId = payload.requestId,
            senderUid = payload.senderUid,
            senderDisplayName = payload.senderDisplayName,
            senderUsername = payload.senderUsername,
            expiresAt = payload.expiresAt
        )
    }
}
