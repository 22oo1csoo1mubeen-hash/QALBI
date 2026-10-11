package com.wakeus.app.data.fcm

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.wakeus.app.data.model.FcmDeviceToken
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class FcmTokenRepository(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (_: Exception) {
            null
        }
    }
) {

    private fun hashToken(token: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }

    /**
     * Registers or refreshes the current device's FCM token in Firestore under /users/{uid}/fcmTokens/{tokenHash}.
     */
    suspend fun registerCurrentDeviceToken(uid: String): Result<String> {
        val firestore = firestoreProvider() ?: return Result.failure(Exception("Firebase not configured"))
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            val tokenHash = hashToken(token)
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("fcmTokens")
                .document(tokenHash)

            val deviceToken = FcmDeviceToken(
                token = token,
                deviceModel = android.os.Build.MODEL ?: "Android Device",
            )

            docRef.set(deviceToken.toMap()).await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unregisters the current device token on user logout.
     */
    suspend fun unregisterDeviceToken(uid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(Exception("Firebase not configured"))
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            val tokenHash = hashToken(token)
            firestore.collection("users")
                .document(uid)
                .collection("fcmTokens")
                .document(tokenHash)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the registered device tokens for a recipient user.
     */
    suspend fun getReceiverTokens(receiverUid: String): Result<List<String>> {
        val firestore = firestoreProvider() ?: return Result.failure(Exception("Firebase not configured"))
        return try {
            val snapshot = firestore.collection("users")
                .document(receiverUid)
                .collection("fcmTokens")
                .get()
                .await()

            val tokens = snapshot.documents.mapNotNull { it.getString("token") }
            Result.success(tokens)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
