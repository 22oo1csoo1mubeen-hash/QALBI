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
 * [isDemo] is always true in Phase 1 — no real pairing exists yet.
 */
data class Friend(
    val id: String,
    val displayName: String,
    val username: String,
    /** Initials shown when no avatar URL is available. */
    val initials: String,
    /** Status string shown in the UI (e.g. "Away", "Active"). */
    val status: String = "Demo",
    /** Always true in Phase 1. Signals that this is mock/demo data. */
    val isDemo: Boolean = true,
)

/** Represents the result of a wake-up request. Always simulated in Phase 1. */
enum class WakeUpState {
    IDLE,
    PENDING,
    SENT_DEMO,    // Simulated send — not a real notification.
    UNAVAILABLE,
}

/** Connection code wrapper. Always a demo code in Phase 1. */
data class ConnectionCode(
    val code: String,
    /** Human-readable expiry label shown in the UI. Always "Demo — not redeemable". */
    val expiryLabel: String = "Demo — not redeemable on any server",
)
