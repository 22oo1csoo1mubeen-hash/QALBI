package com.wakeus.app.data.wakeup

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.ScheduledAlarm
import com.wakeus.app.data.model.ScheduledAlarmStatus
import com.wakeus.app.data.model.User
import com.wakeus.app.data.user.FirebaseNotConfiguredException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.util.Date

class ScheduledAlarmRepository(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    },
    private val authProvider: () -> FirebaseAuth? = {
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
    },
) {
    suspend fun create(
        sender: User,
        friend: Friend,
        scheduledAtMillis: Long,
        timeZoneId: String,
        message: String,
        scheduleId: String,
    ): Result<String> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val uid = authProvider()?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in before scheduling an alarm."))
        if (uid != sender.id || friend.id == uid || friend.isDemo || friend.connectionId.isBlank()) {
            return Result.failure(IllegalArgumentException("Choose a real connected friend."))
        }
        val now = System.currentTimeMillis()
        if (scheduledAtMillis <= now) {
            return Result.failure(IllegalArgumentException("Choose a date and time in the future."))
        }
        if (scheduledAtMillis > now + MAX_SCHEDULE_AHEAD_MS) {
            return Result.failure(IllegalArgumentException("Schedules can be set up to one year ahead."))
        }
        val normalizedMessage = message.trim()
        if (normalizedMessage.length > MAX_MESSAGE_LENGTH) {
            return Result.failure(IllegalArgumentException("Keep your message under 160 characters."))
        }
        if (runCatching { ZoneId.of(timeZoneId) }.isFailure) {
            return Result.failure(IllegalArgumentException("The device timezone could not be read."))
        }

        return runCatching {
            val connection = firestore.collection("connections").document(friend.connectionId).get().await()
            @Suppress("UNCHECKED_CAST")
            val members = connection.get("memberUids") as? List<String> ?: emptyList()
            if (!connection.exists() || connection.getString("status") != "CONNECTED" ||
                members.size != 2 || uid !in members || friend.id !in members
            ) throw NoActiveConnectionException()

            val senderBlock = firestore.collection("userBlocks").document(uid)
                .collection("blockedUsers").document(friend.id).get().await()
            val receiverBlock = firestore.collection("userBlocks").document(friend.id)
                .collection("blockedUsers").document(uid).get().await()
            if (senderBlock.exists() || receiverBlock.exists()) throw FriendBlockedException()

            val id = scheduleId
            val scheduledAt = Timestamp(Date(scheduledAtMillis))
            val scheduleData = mapOf(
                "scheduleId" to id,
                "senderUid" to uid,
                "senderDisplayName" to sender.displayName,
                "senderUsername" to sender.username,
                "receiverUid" to friend.id,
                "receiverDisplayName" to friend.displayName,
                "receiverUsername" to friend.username,
                "connectionId" to friend.connectionId,
                "message" to normalizedMessage,
                "scheduledAt" to scheduledAt,
                "timeZoneId" to timeZoneId,
                "status" to ScheduledAlarmStatus.SCHEDULED.name,
                "attemptCount" to 0,
                "nextAttemptAt" to scheduledAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "schemaVersion" to 1,
            )
            val scheduleRef = firestore.collection(COLLECTION).document(id)
            val existing = scheduleRef.get().await()
            if (existing.exists()) {
                val sameRequest = existing.getString("senderUid") == uid &&
                    existing.getString("receiverUid") == friend.id &&
                    existing.getTimestamp("scheduledAt")?.toDate()?.time == scheduledAtMillis &&
                    existing.getString("timeZoneId") == timeZoneId &&
                    existing.getString("message").orEmpty() == normalizedMessage
                if (!sameRequest) throw IllegalStateException("This schedule request ID is already in use.")
            } else {
                scheduleRef.set(scheduleData).await()
            }
            id
        }
    }

    fun observeMine(senderUid: String): Flow<List<ScheduledAlarm>> = callbackFlow {
        val firestore = firestoreProvider()
        if (firestore == null || senderUid.isBlank() || authProvider()?.currentUser?.uid != senderUid) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = firestore.collection(COLLECTION)
            .whereEqualTo("senderUid", senderUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val schedules = snapshot.documents.mapNotNull(ScheduledAlarm::fromDocument)
                    .sortedBy(ScheduledAlarm::scheduledAtMillis)
                trySend(schedules)
            }
        awaitClose { listener.remove() }
    }

    suspend fun cancel(schedule: ScheduledAlarm): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val uid = authProvider()?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in before cancelling a schedule."))
        if (schedule.senderUid != uid || !schedule.canBeCancelled) {
            return Result.failure(IllegalStateException("This schedule can no longer be cancelled."))
        }
        return runCatching {
            firestore.collection(COLLECTION).document(schedule.scheduleId).update(
                mapOf(
                    "status" to ScheduledAlarmStatus.CANCELLED.name,
                    "cancelledAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    companion object {
        const val COLLECTION = "scheduledAlarms"
        const val MAX_MESSAGE_LENGTH = 160
        const val MAX_SCHEDULE_AHEAD_MS = 366L * 24 * 60 * 60 * 1000
    }
}
