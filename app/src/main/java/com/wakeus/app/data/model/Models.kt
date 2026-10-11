package com.wakeus.app.data.model

/**
 * Registration method / account type supported by QALBI.
 */
enum class AccountType {
    EMAIL,
    USERNAME,
}

/**
 * Represents an authenticated user within the QALBI app.
 * In Phase 2, this is backed by Firebase Authentication and Cloud Firestore.
 */
data class User(
    val id: String,
    val displayName: String,
    val username: String,
    val usernameNormalized: String = username.lowercase().trim(),
    val email: String? = null,
    val accountType: AccountType = if (email.isNullOrBlank()) AccountType.USERNAME else AccountType.EMAIL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val profileVersion: Int = 1,
) {
    /** Backward compatibility helper for displays expecting non-null string. */
    val emailDisplay: String get() = email ?: "Username-only account"

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): User {
            val displayName = data["displayName"] as? String ?: ""
            val username = data["username"] as? String ?: ""
            val usernameNormalized = data["usernameNormalized"] as? String ?: username.lowercase().trim()
            val email = data["email"] as? String
            val accountTypeStr = data["accountType"] as? String
            val accountType = if (accountTypeStr == "USERNAME") AccountType.USERNAME else AccountType.EMAIL
            val createdAt = when (val c = data["createdAt"]) {
                is com.google.firebase.Timestamp -> c.toDate().time
                is Number -> c.toLong()
                else -> System.currentTimeMillis()
            }
            val updatedAt = when (val u = data["updatedAt"]) {
                is com.google.firebase.Timestamp -> u.toDate().time
                is Number -> u.toLong()
                else -> System.currentTimeMillis()
            }
            val profileVersion = (data["profileVersion"] as? Number)?.toInt() ?: 1

            return User(
                id = id,
                displayName = displayName,
                username = username,
                usernameNormalized = usernameNormalized,
                email = email,
                accountType = accountType,
                createdAt = createdAt,
                updatedAt = updatedAt,
                profileVersion = profileVersion,
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to id,
            "displayName" to displayName,
            "username" to username,
            "usernameNormalized" to usernameNormalized,
            "email" to email,
            "accountType" to accountType.name,
            "createdAt" to com.google.firebase.Timestamp(java.util.Date(createdAt)),
            "updatedAt" to com.google.firebase.Timestamp(java.util.Date(updatedAt)),
            "profileVersion" to profileVersion,
        )
    }
}

/**
 * Represents a connected friend.
 */
data class Friend(
    val id: String,
    val displayName: String,
    val username: String,
    /** Initials shown when no avatar URL is available. */
    val initials: String = displayName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { username.take(2).uppercase() },
    /** Status string shown in the UI (e.g. "Connected", "Away"). */
    val status: String = "Connected",
    /** True only for simulated mock/demo friends. */
    val isDemo: Boolean = false,
    /** Deterministic connection ID in Firestore. */
    val connectionId: String = "",
    val connectedAt: Long = System.currentTimeMillis(),
)

/**
 * Minimal public profile representation for friend discovery and pairing.
 * Omits private fields such as email, account type, and settings.
 */
data class PublicProfile(
    val uid: String,
    val displayName: String,
    val username: String,
    val usernameNormalized: String = username.lowercase().trim(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "username" to username,
        "usernameNormalized" to usernameNormalized,
        "updatedAt" to com.google.firebase.Timestamp(java.util.Date(updatedAt)),
    )

    companion object {
        fun fromMap(uid: String, data: Map<String, Any?>): PublicProfile {
            val displayName = data["displayName"] as? String ?: ""
            val username = data["username"] as? String ?: ""
            val usernameNormalized = data["usernameNormalized"] as? String ?: username.lowercase().trim()
            val updatedAt = when (val u = data["updatedAt"]) {
                is com.google.firebase.Timestamp -> u.toDate().time
                is Number -> u.toLong()
                else -> System.currentTimeMillis()
            }
            return PublicProfile(uid, displayName, username, usernameNormalized, updatedAt)
        }
    }
}

/** Status lifecycle of a temporary pairing code. */
enum class PairingCodeStatus {
    ACTIVE,
    REDEEMED,
    REVOKED,
    EXPIRED,
}

/**
 * Temporary, expiring friend pairing code.
 */
