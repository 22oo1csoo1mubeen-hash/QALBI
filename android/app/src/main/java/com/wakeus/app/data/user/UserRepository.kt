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
            FirebaseFirestore.getInstance()
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

                // 1. Claim username registry entry with email for seamless login resolution
                transaction.set(
                    usernameDocRef,
                    mapOf(
                        "uid" to user.id,
                        "email" to (user.email ?: "${user.usernameNormalized}@users.qalbi.app"),
                        "claimedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Create user profile document
                val userDocRef = firestore.collection("users").document(user.id)
                transaction.set(userDocRef, user.toMap())

                // 3. Create minimal public profile for friend pairing
                val publicProfile = com.wakeus.app.data.model.PublicProfile(
                    uid = user.id,
                    displayName = user.displayName,
                    username = user.username,
                    usernameNormalized = user.usernameNormalized,
                )
                val publicDocRef = firestore.collection("publicProfiles").document(user.id)
                transaction.set(publicDocRef, publicProfile.toMap())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves the minimal public profile for [uid] without accessing sensitive fields.
     */
    suspend fun getPublicProfile(uid: String): Result<com.wakeus.app.data.model.PublicProfile?> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val doc = firestore.collection("publicProfiles").document(uid).get().await()
            if (doc.exists() && doc.data != null) {
                Result.success(com.wakeus.app.data.model.PublicProfile.fromMap(uid, doc.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ensures that the public profile exists for an existing authenticated user.
     */
    suspend fun ensurePublicProfile(user: User): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val publicProfile = com.wakeus.app.data.model.PublicProfile(
                uid = user.id,
                displayName = user.displayName,
                username = user.username,
                usernameNormalized = user.usernameNormalized,
            )
            firestore.collection("publicProfiles")
                .document(user.id)
                .set(publicProfile.toMap())
                .await()
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
                val publicDocRef = firestore.collection("publicProfiles").document(uid)
                transaction.delete(usernameDocRef)
                transaction.delete(userDocRef)
                transaction.delete(publicDocRef)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the user's avatar style in both private profile and public profile.
     */
    suspend fun updateAvatarStyle(uid: String, avatarStyle: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val userDocRef = firestore.collection("users").document(uid)
            val publicDocRef = firestore.collection("publicProfiles").document(uid)
            val updates = mapOf(
                "avatarStyle" to avatarStyle,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.runBatch { batch ->
                batch.update(userDocRef, updates)
                batch.update(publicDocRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves the authentication email associated with a given username.
     * Looks in the `usernames` registry first. If missing (e.g. for users registered before
     * this feature was introduced), it reads `users/{uid}`, extracts their registered email,
     * and backfills the `usernames` registry so subsequent lookups are instant.
     */
    suspend fun getEmailForUsername(usernameNormalized: String): Result<String?> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val clean = usernameNormalized.trim().lowercase().removePrefix("@")
        return try {
            // 1. Check usernames registry document
            val usernameDoc = firestore.collection("usernames").document(clean).get().await()
            if (usernameDoc.exists() && usernameDoc.data != null) {
                val emailInDoc = usernameDoc.getString("email")
                if (!emailInDoc.isNullOrBlank()) {
                    return Result.success(emailInDoc)
                }

                // If doc was created before email was recorded, read from users/{uid}
                val uid = usernameDoc.getString("uid")
                if (!uid.isNullOrBlank()) {
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    if (userDoc.exists() && userDoc.data != null) {
                        val userEmail = userDoc.getString("email")
                        if (!userEmail.isNullOrBlank()) {
                            // Backfill the email in usernames registry
                            try {
                                firestore.collection("usernames").document(clean).update("email", userEmail).await()
                            } catch (_: Exception) {}
                            return Result.success(userEmail)
                        }
                    }
                }
            }

            // 2. Fallback: Query users collection where usernameNormalized matches
            try {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("usernameNormalized", clean)
                    .limit(1)
                    .get()
                    .await()
                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents.first()
                    val userEmail = userDoc.getString("email")
                    val uid = userDoc.id
                    if (!userEmail.isNullOrBlank()) {
                        try {
                            firestore.collection("usernames").document(clean).set(
                                mapOf(
                                    "uid" to uid,
                                    "email" to userEmail,
                                    "claimedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        } catch (_: Exception) {}
                        return Result.success(userEmail)
                    }
                }
            } catch (_: Exception) {}

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves an account identifier (email or username) into a Pair of (uid, registeredEmail).
     */
    suspend fun findUserAccountByIdentifier(identifier: String): Result<Pair<String, String?>?> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val clean = identifier.trim().lowercase().removePrefix("@")
        val isEmail = clean.contains("@") && clean.contains(".")

        return try {
            if (isEmail) {
                val query = firestore.collection("users")
                    .whereEqualTo("email", clean)
                    .limit(1)
                    .get()
                    .await()
                if (!query.isEmpty) {
                    val doc = query.documents.first()
                    Result.success(Pair(doc.id, clean))
                } else {
                    Result.success(null)
                }
            } else {
                val usernameDoc = firestore.collection("usernames").document(clean).get().await()
                if (usernameDoc.exists()) {
                    val uid = usernameDoc.getString("uid")
                    var email = usernameDoc.getString("email")
                    if (email.isNullOrBlank() && !uid.isNullOrBlank()) {
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        email = userDoc.getString("email")
                    }
                    if (!uid.isNullOrBlank()) {
                        Result.success(Pair(uid, email))
                    } else {
                        Result.success(null)
                    }
                } else {
                    // Try query users by usernameNormalized
                    val query = firestore.collection("users")
                        .whereEqualTo("usernameNormalized", clean)
                        .limit(1)
                        .get()
                        .await()
                    if (!query.isEmpty) {
                        val doc = query.documents.first()
                        Result.success(Pair(doc.id, doc.getString("email")))
                    } else {
                        Result.success(null)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stores a password reset code with an expiration timestamp in the user's Firestore document.
     */
    suspend fun savePasswordResetCode(uid: String, code: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val expiresAt = com.google.firebase.Timestamp(
                java.util.Date(System.currentTimeMillis() + 15 * 60 * 1000)
            )
            firestore.collection("users").document(uid).update(
                mapOf(
                    "passwordResetCode" to code,
                    "passwordResetExpiresAt" to expiresAt
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
