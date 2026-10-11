package com.wakeus.app.data.model

/**
 * Represents a user within the QALBI app.
 * In Phase 1 this is populated only with demo/mock values.
 * In later phases this will be backed by Firebase Auth and Firestore.
 */
data class User(
    val id: String,
    val displayName: String,
    val username: String,
    val email: String,
)

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
