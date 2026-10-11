package com.wakeus.app.data.wakeup

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.wakeus.app.data.model.Connection
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.data.user.FirebaseNotConfiguredException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

// ─── Phase 4 Exception Hierarchy ─────────────────────────────────────────────

open class WakeUpException(message: String) : Exception(message)

class SelfWakeUpException(message: String = "You cannot send a wake-up request to yourself.") :
    WakeUpException(message)

class NoActiveConnectionException(message: String = "You must be connected with this friend to send wake-up requests.") :
    WakeUpException(message)

class FriendBlockedException(message: String = "Cannot send wake-up requests to a blocked user.") :
    WakeUpException(message)

class DuplicateWakeUpRequestException(message: String = "A wake-up request for this friend is already pending. Please wait for them to respond.") :
    WakeUpException(message)

class RequestNotFoundException(message: String = "Wake-up request not found.") :
    WakeUpException(message)

class RequestAlreadyTerminalException(status: String) :
    WakeUpException("This request has already been $status.")

class RequestExpiredException(message: String = "This wake-up request has expired.") :
    WakeUpException(message)

class UnauthorizedRequestActionException(message: String = "You are not authorized to perform this action.") :
    WakeUpException(message)

/**
 * Production-ready repository for remote wake-up communication lifecycle in Cloud Firestore.
 * Concurrency-safe, atomic status transitions under strict ₹0 Spark plan.
 */
