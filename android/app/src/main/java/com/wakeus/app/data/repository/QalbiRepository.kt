package com.wakeus.app.data.repository

import com.wakeus.app.data.model.ConnectionCode
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpState
import com.wakeus.app.data.auth.AuthRepository
import com.wakeus.app.data.user.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    private val _friendsCache = java.util.concurrent.ConcurrentHashMap<String, Friend>()

    fun getFriendById(id: String): Friend? {
        if (id == demoFriend.id) return demoFriend
        return _friendsCache[id]
    }

    fun cacheFriend(friend: Friend) {
        _friendsCache[friend.id] = friend
    }

    // ─── Phase 2, 3 & 4 Repositories ────────────────────────────────────────
    val authRepository = AuthRepository()
    val userRepository = UserRepository()
    val friendRepository = com.wakeus.app.data.friends.FriendRepository()
    val wakeUpRepository = com.wakeus.app.data.wakeup.WakeUpRepository()
    val scheduledAlarmRepository = com.wakeus.app.data.wakeup.ScheduledAlarmRepository()
    val fcmTokenRepository = com.wakeus.app.data.fcm.FcmTokenRepository()

    val authState: StateFlow<com.wakeus.app.data.auth.AuthState> = authRepository.authState

    /**
     * Observes real connected friends dynamically for the currently authenticated user.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val friends: Flow<List<Friend>> = authRepository.authState.flatMapLatest { state ->
        if (state is com.wakeus.app.data.auth.AuthState.Authenticated) {
            kotlinx.coroutines.flow.flow {
                friendRepository.observeConnectedFriends(state.user.id).collect { list ->
                    list.forEach { _friendsCache[it.id] = it }
                    emit(list)
                }
            }
        } else {
            flowOf(emptyList())
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

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.sendPasswordResetEmail(email)
        } else {
            delay(1000)
            Result.success(Unit)
        }
    }

    suspend fun sendPasswordResetInstructions(emailOrUsername: String): Result<String> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.sendPasswordResetInstructions(emailOrUsername)
        } else {
            delay(1000)
            val isEmail = emailOrUsername.contains("@")
            val target = if (isEmail) emailOrUsername else "$emailOrUsername@example.com"
            Result.success(authRepository.maskEmail(target))
        }
    }

    suspend fun confirmPasswordReset(codeOrLink: String, newPassword: String): Result<Unit> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.confirmPasswordReset(codeOrLink, newPassword)
        } else {
            delay(1000)
            if (newPassword.length < 8) {
                Result.failure(IllegalArgumentException("New password must be at least 8 characters."))
            } else {
                Result.success(Unit)
            }
        }
    }

    suspend fun changePasswordForUsernameAccount(
        username: String,
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {
        return if (authRepository.isFirebaseConfigured()) {
            authRepository.changePasswordForUsernameAccount(username, oldPassword, newPassword)
        } else {
            delay(1000)
            Result.success(Unit)
        }
    }

    suspend fun logout(): Result<Unit> {
        val currentUser = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user
        if (currentUser != null) {
            // Remove this token while Firestore rules still see the user's authenticated
            // session; signing out first made the old fire-and-forget delete fail.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                fcmTokenRepository.unregisterDeviceToken(currentUser.id)
            }
            try {
                val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
                com.wakeus.app.data.fcm.FcmTokenRegistrationWorker.cancel(context, currentUser.id)
            } catch (_: Exception) {}
        }
        com.wakeus.app.data.alarm.AlarmSoundPlayer.stop()
        com.wakeus.app.data.alarm.AlarmVibrator.stop()
        com.wakeus.app.data.alarm.WakeLockHelper.release()
        return authRepository.logout()
    }

    /**
     * Updates the avatar style preference and synchronizes across user profiles.
     */
    suspend fun updateAvatarStyle(avatarStyle: String): Result<Unit> {
        val currentUser = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        val updatedUser = currentUser.copy(avatarStyle = avatarStyle)
        authRepository.updateCurrentUser(updatedUser)
        return if (userRepository.isConfigured() && currentUser.id != demoUser.id) {
            userRepository.updateAvatarStyle(currentUser.id, avatarStyle)
        } else {
            Result.success(Unit)
        }
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

    /**
     * Resolves the active user session safely from in-memory AuthState or Firebase Auth instance.
     */
    fun resolveCurrentUser(): User? {
        val fromState = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user
        if (fromState != null) return fromState

        val fbUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) { null }

        return fbUser?.let {
            User(
                id = it.uid,
                displayName = it.displayName?.takeIf { d -> d.isNotBlank() } ?: (it.email?.substringBefore("@") ?: "User"),
                username = it.email?.substringBefore("@")?.lowercase() ?: "user_${it.uid.take(6)}",
                email = it.email ?: "",
            )
        }
    }

    // ─── Phase 3 Friend Pairing Operations ──────────────────────────────────
    /**
     * Generates and registers an authentic 8-character pairing code in Firestore.
     */
    suspend fun createPairingCode(): Result<com.wakeus.app.data.model.PairingCode> {
        val user = resolveCurrentUser()
            ?: return Result.failure(Exception("Please log in to generate a connection code."))
        return friendRepository.createPairingCode(user)
    }

    /**
     * Cancels / revokes an active pairing code.
     */
    suspend fun revokePairingCode(code: String): Result<Unit> {
        val user = resolveCurrentUser()
            ?: return Result.failure(Exception("Please log in to cancel code."))
        return friendRepository.revokePairingCode(code, user.id)
    }

    /**
     * Inspects a pairing code prior to redemption.
     */
    suspend fun inspectPairingCode(code: String): Result<com.wakeus.app.data.model.PairingCode> {
        return friendRepository.inspectPairingCode(code)
    }

    /**
     * Atomically redeems a pairing code in Firestore, establishing the bidirectional connection.
     */
    suspend fun redeemPairingCode(code: String): Result<Friend> {
        val user = resolveCurrentUser()
            ?: return Result.failure(Exception("Please log in to connect with a friend."))
        return friendRepository.redeemPairingCode(code, user.id)
    }

    /**
     * Disconnects a friend, removing the connection document.
     */
    suspend fun disconnectFriend(connectionId: String): Result<Unit> {
        val user = resolveCurrentUser()
            ?: return Result.failure(Exception("Please log in to disconnect friend."))
        return friendRepository.disconnectFriend(connectionId, user.id)
    }

    /**
     * Blocks a user and severs any existing connection.
     */
    suspend fun blockUser(blockedUid: String, connectionId: String? = null): Result<Unit> {
        val user = resolveCurrentUser()
            ?: return Result.failure(Exception("Please log in to block user."))
        return friendRepository.blockUser(user.id, blockedUid, connectionId)
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

    // ─── Phase 4 Wake-Up Communication ───────────────────────────────────────
    /**
     * Sends an authentic remote wake-up request to a connected friend in Firestore.
     */
    suspend fun sendWakeUp(receiver: Friend): Result<com.wakeus.app.data.model.WakeUpRequest> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (wakeUpRepository.isConfigured() && !receiver.isDemo) {
            wakeUpRepository.sendWakeUpRequest(user, receiver)
        } else {
            delay(1200)
            val now = System.currentTimeMillis()
            Result.success(
                com.wakeus.app.data.model.WakeUpRequest(
                    requestId = "mock-req-" + now.toString().takeLast(6),
                    senderUid = user.id,
                    senderDisplayName = user.displayName,
                    senderUsername = user.username,
                    receiverUid = receiver.id,
                    receiverDisplayName = receiver.displayName,
                    receiverUsername = receiver.username,
                    connectionId = "demo_connection",
                    status = com.wakeus.app.data.model.WakeUpRequestStatus.PENDING,
                    createdAt = now,
                )
            )
        }
    }

    /**
     * Observes real-time updates for an outgoing wake-up request.
     */
    fun observeRequest(requestId: String): Flow<com.wakeus.app.data.model.WakeUpRequest?> {
        return if (wakeUpRepository.isConfigured()) {
            wakeUpRepository.observeRequest(requestId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }

    /**
     * Observes active incoming wake-up requests for the authenticated user.
     */
    val incomingWakeUpRequests: Flow<List<com.wakeus.app.data.model.WakeUpRequest>> = kotlinx.coroutines.flow.flow {
        authState.collect { state ->
            if (state is com.wakeus.app.data.auth.AuthState.Authenticated && wakeUpRepository.isConfigured()) {
                wakeUpRepository.observeIncomingRequests(state.user.id).collect { emit(it) }
            } else {
                emit(emptyList())
            }
        }
    }

    /**
     * Receiver acknowledges the wake-up request ("I'm Awake!").
     */
    suspend fun acknowledgeWakeUp(requestId: String): Result<Unit> {
        val currentFbUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) { null }
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user
        val effectiveUid = currentFbUid ?: user?.id ?: ""
        return if (wakeUpRepository.isConfigured() && effectiveUid.isNotBlank()) {
            wakeUpRepository.acknowledgeRequest(requestId, effectiveUid)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Receiver declines the wake-up request.
     */
    suspend fun declineWakeUp(requestId: String): Result<Unit> {
        val currentFbUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) { null }
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user
        val effectiveUid = currentFbUid ?: user?.id ?: ""
        return if (wakeUpRepository.isConfigured() && effectiveUid.isNotBlank()) {
            wakeUpRepository.declineRequest(requestId, effectiveUid)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Sender cancels a pending wake-up request.
     */
    suspend fun cancelWakeUp(requestId: String): Result<Unit> {
        val user = (authState.value as? com.wakeus.app.data.auth.AuthState.Authenticated)?.user ?: demoUser
        return if (wakeUpRepository.isConfigured()) {
            wakeUpRepository.cancelRequest(requestId, user.id)
        } else {
            Result.success(Unit)
        }
    }

    // ─── Mock wake-up compatibility helper ───────────────────────────────────
    suspend fun sendWakeUpRequest(@Suppress("UNUSED_PARAMETER") friendId: String): WakeUpState {
        delay(1200)
        return WakeUpState.SENT_DEMO
    }
}
