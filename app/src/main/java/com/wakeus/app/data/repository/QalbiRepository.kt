package com.wakeus.app.data.repository

import com.wakeus.app.data.model.ConnectionCode
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.data.auth.AuthRepository
import com.wakeus.app.data.user.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // ─── Phase 2 & 3 Repositories ──────────────────────────────────────────
    val authRepository = AuthRepository()
    val userRepository = UserRepository()
    val friendRepository = com.wakeus.app.data.friends.FriendRepository()

    val authState: StateFlow<com.wakeus.app.data.auth.AuthState> = authRepository.authState

    /**
     * Observes real connected friends dynamically for the currently authenticated user.
     */
    val friends: Flow<List<Friend>> = kotlinx.coroutines.flow.flow {
        authRepository.authState.collect { state ->
            if (state is com.wakeus.app.data.auth.AuthState.Authenticated) {
                friendRepository.observeConnectedFriends(state.user.id).collect { emit(it) }
            } else {
                emit(emptyList())
            }
        }
    }

    suspend fun registerWithEmail(
        displayName: String,
        username: String,
        email: String,
        password: String
    ): Result<User> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.registerWithEmail(displayName, username, email, password)
        } else {
            // Local preview when Firebase configuration is pending
            delay(1000)
            Result.success(
                User(
                    id = "local-user-" + System.currentTimeMillis().toString().takeLast(6),
                    displayName = displayName.trim(),
                    username = username.trim(),
                    email = email.trim(),
                    accountType = com.wakeus.app.data.model.AccountType.EMAIL,
                )
            )
        }
    }

    suspend fun registerWithUsername(
        displayName: String,
        username: String,
        password: String
    ): Result<User> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.registerWithUsername(displayName, username, password)
        } else {
            delay(1000)
            Result.success(
                User(
                    id = "local-user-" + System.currentTimeMillis().toString().takeLast(6),
                    displayName = displayName.trim(),
                    username = username.trim(),
                    email = null,
                    accountType = com.wakeus.app.data.model.AccountType.USERNAME,
                )
            )
        }
    }

    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<User> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.login(emailOrUsername, password)
        } else {
            delay(1000)
            val isEmail = emailOrUsername.contains("@")
            Result.success(
                User(
                    id = "local-user-" + System.currentTimeMillis().toString().takeLast(6),
                    displayName = if (isEmail) emailOrUsername.substringBefore("@") else emailOrUsername,
                    username = emailOrUsername.substringBefore("@"),
                    email = if (isEmail) emailOrUsername.trim() else null,
                    accountType = if (isEmail) com.wakeus.app.data.model.AccountType.EMAIL else com.wakeus.app.data.model.AccountType.USERNAME,
                )
            )
        }
    }

    fun logout(): Result<Unit> {
        return authRepository.logout()
    }

    // ─── Legacy mock functions for Phase 1 compatibility ─────────────────────
    suspend fun mockRegister(
        displayName: String,
        username: String,
        email: String,
        @Suppress("UNUSED_PARAMETER") password: String,
    ): Result<User> = registerWithEmail(displayName, username, email, password)

    suspend fun mockLogin(
        emailOrUsername: String,
        password: String,
    ): Result<User> = login(emailOrUsername, password)

    // ─── Phase 3 Friend Pairing Operations ──────────────────────────────────
    /**
     * Generates and registers an authentic 8-character pairing code in Firestore.
     */
    suspend fun createPairingCode(): Result<com.wakeus.app.data.model.PairingCode> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (friendRepository.isConfigured()) {
            friendRepository.createPairingCode(user)
        } else {
            delay(600)
            val code = friendRepository.generateSecureCode()
            Result.success(
                com.wakeus.app.data.model.PairingCode(
                    code = code,
                    creatorUid = user.id,
                    creatorDisplayName = user.displayName,
                    creatorUsername = user.username,
                )
            )
        }
    }

    /**
     * Cancels / revokes an active pairing code.
     */
    suspend fun revokePairingCode(code: String): Result<Unit> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (friendRepository.isConfigured()) {
            friendRepository.revokePairingCode(code, user.id)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Inspects a pairing code prior to redemption.
     */
    suspend fun inspectPairingCode(code: String): Result<com.wakeus.app.data.model.PairingCode> {
        return if (friendRepository.isConfigured()) {
            friendRepository.inspectPairingCode(code)
        } else {
            delay(500)
            Result.success(
                com.wakeus.app.data.model.PairingCode(
                    code = code.uppercase(),
                    creatorUid = "demo-friend-999",
                    creatorDisplayName = "Sample Friend",
                    creatorUsername = "sample.friend",
                )
            )
        }
    }

    /**
     * Atomically redeems a pairing code in Firestore, establishing the bidirectional connection.
     */
    suspend fun redeemPairingCode(code: String): Result<Friend> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (friendRepository.isConfigured()) {
            friendRepository.redeemPairingCode(code, user.id)
        } else {
            delay(800)
            Result.success(
                Friend(
                    id = "preview-friend-001",
                    displayName = "New Friend",
                    username = "new_friend",
                    status = "Connected",
                )
            )
        }
    }

    /**
     * Disconnects a friend, removing the connection document.
     */
    suspend fun disconnectFriend(connectionId: String): Result<Unit> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (friendRepository.isConfigured()) {
            friendRepository.disconnectFriend(connectionId, user.id)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Blocks a user and severs any existing connection.
     */
    suspend fun blockUser(blockedUid: String, connectionId: String? = null): Result<Unit> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (friendRepository.isConfigured()) {
            friendRepository.blockUser(user.id, blockedUid, connectionId)
        } else {
            Result.success(Unit)
        }
    }

    // ─── Legacy compatibility functions ──────────────────────────────────────
    suspend fun generateConnectionCode(): ConnectionCode {
        val result = createPairingCode()
        return result.fold(
            onSuccess = { ConnectionCode(it.code, "Expires in 10 minutes") },
            onFailure = { ConnectionCode("ERROR", it.message ?: "Failed to generate") }
        )
    }

    suspend fun submitConnectionCode(code: String): Result<String> {
        return redeemPairingCode(code).map { "Successfully connected with @${it.username}!" }
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
