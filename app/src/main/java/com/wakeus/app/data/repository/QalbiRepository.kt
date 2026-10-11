package com.wakeus.app.data.repository

import com.wakeus.app.data.model.ConnectionCode
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * QalbiRepository is the single data-access boundary between the UI layer
 * and any future backend integration.
 *
 * ALL data returned here is local mock/demo data.
 * In Phase 3+ this class will be replaced or extended with:
 *   - Firebase Authentication calls
 *   - Firestore reads/writes for friends and codes
 *   - FCM for remote wake-up
 *
 * IMPORTANT: No passwords are stored, persisted, or logged anywhere.
 */
object QalbiRepository {

    // ─── Mock demo user ──────────────────────────────────────────────────────
    val demoUser = User(
        id          = "demo-user-001",
        displayName = "Alex Morningstar",
        username    = "alex.demo",
        email       = "alex.demo@example.com",
    )

    // ─── Connected friends (empty list — Phase 1 requirement) ────────────────
    // Friends list is intentionally empty to reflect honest empty state.
    // To enable a demo flow, a single demo friend is exposed separately below.
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: Flow<List<Friend>> = _friends.asStateFlow()

    /** Demo friend shown only when the user navigates to WakeUp via explicit demo link. */
    val demoFriend = Friend(
        id          = "demo-friend-001",
        displayName = "Sam Rivera",
        username    = "sam.demo",
        initials    = "SR",
        status      = "Demo Profile",
        isDemo      = true,
    )

    fun getFriendById(id: String): Friend? {
        return if (id == demoFriend.id) demoFriend else null
    }

    // ─── Mock authentication ─────────────────────────────────────────────────
    /**
     * Simulates a registration API call.
     * Returns [Result.success] after a short delay.
     * Passwords are validated only in memory and are NEVER stored.
     */
    suspend fun mockRegister(
        displayName: String,
        username: String,
        email: String,
        // Password is accepted here only so the ViewModel can validate length/match.
        // It is discarded immediately after this function returns.
        @Suppress("UNUSED_PARAMETER") password: String,
    ): Result<User> {
        delay(1500) // Simulate network latency
        return Result.success(demoUser.copy(displayName = displayName, username = username, email = email))
    }

    /**
     * Simulates a login API call.
     * Returns [Result.success] after a short delay.
     * Credentials are never stored.
     */
    suspend fun mockLogin(
        @Suppress("UNUSED_PARAMETER") emailOrUsername: String,
        @Suppress("UNUSED_PARAMETER") password: String,
    ): Result<User> {
        delay(1200) // Simulate network latency
        return Result.success(demoUser)
    }

    // ─── Mock connection code ─────────────────────────────────────────────────
    /**
     * Generates a UI-only connection code.
     * This code is NOT redeemable on any server and is purely decorative
     * for Phase 1 demonstration purposes.
     */
    suspend fun generateConnectionCode(): ConnectionCode {
        delay(600)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..8).map { chars.random(Random) }.joinToString("")
        return ConnectionCode(code = code)
    }

    /**
     * Simulates entering a friend's code.
     * Always returns a "not yet functional" response in Phase 1.
     */
    suspend fun submitConnectionCode(code: String): Result<String> {
        delay(1000)
        return Result.failure(
            UnsupportedOperationException(
                "Real friend pairing will be enabled in Phase 3. " +
                "Code '$code' was not sent to any server."
            )
        )
    }

    // ─── Mock wake-up ─────────────────────────────────────────────────────────
    /**
     * Simulates sending a wake-up request.
     * No real notification or alarm is sent.
     * Returns [WakeUpState.SENT_DEMO] after a short delay.
     */
    suspend fun sendWakeUpRequest(@Suppress("UNUSED_PARAMETER") friendId: String): WakeUpState {
        delay(1500)
        return WakeUpState.SENT_DEMO
    }
}
