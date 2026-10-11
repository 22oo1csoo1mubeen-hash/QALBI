package com.wakeus.app

import com.wakeus.app.data.model.AccountType
import com.wakeus.app.data.model.User
import com.wakeus.app.viewmodel.RegisterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for Phase 2 authentication validation logic,
 * username normalization, and User model handling.
 */
class AuthValidationTest {

    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        viewModel = RegisterViewModel()
    }

    // ─── Username Validation Tests ───────────────────────────────────────────

    @Test
    fun validateUsername_validUsernames_returnNull() {
        val validUsernames = listOf("alex", "john_doe", "user123", "a_b_c", "mubeen_dev_99")
        for (username in validUsernames) {
            assertNull("Expected '$username' to be valid", viewModel.validateUsername(username))
        }
    }

    @Test
    fun validateUsername_tooShort_returnsError() {
        assertNotNull(viewModel.validateUsername("ab"))
        assertNotNull(viewModel.validateUsername("a"))
        assertNotNull(viewModel.validateUsername(""))
        assertNotNull(viewModel.validateUsername("   "))
    }

    @Test
    fun validateUsername_tooLong_returnsError() {
        val longUsername = "a".repeat(21)
        assertNotNull(viewModel.validateUsername(longUsername))
    }

    @Test
    fun validateUsername_invalidCharacters_returnsError() {
        val invalidUsernames = listOf(
            "alex@domain",
            "user-dash",
            "name with space",
            "user!name",
            "user#hash",
            "user.dot"
        )
        for (username in invalidUsernames) {
            assertNotNull("Expected '$username' to be rejected", viewModel.validateUsername(username))
        }
    }

    // ─── Display Name Validation Tests ───────────────────────────────────────

    @Test
    fun validateDisplayName_valid_returnNull() {
        assertNull(viewModel.validateDisplayName("Alex Rivera"))
        assertNull(viewModel.validateDisplayName("Mubeen"))
    }

    @Test
    fun validateDisplayName_blankOrTooShort_returnsError() {
        assertNotNull(viewModel.validateDisplayName(""))
        assertNotNull(viewModel.validateDisplayName("   "))
        assertNotNull(viewModel.validateDisplayName("A"))
    }

    @Test
    fun validateDisplayName_tooLong_returnsError() {
        assertNotNull(viewModel.validateDisplayName("A".repeat(51)))
    }

    // ─── Email Validation Tests ──────────────────────────────────────────────

    @Test
    fun validateEmail_validEmails_returnNull() {
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "dev+test@company.io",
            "alex_99@qalbi.app"
        )
        for (email in validEmails) {
            assertNull("Expected '$email' to be valid", viewModel.validateEmail(email))
        }
    }

    @Test
    fun validateEmail_invalidEmails_returnsError() {
        val invalidEmails = listOf(
            "",
            "   ",
            "notanemail",
            "@domain.com",
            "user@",
            "user@domain",
            "user space@domain.com"
        )
        for (email in invalidEmails) {
            assertNotNull("Expected '$email' to be rejected", viewModel.validateEmail(email))
        }
    }

    // ─── Password Validation Tests ───────────────────────────────────────────

    @Test
    fun validatePassword_lengthEightOrMore_returnNull() {
        assertNull(viewModel.validatePassword("12345678"))
        assertNull(viewModel.validatePassword("strong_password_123"))
    }

    @Test
    fun validatePassword_underEightCharacters_returnsError() {
        assertNotNull(viewModel.validatePassword(""))
        assertNotNull(viewModel.validatePassword("1234567"))
        assertNotNull(viewModel.validatePassword("short"))
    }

    @Test
    fun validateConfirmPassword_matching_returnNull() {
        assertNull(viewModel.validateConfirmPassword("mysecret123", "mysecret123"))
    }

    @Test
    fun validateConfirmPassword_mismatchedOrBlank_returnsError() {
        assertNotNull(viewModel.validateConfirmPassword("mysecret123", "different123"))
        assertNotNull(viewModel.validateConfirmPassword("mysecret123", ""))
    }

    // ─── User Model Handling Tests ───────────────────────────────────────────

    @Test
    fun userModel_computesNormalizedUsernameAndAccountType() {
        val emailUser = User(
            id = "uid-123",
            displayName = "Alex Morningstar",
            username = "Alex_Dev",
            email = "alex@example.com"
        )
        assertEquals("alex_dev", emailUser.usernameNormalized)
        assertEquals(AccountType.EMAIL, emailUser.accountType)

        val usernameUser = User(
            id = "uid-456",
            displayName = "Sam Rivera",
            username = "Sam_99",
            email = null
        )
        assertEquals("sam_99", usernameUser.usernameNormalized)
        assertEquals(AccountType.USERNAME, usernameUser.accountType)
    }

    @Test
    fun userModel_toMapAndFromMap_preservesData() {
        val original = User(
            id = "uid-789",
            displayName = "Noor Fatima",
            username = "Noor_F",
            email = "noor@qalbi.app",
            accountType = AccountType.EMAIL,
            profileVersion = 1
        )

        val map = original.toMap()
        assertEquals("uid-789", map["uid"])
        assertEquals("Noor Fatima", map["displayName"])
        assertEquals("Noor_F", map["username"])
        assertEquals("noor_f", map["usernameNormalized"])
        assertEquals("noor@qalbi.app", map["email"])
        assertEquals("EMAIL", map["accountType"])

        val reconstructed = User.fromMap("uid-789", map)
        assertEquals(original.id, reconstructed.id)
        assertEquals(original.displayName, reconstructed.displayName)
        assertEquals(original.username, reconstructed.username)
        assertEquals(original.usernameNormalized, reconstructed.usernameNormalized)
        assertEquals(original.email, reconstructed.email)
        assertEquals(original.accountType, reconstructed.accountType)
    }
}
