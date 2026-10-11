package com.wakeus.app.data.user

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wakeus.app.data.model.User
import kotlinx.coroutines.tasks.await

class UsernameAlreadyTakenException(username: String) :
    Exception("The username '@$username' is already taken. Please choose a different username.")

class FirebaseNotConfiguredException :
    Exception("Firebase configuration is missing. Please add google-services.json to the app/ directory.")

/**
 * Repository responsible for user profile management and atomic username claims in Cloud Firestore.
 */
class UserRepository(
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

    fun isConfigured(): Boolean = firestoreProvider() != null

    /**
     * Retrieves the profile document for the specified [uid].
     */
    suspend fun getUserProfile(uid: String): Result<User?> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists() && doc.data != null) {
                Result.success(User.fromMap(uid, doc.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a normalized username is available.
     */
    suspend fun isUsernameAvailable(usernameNormalized: String): Result<Boolean> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val doc = firestore.collection("usernames").document(usernameNormalized).get().await()
            Result.success(!doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically claims the username and creates the user profile in Firestore.
     * Uses a transaction so that if two users attempt to claim the exact same username
     * simultaneously, only one succeeds and the other receives [UsernameAlreadyTakenException].
     */
    suspend fun claimUsernameAndCreateProfile(user: User): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            firestore.runTransaction { transaction ->
                val usernameDocRef = firestore.collection("usernames").document(user.usernameNormalized)
                val usernameSnapshot = transaction.get(usernameDocRef)

                if (usernameSnapshot.exists()) {
                    val existingUid = usernameSnapshot.getString("uid")
                    if (existingUid != user.id) {
                        throw UsernameAlreadyTakenException(user.username)
                    }
                }

                // 1. Claim username registry entry
                transaction.set(
                    usernameDocRef,
                    mapOf(
                        "uid" to user.id,
                        "claimedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Create user profile document
                val userDocRef = firestore.collection("users").document(user.id)
                transaction.set(userDocRef, user.toMap())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans up username and profile in case of partial rollback.
     */
    suspend fun deleteProfileAndReleaseUsername(uid: String, usernameNormalized: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            firestore.runTransaction { transaction ->
                val usernameDocRef = firestore.collection("usernames").document(usernameNormalized)
                val userDocRef = firestore.collection("users").document(uid)
                transaction.delete(usernameDocRef)
                transaction.delete(userDocRef)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
