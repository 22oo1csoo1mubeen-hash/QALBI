package com.wakeus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRecoveryTest {

    private val developerEmail = "22oo1csoo1mubeen@gmail.com"

    @Test
    fun testDeveloperEmailConstant() {
        assertEquals("22oo1csoo1mubeen@gmail.com", developerEmail)
    }

    @Test
    fun testEmailFormatValidation() {
        fun isValidEmail(email: String): Boolean {
            val clean = email.trim()
            return clean.isNotBlank() && clean.contains("@") && clean.contains(".")
        }

        assertTrue(isValidEmail("user@example.com"))
        assertTrue(isValidEmail("syed.mubeen@gmail.com"))
        assertFalse(isValidEmail("syed001"))
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("user@localhost"))
    }

    @Test
    fun testUsernameOnlyAccountClassification() {
        fun isUsernameOnlyInput(input: String): Boolean {
            return !input.trim().contains("@")
        }

        assertTrue(isUsernameOnlyInput("syed001"))
        assertTrue(isUsernameOnlyInput("Mubeen_01"))
        assertFalse(isUsernameOnlyInput("syed@gmail.com"))
    }

    @Test
    fun testPasswordChangeValidation() {
        fun validatePasswordChange(username: String, currentPass: String, newPass: String, confirmPass: String): String? {
            if (username.isBlank()) return "Username required"
            if (currentPass.isBlank()) return "Current password required"
            if (newPass.length < 6) return "New password must be at least 6 characters"
            if (newPass != confirmPass) return "Passwords do not match"
            return null
        }

        assertEquals(null, validatePasswordChange("syed001", "oldPass123", "newPass456", "newPass456"))
        assertEquals("Username required", validatePasswordChange("", "oldPass123", "newPass456", "newPass456"))
        assertEquals("Current password required", validatePasswordChange("syed001", "", "newPass456", "newPass456"))
        assertEquals("New password must be at least 6 characters", validatePasswordChange("syed001", "oldPass123", "123", "123"))
        assertEquals("Passwords do not match", validatePasswordChange("syed001", "oldPass123", "newPass456", "mismatch"))
    }

    @Test
    fun testCodeExtractionFromRawCodeAndFullLink() {
        val authRepo = com.wakeus.app.data.auth.AuthRepository()

        // Raw 6-digit code or action code
        assertEquals("123456", authRepo.extractResetCode("123456"))
        assertEquals("abcXYZcode", authRepo.extractResetCode("  abcXYZcode  "))

        // Firebase password reset URL with oobCode parameter
        val url = "https://qalbi-8414f.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=Kk7d9L1P0q&apiKey=AIzaSyFakeKey"
        assertEquals("Kk7d9L1P0q", authRepo.extractResetCode(url))

        // Query string in middle of parameters
        val url2 = "https://example.com/reset?lang=en&oobCode=SecretCode123&other=true"
        assertEquals("SecretCode123", authRepo.extractResetCode(url2))
    }

    @Test
    fun testEmailMasking() {
        val authRepo = com.wakeus.app.data.auth.AuthRepository()

        assertEquals("s***n@gmail.com", authRepo.maskEmail("syed.mubeen@gmail.com"))
        assertEquals("m***a@gmail.com", authRepo.maskEmail("madiha@gmail.com"))
        assertEquals("a*@domain.com", authRepo.maskEmail("ab@domain.com"))
    }

    @Test
    fun testInAppPasswordResetValidation() {
        fun validateReset(code: String, newPass: String, confirmPass: String): String? {
            if (code.isBlank()) return "Reset code required"
            if (newPass.length < 8) return "Password must be at least 8 characters"
            if (newPass != confirmPass) return "Passwords do not match"
            return null
        }

        assertEquals(null, validateReset("123456", "strongPass123", "strongPass123"))
        assertEquals("Reset code required", validateReset("", "strongPass123", "strongPass123"))
        assertEquals("Password must be at least 8 characters", validateReset("123456", "short", "short"))
        assertEquals("Passwords do not match", validateReset("123456", "strongPass123", "different123"))
    }
}
