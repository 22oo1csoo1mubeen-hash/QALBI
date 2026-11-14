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
            cacheUsernameEmail(newUser.username, newUser.email ?: email.trim())
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

    private fun getAuthCache(): android.content.SharedPreferences? {
        return try {
            val context = FirebaseApp.getInstance().applicationContext
            context.getSharedPreferences("qalbi_auth_cache", android.content.Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheUsernameEmail(username: String, email: String) {
        try {
            val clean = username.trim().lowercase().removePrefix("@")
            val cleanEmail = email.trim().lowercase()
            if (clean.isNotBlank() && cleanEmail.isNotBlank() && !cleanEmail.endsWith("@users.qalbi.app")) {
                getAuthCache()?.edit()?.putString("user_$clean", cleanEmail)?.apply()
            }
        } catch (_: Exception) {}
    }

    private fun getCachedEmailForUsername(username: String): String? {
        return try {
            val clean = username.trim().lowercase().removePrefix("@")
            getAuthCache()?.getString("user_$clean", null)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Log in using either email or username with password.
     * Automatically resolves usernames to their registered authentication email in Firestore,
     * supporting both email-registered accounts and username-only accounts.
     */
    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<User> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val raw = emailOrUsername.trim()
        val clean = raw.removePrefix("@").trim()

        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Email or username is required."))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password is required."))
        }

        val isEmail = clean.contains("@") && clean.contains(".")

        // Candidate identifiers to try in priority order
        val candidateIdentifiers = mutableListOf<String>()

        if (isEmail) {
            candidateIdentifiers.add(clean.lowercase())
        } else {
            // Candidate 1: Local cache of username -> email from previous sessions
            val cachedEmail = getCachedEmailForUsername(clean)
            if (!cachedEmail.isNullOrBlank() && !cachedEmail.endsWith("@users.qalbi.app")) {
                candidateIdentifiers.add(cachedEmail.lowercase())
            }

            // Candidate 2: Resolved registered email for this username from Firestore
            val resolvedEmail = try {
                userRepository.getEmailForUsername(clean.lowercase()).getOrNull()
            } catch (_: Exception) {
                null
            }

            if (!resolvedEmail.isNullOrBlank() && !resolvedEmail.endsWith("@users.qalbi.app")) {
                if (!candidateIdentifiers.contains(resolvedEmail.lowercase())) {
                    candidateIdentifiers.add(resolvedEmail.lowercase())
                }
                cacheUsernameEmail(clean, resolvedEmail)
            }

            // Candidate 3: Internal domain identifier for username-only accounts
            candidateIdentifiers.add("${clean.lowercase()}@users.qalbi.app")

            // Candidate 4: Raw clean username in case an account was created directly with clean input
            if (!candidateIdentifiers.contains(clean.lowercase()) && clean.contains("@")) {
                candidateIdentifiers.add(clean.lowercase())
            }
        }

        var lastException: Exception? = null

        for (authIdentifier in candidateIdentifiers) {
            try {
                val authResult = auth.signInWithEmailAndPassword(authIdentifier, password).await()
                val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user was null after login.")

                // Fetch user profile from Firestore
                val profile = userRepository.getUserProfile(firebaseUser.uid).getOrNull()
                val user = profile ?: User(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: clean,
                    username = clean.substringBefore("@"),
                    email = if (firebaseUser.email?.endsWith("@users.qalbi.app") == true) null else firebaseUser.email,
                )

                // Cache the username -> email mapping for instant offline resolution
                if (!user.email.isNullOrBlank()) {
                    cacheUsernameEmail(user.username, user.email)
                    cacheUsernameEmail(clean, user.email)
                } else if (firebaseUser.email != null && !firebaseUser.email!!.endsWith("@users.qalbi.app")) {
                    cacheUsernameEmail(user.username, firebaseUser.email!!)
                    cacheUsernameEmail(clean, firebaseUser.email!!)
                }

                // Ensure public profile exists for friend pairing
                try { userRepository.ensurePublicProfile(user) } catch (_: Exception) {}

                _authState.value = AuthState.Authenticated(user)
                return Result.success(user)
            } catch (e: Exception) {
                lastException = e
                // For network failure or unconfigured firebase, fail immediately without trying other candidates
                if (e is com.google.firebase.FirebaseNetworkException || e is FirebaseNotConfiguredException) {
                    return Result.failure(mapFirebaseException(e))
                }
            }
        }

        return Result.failure(mapFirebaseException(lastException ?: IllegalStateException("Login failed.")))
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

    /**
     * Sends password reset email for an email-based account using Firebase Authentication.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Resolves the target account (by email or username) and sends the Firebase password reset email.
     * Returns the masked email (e.g. m***@example.com) so the user knows which inbox to inspect.
     */
    suspend fun sendPasswordResetInstructions(emailOrUsername: String): Result<String> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val clean = emailOrUsername.trim().removePrefix("@").trim()

        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email or username."))
        }

        val isEmail = clean.contains("@") && clean.contains(".")
        val targetEmail: String = if (isEmail) {
            clean.lowercase()
        } else {
            val resolved = userRepository.getEmailForUsername(clean.lowercase()).getOrNull()
            if (resolved.isNullOrBlank() || resolved.endsWith("@users.qalbi.app")) {
                return Result.failure(
                    IllegalArgumentException("The account '@$clean' does not have a registered email address. If you know your current password, you can update it below.")
                )
            }
            resolved.lowercase()
        }

        return try {
            auth.sendPasswordResetEmail(targetEmail).await()
            val masked = maskEmail(targetEmail)
            Result.success(masked)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Completes password reset in-app using the reset code or link from the reset email.
     */
    suspend fun confirmPasswordReset(
        codeOrLink: String,
        newPassword: String
    ): Result<Unit> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val cleanCode = extractResetCode(codeOrLink)

        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your reset code or paste the reset link."))
        }
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("New password must be at least 8 characters."))
        }

        return try {
            auth.confirmPasswordReset(cleanCode, newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    /**
     * Extracts the action code (oobCode) if the user pastes the full email link.
     */
    fun extractResetCode(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.contains("oobCode=")) {
            try {
                android.net.Uri.parse(trimmed).getQueryParameter("oobCode")
                    ?: Regex("oobCode=([^&\\s]+)").find(trimmed)?.groupValues?.get(1)
                    ?: trimmed
            } catch (_: Exception) {
                Regex("oobCode=([^&\\s]+)").find(trimmed)?.groupValues?.get(1) ?: trimmed
            }
        } else {
            trimmed
        }
    }

    /**
     * Masks an email address for privacy (e.g. s***n@gmail.com).
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        val maskedName = if (name.length <= 2) {
            "${name.take(1)}*"
        } else {
            "${name.first()}***${name.last()}"
        }
        return "$maskedName@$domain"
    }

    /**
     * Re-authenticates and updates password for a username-only account.
     * Uses authentic Firebase Auth signInWithEmailAndPassword to securely verify the old password.
     * No passwords are ever stored or queried in Firestore or logged.
     */
    suspend fun changePasswordForUsernameAccount(
        username: String,
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {
        val auth = authProvider() ?: return Result.failure(FirebaseNotConfiguredException())
        val usernameNormalized = username.trim().lowercase().removePrefix("@")
        
        // Try resolved email first, then internal domain
        val resolvedEmail = userRepository.getEmailForUsername(usernameNormalized).getOrNull()
        val internalEmail = if (!resolvedEmail.isNullOrBlank()) resolvedEmail else "$usernameNormalized@users.qalbi.app"

        if (oldPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your current password."))
        }
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("New password must be at least 8 characters."))
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(internalEmail, oldPassword).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("User not found.")
            firebaseUser.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseException(e))
        }
    }

    private fun mapFirebaseException(e: Exception): Exception {
        val msg = e.localizedMessage ?: "Authentication failed."
        if (msg.contains("invalid-action-code", ignoreCase = true) ||
            msg.contains("action code", ignoreCase = true)
        ) {
            return Exception("The reset code is invalid, expired, or has already been used. Please request a new reset link.")
        }

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
                if (msg.contains("API key not valid", ignoreCase = true) || msg.contains("no-app", ignoreCase = true)) {
                    FirebaseNotConfiguredException()
                } else {
                    Exception(msg)
                }
            }
        }
    }
}
