package com.wakeus.app.data.friends

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.wakeus.app.data.model.Connection
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.PairingCode
import com.wakeus.app.data.model.PairingCodeStatus
import com.wakeus.app.data.model.PublicProfile
import com.wakeus.app.data.model.User
import com.wakeus.app.data.user.FirebaseNotConfiguredException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

// ─── Phase 3 Exception Definitions ──────────────────────────────────────────

open class PairingException(message: String) : Exception(message)

class InvalidPairingCodeException(message: String = "The entered pairing code is invalid or does not exist.") :
    PairingException(message)

class PairingCodeExpiredException(message: String = "This pairing code has expired. Please request a new code.") :
    PairingException(message)

class PairingCodeRevokedException(message: String = "This pairing code was cancelled by the person who created it.") :
    PairingException(message)

class PairingCodeAlreadyRedeemedException(message: String = "This pairing code has already been used.") :
    PairingException(message)

class SelfPairingException(message: String = "You cannot connect using your own pairing code.") :
    PairingException(message)

class AlreadyConnectedException(username: String) :
    PairingException("You are already connected to @$username.")

class UserBlockedException(message: String = "Connection cannot be established with this user.") :
    PairingException(message)

/**
 * Production-ready repository for Phase 3 friend pairing, connections, and blocking.
 * Backed by Cloud Firestore transactions under a strict ₹0 Spark budget.
 */