data class PairingCode(
    val code: String,
    val creatorUid: String,
    val creatorDisplayName: String,
    val creatorUsername: String,
    val status: PairingCodeStatus = PairingCodeStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 10 * 60 * 1000L,
    val redeemedBy: String? = null,
    val redeemedAt: Long? = null,
    val revokedAt: Long? = null,
    val schemaVersion: Int = 1,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAt
    val remainingMinutes: Long get() = ((expiresAt - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)

    fun toMap(): Map<String, Any?> = mapOf(
        "code" to code,
        "creatorUid" to creatorUid,
        "creatorDisplayName" to creatorDisplayName,
        "creatorUsername" to creatorUsername,
        "status" to status.name,
        "createdAt" to com.google.firebase.Timestamp(java.util.Date(createdAt)),
        "expiresAt" to com.google.firebase.Timestamp(java.util.Date(expiresAt)),
        "redeemedBy" to redeemedBy,
        "redeemedAt" to (redeemedAt?.let { com.google.firebase.Timestamp(java.util.Date(it)) }),
        "revokedAt" to (revokedAt?.let { com.google.firebase.Timestamp(java.util.Date(it)) }),
        "schemaVersion" to schemaVersion,
    )

    companion object {
        fun fromMap(code: String, data: Map<String, Any?>): PairingCode {
            val creatorUid = data["creatorUid"] as? String ?: ""
            val creatorDisplayName = data["creatorDisplayName"] as? String ?: ""
            val creatorUsername = data["creatorUsername"] as? String ?: ""
            val statusStr = data["status"] as? String ?: PairingCodeStatus.ACTIVE.name
            val status = try {
                PairingCodeStatus.valueOf(statusStr)
            } catch (_: Exception) {
                PairingCodeStatus.ACTIVE
            }
            val createdAt = when (val c = data["createdAt"]) {
                is com.google.firebase.Timestamp -> c.toDate().time
                is Number -> c.toLong()
                else -> System.currentTimeMillis()
            }
            val expiresAt = when (val e = data["expiresAt"]) {
                is com.google.firebase.Timestamp -> e.toDate().time
                is Number -> e.toLong()
                else -> createdAt + 10 * 60 * 1000L
            }
            val redeemedBy = data["redeemedBy"] as? String
            val redeemedAt = (data["redeemedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
            val revokedAt = (data["revokedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
            val schemaVersion = (data["schemaVersion"] as? Number)?.toInt() ?: 1

            return PairingCode(
                code = code,
                creatorUid = creatorUid,
                creatorDisplayName = creatorDisplayName,
                creatorUsername = creatorUsername,
                status = status,
                createdAt = createdAt,
                expiresAt = expiresAt,
                redeemedBy = redeemedBy,
                redeemedAt = redeemedAt,
                revokedAt = revokedAt,
                schemaVersion = schemaVersion,
            )
        }
    }
}

/**
 * Represents an established, bidirectional friend connection document in Firestore.
 */
data class Connection(
    val connectionId: String,
    val memberUids: List<String>,
    val user1Uid: String,
    val user2Uid: String,
    val createdFromPairingCode: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "CONNECTED",
    val schemaVersion: Int = 1,
) {
    fun getOtherUid(currentUid: String): String {
        return if (memberUids.getOrNull(0) == currentUid) {
            memberUids.getOrNull(1) ?: ""
        } else {
            memberUids.getOrNull(0) ?: ""
        }
    }

    companion object {
        /**
         * Generates a deterministic connection ID by lexicographically sorting user IDs.
         * Guarantees exactly one document between any two users.
         */
        fun createDeterministicId(uidA: String, uidB: String): String {
            return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
        }

        fun fromMap(id: String, data: Map<String, Any?>): Connection {
            @Suppress("UNCHECKED_CAST")
            val memberUids = data["memberUids"] as? List<String> ?: emptyList()
            val user1Uid = data["user1Uid"] as? String ?: memberUids.getOrNull(0) ?: ""
            val user2Uid = data["user2Uid"] as? String ?: memberUids.getOrNull(1) ?: ""
            val createdFromPairingCode = data["createdFromPairingCode"] as? String ?: ""
            val createdAt = when (val c = data["createdAt"]) {
                is com.google.firebase.Timestamp -> c.toDate().time
                is Number -> c.toLong()
                else -> System.currentTimeMillis()
            }
            val status = data["status"] as? String ?: "CONNECTED"
            val schemaVersion = (data["schemaVersion"] as? Number)?.toInt() ?: 1

            return Connection(
                connectionId = id,
                memberUids = memberUids,
                user1Uid = user1Uid,
                user2Uid = user2Uid,
                createdFromPairingCode = createdFromPairingCode,
                createdAt = createdAt,
                status = status,
                schemaVersion = schemaVersion,
            )
        }
    }
}

/** Represents the result of a wake-up request. */
enum class WakeUpState {
    IDLE,
    PENDING,
    SENT_DEMO,
    UNAVAILABLE,
}

/** Connection code wrapper for UI consumption. */
data class ConnectionCode(
    val code: String,
    val expiryLabel: String = "Expires in 10 minutes",
)