class WakeUpRepository(
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

    companion object {
        const val REQUEST_EXPIRY_MS = 15 * 60 * 1000L // 15 minutes default validity
    }

    fun isConfigured(): Boolean = firestoreProvider() != null

    /**
     * Creates and persists a new remote wake-up request after verifying connection, block state, and duplicates.
     */
    suspend fun sendWakeUpRequest(
        sender: User,
        receiver: Friend,
    ): Result<WakeUpRequest> {
        // 1. Domain validation checks
        if (sender.id == receiver.id) {
            return Result.failure(SelfWakeUpException())
        }

        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())

        val connectionId = if (receiver.connectionId.isNotBlank()) {
            receiver.connectionId
        } else {
            Connection.createDeterministicId(sender.id, receiver.id)
        }

        return try {
            // 2. Verify mutual active connection document
            val connDoc = firestore.collection("connections").document(connectionId).get().await()
            if (!connDoc.exists() || connDoc.getString("status") != "CONNECTED") {
                return Result.failure(NoActiveConnectionException())
            }

            // 3. Verify block status
            val block1 = firestore.collection("userBlocks")
                .document(sender.id)
                .collection("blockedUsers")
                .document(receiver.id)
                .get().await()
            val block2 = firestore.collection("userBlocks")
                .document(receiver.id)
                .collection("blockedUsers")
                .document(sender.id)
                .get().await()

            if (block1.exists() || block2.exists()) {
                return Result.failure(FriendBlockedException())
            }

            // 4. Duplicate request prevention: check for any active PENDING request to this receiver
            val pendingQuery = firestore.collection("wakeUpRequests")
                .whereEqualTo("senderUid", sender.id)
                .whereEqualTo("receiverUid", receiver.id)
                .whereEqualTo("status", WakeUpRequestStatus.PENDING.name)
                .get()
                .await()

            val now = System.currentTimeMillis()
            val existingActive = pendingQuery.documents.any { doc ->
                val expiresAt = doc.getTimestamp("expiresAt")?.toDate()?.time ?: (now + 1)
                now < expiresAt
            }

            if (existingActive) {
                return Result.failure(DuplicateWakeUpRequestException())
            }

            // 5. Build and save request document
            val requestId = UUID.randomUUID().toString()
            val expiresAt = now + REQUEST_EXPIRY_MS

            val wakeUpRequest = WakeUpRequest(
                requestId = requestId,
                senderUid = sender.id,
                senderDisplayName = sender.displayName,
                senderUsername = sender.username,
                receiverUid = receiver.id,
                receiverDisplayName = receiver.displayName,
                receiverUsername = receiver.username,
                connectionId = connectionId,
                status = WakeUpRequestStatus.PENDING,
                createdAt = now,
                expiresAt = expiresAt,
                updatedAt = now,
            )

            firestore.collection("wakeUpRequests")
                .document(requestId)
                .set(wakeUpRequest.toMap())
                .await()

            Result.success(wakeUpRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes real-time updates for a specific wake-up request document.
     */
    fun observeRequest(requestId: String): Flow<WakeUpRequest?> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || requestId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("wakeUpRequests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists() || snapshot.data == null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val request = WakeUpRequest.fromMap(snapshot.id, snapshot.data!!)
                trySend(request)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes the latest active outgoing request to a friend.
     */
    fun observeActiveOutgoingRequest(senderUid: String, receiverUid: String): Flow<WakeUpRequest?> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || senderUid.isBlank() || receiverUid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("wakeUpRequests")
            .whereEqualTo("senderUid", senderUid)
            .whereEqualTo("receiverUid", receiverUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val latest = snapshot.documents
                    .mapNotNull { if (it.data != null) WakeUpRequest.fromMap(it.id, it.data!!) else null }
                    .maxByOrNull { it.createdAt }

                trySend(latest)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Observes incoming wake-up requests for [receiverUid].
     * Emits all active requests awaiting response.
     */
    fun observeIncomingRequests(receiverUid: String): Flow<List<WakeUpRequest>> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || receiverUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection("wakeUpRequests")
            .whereEqualTo("receiverUid", receiverUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val now = System.currentTimeMillis()
                val incoming = snapshot.documents.mapNotNull { doc ->
                    if (doc.data != null) {
                        val req = WakeUpRequest.fromMap(doc.id, doc.data!!)
                        if (req.status == WakeUpRequestStatus.PENDING && now < req.expiresAt) {
                            req
                        } else null
                    } else null
                }.sortedByDescending { it.createdAt }

                trySend(incoming)
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Receiver acknowledges the wake-up request ("I'm Awake!").
     * Atomic transition from PENDING -> ACKNOWLEDGED.
     */
    suspend fun acknowledgeRequest(requestId: String, receiverUid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection("wakeUpRequests").document(requestId)
                val snapshot = transaction.get(docRef)

                if (!snapshot.exists() || snapshot.data == null) {
                    throw RequestNotFoundException()
                }

                val request = WakeUpRequest.fromMap(requestId, snapshot.data!!)
                if (request.receiverUid != receiverUid) {
                    throw UnauthorizedRequestActionException("Only the intended recipient can acknowledge this request.")
                }

                if (request.status != WakeUpRequestStatus.PENDING) {
                    throw RequestAlreadyTerminalException(request.status.name.lowercase())
                }

                if (request.isExpired) {
                    throw RequestExpiredException()
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to WakeUpRequestStatus.ACKNOWLEDGED.name,
                        "acknowledgedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Receiver declines the wake-up request.
     * Atomic transition from PENDING -> DECLINED.
     */
    suspend fun declineRequest(requestId: String, receiverUid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection("wakeUpRequests").document(requestId)
                val snapshot = transaction.get(docRef)

                if (!snapshot.exists() || snapshot.data == null) {
                    throw RequestNotFoundException()
                }

                val request = WakeUpRequest.fromMap(requestId, snapshot.data!!)
                if (request.receiverUid != receiverUid) {
                    throw UnauthorizedRequestActionException("Only the intended recipient can decline this request.")
                }

                if (request.status != WakeUpRequestStatus.PENDING) {
                    throw RequestAlreadyTerminalException(request.status.name.lowercase())
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to WakeUpRequestStatus.DECLINED.name,
                        "declinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sender cancels a pending wake-up request before the receiver responds.
     * Atomic transition from PENDING -> CANCELLED.
     */
    suspend fun cancelRequest(requestId: String, senderUid: String): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection("wakeUpRequests").document(requestId)
                val snapshot = transaction.get(docRef)

                if (!snapshot.exists() || snapshot.data == null) {
                    throw RequestNotFoundException()
                }

                val request = WakeUpRequest.fromMap(requestId, snapshot.data!!)
                if (request.senderUid != senderUid) {
                    throw UnauthorizedRequestActionException("Only the sender can cancel this request.")
                }

                if (request.status != WakeUpRequestStatus.PENDING) {
                    throw RequestAlreadyTerminalException(request.status.name.lowercase())
                }

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to WakeUpRequestStatus.CANCELLED.name,
                        "cancelledAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