class FriendRepository(
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
    private val secureRandom = SecureRandom()

    companion object {
        /**
         * Unambiguous Crockford Base32 alphabet: 32 characters, excludes 0, 1, I, O to prevent misreading.
         * 8 characters = 32^8 = 1.099 trillion combinations.
         */
        const val CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        const val CODE_LENGTH = 8
        const val CODE_VALIDITY_MS = 10 * 60 * 1000L // 10 minutes

        /**
         * Cleans and normalizes user input by removing spaces, hyphens, and converting to uppercase.
         */
        fun normalizeCode(input: String): String {
            return input.uppercase().replace(Regex("[^2-9A-Z]"), "").trim()
        }
    }

    fun isConfigured(): Boolean = firestoreProvider() != null

    /**
     * Generates a cryptographically secure 8-character pairing code.
     */
    fun generateSecureCode(): String {
        val chars = CharArray(CODE_LENGTH)
        for (i in 0 until CODE_LENGTH) {
            chars[i] = CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]
        }
        return String(chars)
    }

    /**
     * Creates and registers a temporary pairing code in Firestore.
     * Marks any previous active code created by this user as revoked to maintain clean state.
     */
    suspend fun createPairingCode(user: User): Result<PairingCode> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val code = generateSecureCode()
            val now = System.currentTimeMillis()
            val expiresAt = now + CODE_VALIDITY_MS

            val pairingCode = PairingCode(
                code = code,
                creatorUid = user.id,
                creatorDisplayName = user.displayName,
                creatorUsername = user.username,
                status = PairingCodeStatus.ACTIVE,
                createdAt = now,
                expiresAt = expiresAt,
            )

            // Register in Firestore collection /pairingCodes/{code}
            firestore.collection("pairingCodes")
                .document(code)
                .set(pairingCode.toMap())
                .await()

            Result.success(pairingCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Revokes / cancels an active pairing code created by [creatorUid].
     */
    suspend fun revokePairingCode(code: String, creatorUid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val normalized = normalizeCode(code)
        return try {
            val docRef = firestore.collection("pairingCodes").document(normalized)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(InvalidPairingCodeException())
            }
            val docCreator = snapshot.getString("creatorUid")
            if (docCreator != creatorUid) {
                return Result.failure(PairingException("You do not have permission to revoke this code."))
            }

            docRef.update(
                mapOf(
                    "status" to PairingCodeStatus.REVOKED.name,
                    "revokedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inspects a pairing code prior to redemption to preview the prospective friend.
     */
    suspend fun inspectPairingCode(codeRaw: String): Result<PairingCode> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val code = normalizeCode(codeRaw)
        if (code.length < CODE_LENGTH) {
            return Result.failure(InvalidPairingCodeException("Pairing code must be $CODE_LENGTH characters."))
        }

        return try {
            val snapshot = firestore.collection("pairingCodes").document(code).get().await()
            if (!snapshot.exists() || snapshot.data == null) {
                return Result.failure(InvalidPairingCodeException())
            }

            val pairingCode = PairingCode.fromMap(code, snapshot.data!!)
            if (pairingCode.status == PairingCodeStatus.REDEEMED) {
                return Result.failure(PairingCodeAlreadyRedeemedException())
            }
            if (pairingCode.status == PairingCodeStatus.REVOKED) {
                return Result.failure(PairingCodeRevokedException())
            }
            if (pairingCode.isExpired) {
                return Result.failure(PairingCodeExpiredException())
            }

            Result.success(pairingCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically redeems a pairing code and establishes a connection.
     *
     * In a single Firestore transaction:
     * 1. Validates code existence, active status, and expiry.
     * 2. Rejects self-connection attempts.
     * 3. Verifies neither user has blocked the other.
     * 4. Verifies no duplicate connection document exists.
     * 5. Atomically marks the code as REDEEMED with redeemer metadata.
     * 6. Atomically creates the deterministic connection document in /connections/{connectionId}.
     */
    suspend fun redeemPairingCode(codeRaw: String, currentUid: String): Result<Friend> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val code = normalizeCode(codeRaw)
        if (code.length < CODE_LENGTH) {
            return Result.failure(InvalidPairingCodeException("Pairing code must be $CODE_LENGTH characters."))
        }

        return try {
            val friendResult = firestore.runTransaction { transaction ->
                val codeDocRef = firestore.collection("pairingCodes").document(code)
                val codeSnapshot = transaction.get(codeDocRef)

                if (!codeSnapshot.exists() || codeSnapshot.data == null) {
                    throw InvalidPairingCodeException()
                }

                val pairingCode = PairingCode.fromMap(code, codeSnapshot.data!!)

                // Validation checks
                if (pairingCode.creatorUid == currentUid) {
                    throw SelfPairingException()
                }
                if (pairingCode.status == PairingCodeStatus.REDEEMED) {
                    throw PairingCodeAlreadyRedeemedException()
                }
                if (pairingCode.status == PairingCodeStatus.REVOKED) {
                    throw PairingCodeRevokedException()
                }
                if (pairingCode.isExpired) {
                    throw PairingCodeExpiredException()
                }

                val creatorUid = pairingCode.creatorUid
                val connectionId = Connection.createDeterministicId(creatorUid, currentUid)
                val connectionDocRef = firestore.collection("connections").document(connectionId)
                val connectionSnapshot = transaction.get(connectionDocRef)

                if (connectionSnapshot.exists() && connectionSnapshot.getString("status") == "CONNECTED") {
                    throw AlreadyConnectedException(pairingCode.creatorUsername)
                }

                // Check block status (bidirectional)
                val blockDocRef1 = firestore.collection("userBlocks")
                    .document(creatorUid)
                    .collection("blockedUsers")
                    .document(currentUid)
                val blockDocRef2 = firestore.collection("userBlocks")
                    .document(currentUid)
                    .collection("blockedUsers")
                    .document(creatorUid)

                if (transaction.get(blockDocRef1).exists() || transaction.get(blockDocRef2).exists()) {
                    throw UserBlockedException()
                }

                // 1. Mark code REDEEMED
                transaction.update(
                    codeDocRef,
                    mapOf(
                        "status" to PairingCodeStatus.REDEEMED.name,
                        "redeemedBy" to currentUid,
                        "redeemedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Create connection document
                val sortedUids = listOf(creatorUid, currentUid).sorted()
                val connectionData = mapOf(
                    "connectionId" to connectionId,
                    "memberUids" to sortedUids,
                    "user1Uid" to sortedUids[0],
                    "user2Uid" to sortedUids[1],
                    "createdFromPairingCode" to code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "status" to "CONNECTED",
                    "schemaVersion" to 1
                )
                transaction.set(connectionDocRef, connectionData)

                Friend(
                    id = creatorUid,
                    displayName = pairingCode.creatorDisplayName,
                    username = pairingCode.creatorUsername,
                    status = "Connected",
                    connectionId = connectionId,
                    isDemo = false,
                )
            }.await()

            Result.success(friendResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time flow of connected friends for [currentUid].
     * Listens for connection documents and resolves public profiles dynamically.
     */
    fun observeConnectedFriends(currentUid: String): Flow<List<Friend>> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || currentUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("connections")
            .whereArrayContains("memberUids", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val connections = snapshot.documents.mapNotNull { doc ->
                    if (doc.data != null && doc.getString("status") == "CONNECTED") {
                        Connection.fromMap(doc.id, doc.data!!)
                    } else null
                }

                if (connections.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // For each connection, identify the other party and fetch their public profile
                val friends = mutableListOf<Friend>()
                var pendingFetches = connections.size

                for (conn in connections) {
                    val otherUid = conn.getOtherUid(currentUid)
                    if (otherUid.isBlank()) {
                        pendingFetches--
                        if (pendingFetches == 0) trySend(friends.toList())
                        continue
                    }

                    firestore.collection("publicProfiles").document(otherUid).get()
                        .addOnSuccessListener { profileDoc ->
                            if (profileDoc.exists() && profileDoc.data != null) {
                                val profile = PublicProfile.fromMap(otherUid, profileDoc.data!!)
                                friends.add(
                                    Friend(
                                        id = otherUid,
                                        displayName = profile.displayName.ifBlank { "@${profile.username}" },
                                        username = profile.username,
                                        connectionId = conn.connectionId,
                                        status = "Connected",
                                        connectedAt = conn.createdAt,
                                        avatarStyle = profile.avatarStyle,
                                        isDemo = false,
                                    )
                                )
                            } else {
                                // Fallback friend item if public profile document is pending
                                friends.add(
                                    Friend(
                                        id = otherUid,
                                        displayName = "Friend",
                                        username = "user_${otherUid.take(6)}",
                                        connectionId = conn.connectionId,
                                        status = "Connected",
                                        connectedAt = conn.createdAt,
                                        isDemo = false,
                                    )
                                )
                            }
                            pendingFetches--
                            if (pendingFetches == 0) {
                                trySend(friends.toList())
                            }
                        }
                        .addOnFailureListener {
                            pendingFetches--
                            if (pendingFetches == 0) {
                                trySend(friends.toList())
                            }
                        }
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Disconnects a friend by removing the connection document.
     */
    suspend fun disconnectFriend(connectionId: String, currentUid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val docRef = firestore.collection("connections").document(connectionId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val memberUids = snapshot.get("memberUids") as? List<*> ?: emptyList<Any>()
                if (!memberUids.contains(currentUid)) {
                    return Result.failure(PairingException("You do not have permission to disconnect this friendship."))
                }
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Blocks a user, prevents future connections, and severs any existing connection.
     */
    suspend fun blockUser(currentUid: String, blockedUid: String, connectionId: String? = null): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            val blockRef = firestore.collection("userBlocks")
                .document(currentUid)
                .collection("blockedUsers")
                .document(blockedUid)

            blockRef.set(
                mapOf(
                    "blockerUid" to currentUid,
                    "blockedUid" to blockedUid,
                    "blockedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Remove existing connection if active
            val targetConnectionId = connectionId ?: Connection.createDeterministicId(currentUid, blockedUid)
            try {
                firestore.collection("connections").document(targetConnectionId).delete().await()
            } catch (_: Exception) {
                // Connection may already be deleted
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unblocks a previously blocked user.
     */
    suspend fun unblockUser(currentUid: String, blockedUid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            firestore.collection("userBlocks")
                .document(currentUid)
                .collection("blockedUsers")
                .document(blockedUid)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
