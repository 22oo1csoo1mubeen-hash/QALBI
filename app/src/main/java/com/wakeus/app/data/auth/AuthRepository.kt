package com.wakeus.app.data.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.wakeus.app.data.model.AccountType
import com.wakeus.app.data.model.User
import com.wakeus.app.data.user.FirebaseNotConfiguredException
import com.wakeus.app.data.user.UserRepository
import com.wakeus.app.data.user.UsernameAlreadyTakenException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * High-level state of user authentication.
 */
sealed class AuthState {
    object Initializing : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class ConfigurationRequired(val message: String) : AuthState()
}

/**
 * Authentication repository providing Firebase Authentication and session state.
 */
class AuthRepository(
    private val userRepository: UserRepository = UserRepository(),
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        checkInitialSession()
    }

    /**
     * Checks if Firebase is initialized in the current environment.
     */
    fun isFirebaseConfigured(): Boolean {
        return try {
            authProvider() != null && userRepository.isConfigured()
        } catch (_: Exception) {
            false
        }
    }

    private fun checkInitialSession() {
        val auth = authProvider()
        if (auth == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                scope.launch {
                    loadUserProfile(currentUser)
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    /**
     * Updates the in-memory authenticated user session.
     */
    fun updateCurrentUser(updatedUser: User) {
        if (_authState.value is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(updatedUser)
        }
    }

    private suspend fun loadUserProfile(firebaseUser: FirebaseUser) {
        val profileResult = userRepository.getUserProfile(firebaseUser.uid)
        profileResult.fold(
            onSuccess = { user ->
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    // Profile document missing in Firestore (interrupted registration edge-case).
                    // Recover using Firebase user info:
                    val fallbackUsername = firebaseUser.email?.substringBefore("@")?.lowercase()
                        ?: "user_${firebaseUser.uid.take(6)}"
                    val recoveredUser = User(
                        id = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "User",
                        username = fallbackUsername,
                        usernameNormalized = fallbackUsername.trim().lowercase(),
                        email = if (firebaseUser.email?.endsWith("@users.qalbi.app") == true) null else firebaseUser.email,
                        accountType = if (firebaseUser.email?.endsWith("@users.qalbi.app") == true) AccountType.USERNAME else AccountType.EMAIL,
                    )
                    userRepository.claimUsernameAndCreateProfile(recoveredUser)
                    _authState.value = AuthState.Authenticated(recoveredUser)
                }
            },
            onFailure = {
                // If network/firestore fails temporarily, construct user from Firebase Auth token
                val fallbackUser = User(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "User",
                    username = firebaseUser.email?.substringBefore("@") ?: "user",
                    email = if (firebaseUser.email?.endsWith("@users.qalbi.app") == true) null else firebaseUser.email,
                )
                _authState.value = AuthState.Authenticated(fallbackUser)
            }
        )
    }

    /**
     * Registration Method A: Register with Display Name, Username, Email, and Password.
     */
    suspend fun registerWithEmail(
        displayName: String,
        username: String,
        email: String,
        password: String
    ): Result<User> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val usernameNormalized = username.trim().lowercase()

        return try {
            // 1. Pre-check username availability
            val isAvailable = userRepository.isUsernameAvailable(usernameNormalized).getOrDefault(true)
            if (!isAvailable) {
                return Result.failure(UsernameAlreadyTakenException(username))
            }

            // 2. Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user was null after creation.")

            // 3. Construct user model
            val newUser = User(
                id = firebaseUser.uid,
                displayName = displayName.trim(),
                username = username.trim(),
                usernameNormalized = usernameNormalized,
                email = email.trim(),
                accountType = AccountType.EMAIL,
            )

            // 4. Atomically claim username and create Firestore profile document
            val claimResult = userRepository.claimUsernameAndCreateProfile(newUser)
            if (claimResult.isFailure) {
                val exception = claimResult.exceptionOrNull()
                // Rollback: delete the newly created Firebase Auth account to prevent orphaned user
                try { firebaseUser.delete().await() } catch (_: Exception) {}
                throw exception ?: IllegalStateException("Failed to create profile.")
            }

            // 5. Update Firebase profile display name
            try {
                firebaseUser.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                ).await()
            } catch (_: Exception) {}

            _authState.value = AuthState.Authenticated(newUser)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Registration Method B: Register with Display Name, Username, and Password (no email required).
     * Internal auth identifier uses standard non-routable domain: <username>@users.qalbi.app.
     */
    suspend fun registerWithUsername(
        displayName: String,
        username: String,
        password: String
    ): Result<User> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val usernameNormalized = username.trim().lowercase()
        val internalEmail = "$usernameNormalized@users.qalbi.app"

        return try {
            // 1. Pre-check username availability
            val isAvailable = userRepository.isUsernameAvailable(usernameNormalized).getOrDefault(true)
            if (!isAvailable) {
                return Result.failure(UsernameAlreadyTakenException(username))
            }

            // 2. Create Firebase Auth account using internal identifier
            val authResult = auth.createUserWithEmailAndPassword(internalEmail, password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user was null after creation.")

            // 3. Construct user model
            val newUser = User(
                id = firebaseUser.uid,
                displayName = displayName.trim(),
                username = username.trim(),
                usernameNormalized = usernameNormalized,
                email = null,
                accountType = AccountType.USERNAME,
            )

            // 4. Atomically claim username and create Firestore profile document
            val claimResult = userRepository.claimUsernameAndCreateProfile(newUser)
            if (claimResult.isFailure) {
                val exception = claimResult.exceptionOrNull()
                try { firebaseUser.delete().await() } catch (_: Exception) {}
                throw exception ?: IllegalStateException("Failed to create profile.")
            }

            // 5. Update Firebase profile display name
            try {
                firebaseUser.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                ).await()
            } catch (_: Exception) {}

            _authState.value = AuthState.Authenticated(newUser)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Log in using either email or username with password.
     */
    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<User> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val input = emailOrUsername.trim()

        val authIdentifier = if (input.contains("@")) {
            input
        } else {
            // Username login
            "${input.lowercase()}@users.qalbi.app"
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(authIdentifier, password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user was null after login.")

            // Fetch profile from Firestore
            val profile = userRepository.getUserProfile(firebaseUser.uid).getOrNull()
            val user = profile ?: User(
                id = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: input,
                username = input.substringBefore("@"),
                email = if (firebaseUser.email?.endsWith("@users.qalbi.app") == true) null else firebaseUser.email,
            )

            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Signs out through Firebase Authentication and resets session state.
     */
    fun logout(): Result<Unit> {
        val auth = authProvider()
        return try {
            auth?.signOut()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapFirebaseException(e: Exception): Exception {
        return when (e) {
            is UsernameAlreadyTakenException -> e
            is FirebaseNotConfiguredException -> e
            is FirebaseAuthWeakPasswordException ->
                Exception("Password is too weak. Please use at least 8 characters with a mix of letters and numbers.")
            is FirebaseAuthUserCollisionException ->
                Exception("An account already exists with this email address or username.")
            is FirebaseAuthInvalidUserException ->
                Exception("No account found matching this email or username.")
            is FirebaseAuthInvalidCredentialsException ->
                Exception("Incorrect email, username, or password.")
            is com.google.firebase.FirebaseNetworkException ->
                Exception("Network unavailable. Please check your internet connection and try again.")
            else -> {
                val msg = e.localizedMessage ?: "Authentication failed."
                if (msg.contains("API key not valid", ignoreCase = true) || msg.contains("no-app", ignoreCase = true)) {
                    FirebaseNotConfiguredException()
                } else {
                    Exception(msg)
                }
            }
        }
    }
}
