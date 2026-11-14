package com.wakeus.app.data.wakeup

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
                "senderNotificationSent" to false,
            )
            val scheduleRef = firestore.collection(COLLECTION).document(id)
            try {
                // Write first: reading a fresh random document is denied by rules that only
                // authorize reads of documents with an existing senderUid.
                scheduleRef.set(scheduleData).await()
            } catch (writeError: Exception) {
                // A retry can arrive after Firestore committed the first write but before the
                // client received its response. Updates are intentionally forbidden by rules;
                // read back only after denial and accept only an exact match owned by this user.
                if (writeError !is FirebaseFirestoreException ||
                    writeError.code != FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) throw writeError

                val existing = runCatching { scheduleRef.get().await() }.getOrNull()
                val sameRequest = existing?.exists() == true &&
                    existing.getString("senderUid") == uid &&
                    existing.getString("receiverUid") == friend.id &&
                    existing.getTimestamp("scheduledAt")?.toDate()?.time == scheduledAtMillis &&
                    existing.getString("timeZoneId") == timeZoneId &&
                    existing.getString("message").orEmpty() == normalizedMessage
                if (!sameRequest) throw writeError
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
        var latestSchedules: List<ScheduledAlarm>? = null
        val completedScheduleIds = mutableSetOf<String>()
        fun emitSchedules() {
            val schedules = latestSchedules ?: return
            trySend(schedules.filterNot { it.scheduleId in completedScheduleIds })
        }

        val scheduleListener: ListenerRegistration = firestore.collection(COLLECTION)
            .whereEqualTo("senderUid", senderUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                latestSchedules = snapshot.documents.mapNotNull(ScheduledAlarm::fromDocument)
                    .sortedBy(ScheduledAlarm::scheduledAtMillis)
                emitSchedules()
            }

        // A request can finish before the dispatcher updates its parent schedule.
        // Observe participant-owned requests too, so completed scheduled alarms
        // disappear immediately after an ACK/decline/cancel/expiry transition.
        val requestListener: ListenerRegistration = firestore.collection("wakeUpRequests")
            .whereEqualTo("senderUid", senderUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val terminalStatuses = setOf("ACKNOWLEDGED", "DECLINED", "CANCELLED", "EXPIRED")
                completedScheduleIds.clear()
                snapshot.documents.forEach { request ->
                    if (request.getString("deliverySource") == "CLOUDFLARE_SCHEDULED_ALARM" &&
                        request.getString("status") in terminalStatuses
                    ) {
                        request.getString("scheduleId")?.let(completedScheduleIds::add)
                    }
                }
                emitSchedules()
            }

        awaitClose {
            scheduleListener.remove()
            requestListener.remove()
        }
    }

    suspend fun edit(
        schedule: ScheduledAlarm,
        scheduledAtMillis: Long,
        timeZoneId: String,
        message: String,
    ): Result<Unit> {
        val firestore = firestoreProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val uid = authProvider()?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in before editing a schedule."))
        if (schedule.senderUid != uid || schedule.status != ScheduledAlarmStatus.SCHEDULED) {
            return Result.failure(IllegalStateException("This alarm is no longer editable."))
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
            val ref = firestore.collection(COLLECTION).document(schedule.scheduleId)
            firestore.runTransaction { transaction ->
                val current = transaction.get(ref)
                if (current.getString("senderUid") != uid ||
                    current.getString("status") != ScheduledAlarmStatus.SCHEDULED.name
                ) throw IllegalStateException("This alarm has already started and can no longer be edited.")

                val scheduledAt = Timestamp(Date(scheduledAtMillis))
                transaction.update(
                    ref,
                    mapOf(
                        "scheduledAt" to scheduledAt,
                        "nextAttemptAt" to scheduledAt,
                        "timeZoneId" to timeZoneId,
                        "message" to normalizedMessage,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                Unit
            }.await()
        }
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
