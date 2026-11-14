package com.wakeus.app

import com.wakeus.app.data.friends.FriendRepository
import com.wakeus.app.data.model.Connection
import com.wakeus.app.data.model.PairingCode
import com.wakeus.app.data.model.PairingCodeStatus
import com.wakeus.app.data.model.PublicProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendPairingTest {

    private val friendRepository = FriendRepository { null }

    @Test
    fun `test code generation produces 8-character codes from Crockford alphabet`() {
        val code = friendRepository.generateSecureCode()
        assertEquals(FriendRepository.CODE_LENGTH, code.length)
        assertTrue(code.all { it in FriendRepository.CODE_ALPHABET })

        // Verify ambiguous characters (0, 1, I, O) are completely excluded
        assertFalse(code.contains('0'))
        assertFalse(code.contains('1'))
        assertFalse(code.contains('I'))
        assertFalse(code.contains('O'))
    }

    @Test
    fun `test code generation produces unique codes`() {
        val codes = (1..50).map { friendRepository.generateSecureCode() }.toSet()
        assertEquals(50, codes.size)
    }

    @Test
    fun `test code normalization strips spaces, dashes, and converts to uppercase`() {
        val rawInput = " 7x9k-2m4p "
        val normalized = FriendRepository.normalizeCode(rawInput)
        assertEquals("7X9K2M4P", normalized)
    }

    @Test
    fun `test code normalization removes invalid characters`() {
        val input = "AB!@#CD 123"
        // '1' is excluded from alphabet, '2' and '3' are kept, '!@#' removed
        val normalized = FriendRepository.normalizeCode(input)
        assertEquals("ABCD23", normalized)
    }

    @Test
    fun `test pairing code expiry calculation`() {
        val now = System.currentTimeMillis()
        val activeCode = PairingCode(
            code = "7X9K2M4P",
            creatorUid = "user_123",
            creatorDisplayName = "Alice",
            creatorUsername = "alice",
            createdAt = now,
            expiresAt = now + 600_000L, // 10 minutes
        )
        assertFalse(activeCode.isExpired)
        assertTrue(activeCode.remainingMinutes >= 9)

        val expiredCode = PairingCode(
            code = "7X9K2M4P",
            creatorUid = "user_123",
            creatorDisplayName = "Alice",
            creatorUsername = "alice",
            createdAt = now - 700_000L,
            expiresAt = now - 100_000L,
        )
        assertTrue(expiredCode.isExpired)
        assertEquals(0L, expiredCode.remainingMinutes)
    }

    @Test
    fun `test deterministic connection ID is symmetric regardless of order`() {
        val uidAlice = "usr_alice_001"
        val uidBob = "usr_bob_002"

        val id1 = Connection.createDeterministicId(uidAlice, uidBob)
        val id2 = Connection.createDeterministicId(uidBob, uidAlice)

        assertEquals(id1, id2)
        assertEquals("${uidAlice}_${uidBob}", id1)
    }

    @Test
    fun `test connection getOtherUid resolves the correct counterpart`() {
        val uidAlice = "usr_alice"
        val uidBob = "usr_bob"
        val connection = Connection(
            connectionId = Connection.createDeterministicId(uidAlice, uidBob),
            memberUids = listOf(uidAlice, uidBob),
            user1Uid = uidAlice,
            user2Uid = uidBob,
            createdFromPairingCode = "ABCDEFGH",
        )

        assertEquals(uidBob, connection.getOtherUid(uidAlice))
        assertEquals(uidAlice, connection.getOtherUid(uidBob))
    }

    @Test
    fun `test public profile serialization excludes private fields`() {
        val profile = PublicProfile(
            uid = "usr_test_123",
            displayName = "Zayd Khan",
            username = "zayd_k",
        )
        val map = profile.toMap()

        assertEquals("usr_test_123", map["uid"])
        assertEquals("Zayd Khan", map["displayName"])
        assertEquals("zayd_k", map["username"])
        assertEquals("zayd_k", map["usernameNormalized"])
        assertFalse(map.containsKey("email"))
        assertFalse(map.containsKey("password"))

        val reconstructed = PublicProfile.fromMap("usr_test_123", map)
        assertEquals(profile.uid, reconstructed.uid)
        assertEquals(profile.displayName, reconstructed.displayName)
        assertEquals(profile.username, reconstructed.username)
        assertEquals(profile.usernameNormalized, reconstructed.usernameNormalized)
    }

    @Test
    fun `test pairing code serialization roundtrip`() {
        val original = PairingCode(
            code = "8K4M2P9X",
            creatorUid = "creator_uid_456",
            creatorDisplayName = "Fatima",
            creatorUsername = "fatima_m",
            status = PairingCodeStatus.ACTIVE,
        )
        val map = original.toMap()
        val reconstructed = PairingCode.fromMap("8K4M2P9X", map)

        assertEquals(original.code, reconstructed.code)
        assertEquals(original.creatorUid, reconstructed.creatorUid)
        assertEquals(original.creatorDisplayName, reconstructed.creatorDisplayName)
        assertEquals(original.creatorUsername, reconstructed.creatorUsername)
        assertEquals(original.status, reconstructed.status)
    }
}
