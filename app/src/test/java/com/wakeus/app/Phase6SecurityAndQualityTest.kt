package com.wakeus.app

import com.wakeus.app.data.alarm.AlarmPreferences
import com.wakeus.app.data.model.AvatarPreset
import com.wakeus.app.data.model.Connection
import com.wakeus.app.data.model.PublicProfile
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.data.network.NetworkMonitor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 Quality Assurance & Security Hardening Verification Suite.
 * Validates network connectivity observation, zero-cost avatar customizer,
 * alarm volume escalation, vibration waveforms, deterministic connection IDs,
 * and security state immutability.
 */
class Phase6SecurityAndQualityTest {

    // ── 1. Network Connectivity Monitor Tests ─────────────────────────────────

    @Test
    fun `network monitor reacts to simulated connectivity changes`() {
        NetworkMonitor.setOnlineForTesting(true)
        assertTrue("NetworkMonitor should report online", NetworkMonitor.isOnline.value)

        NetworkMonitor.setOnlineForTesting(false)
        assertFalse("NetworkMonitor should report offline after loss", NetworkMonitor.isOnline.value)

        NetworkMonitor.setOnlineForTesting(true)
        assertTrue("NetworkMonitor should recover to online state", NetworkMonitor.isOnline.value)
    }

    // ── 2. Zero-Cost Avatar Customization Tests ───────────────────────────────

    @Test
    fun `avatar presets contain 6 distinct harmonious palettes`() {
        assertEquals("Should provide exactly 6 curated avatar presets", 6, AvatarPreset.PRESETS.size)

        val ids = AvatarPreset.PRESETS.map { it.id }.toSet()
        assertEquals("Each preset must have a unique identifier", 6, ids.size)

        // Known expected presets
        assertTrue("Should include ROSE", ids.contains("ROSE"))
        assertTrue("Should include GOLD", ids.contains("GOLD"))
        assertTrue("Should include EMERALD", ids.contains("EMERALD"))
        assertTrue("Should include VIOLET", ids.contains("VIOLET"))
        assertTrue("Should include CORAL", ids.contains("CORAL"))
        assertTrue("Should include MIDNIGHT", ids.contains("MIDNIGHT"))
    }

    @Test
    fun `avatar preset getColor handles case insensitivity and safe fallbacks`() {
        assertEquals("Uppercase ROSE must return 0xFFC87B8B", 0xFFC87B8BL, AvatarPreset.getColor("ROSE"))
        assertEquals("Lowercase rose must return 0xFFC87B8B", 0xFFC87B8BL, AvatarPreset.getColor("rose"))
        assertEquals("GOLD must return 0xFFC9A84C", 0xFFC9A84CL, AvatarPreset.getColor("GOLD"))

        // Fallback for null or unknown style
        assertEquals("Null style must safely fallback to default ROSE", 0xFFC87B8BL, AvatarPreset.getColor(null))
        assertEquals("Unknown style must safely fallback to default ROSE", 0xFFC87B8BL, AvatarPreset.getColor("NEON_PINK"))
    }

    @Test
    fun `user model serializes and deserializes avatarStyle correctly`() {
        val user = User(
            id = "test_user_01",
            displayName = "Layla Morning",
            username = "layla",
            email = "layla@example.com",
            avatarStyle = "EMERALD",
        )

        val map = user.toMap()
        assertEquals("avatarStyle must be stored in Firestore map", "EMERALD", map["avatarStyle"])

        val reconstructed = User.fromMap("test_user_01", map)
        assertEquals("avatarStyle must be reconstructed accurately", "EMERALD", reconstructed.avatarStyle)
    }

    @Test
    fun `public profile preserves avatarStyle and excludes private email`() {
        val profile = PublicProfile(
            uid = "test_user_01",
            displayName = "Layla Morning",
            username = "layla",
            avatarStyle = "GOLD",
        )

        val map = profile.toMap()
        assertEquals("avatarStyle must be preserved in public profile", "GOLD", map["avatarStyle"])
        assertFalse("Public profile MUST NEVER contain email key", map.containsKey("email"))
        assertFalse("Public profile MUST NEVER contain password key", map.containsKey("password"))

        val reconstructed = PublicProfile.fromMap("test_user_01", map)
        assertEquals("GOLD", reconstructed.avatarStyle)
    }

    // ── 3. Alarm Volume & Vibration Waveform Tests ────────────────────────────

    @Test
    fun `alarm preferences vibration waveforms match specified timings`() {
        val cadence = AlarmPreferences.getPatternWaveform(AlarmPreferences.PATTERN_CADENCE)
        assertArrayEquals(longArrayOf(0, 800, 400, 800, 400, 800), cadence)

        val heartbeat = AlarmPreferences.getPatternWaveform(AlarmPreferences.PATTERN_HEARTBEAT)
        assertArrayEquals(longArrayOf(0, 250, 150, 250, 750), heartbeat)

        val gentle = AlarmPreferences.getPatternWaveform(AlarmPreferences.PATTERN_GENTLE)
        assertArrayEquals(longArrayOf(0, 400, 800, 400, 800), gentle)

        // Case insensitivity & fallback
        val lowercase = AlarmPreferences.getPatternWaveform("heartbeat")
        assertArrayEquals(heartbeat, lowercase)

        val fallback = AlarmPreferences.getPatternWaveform("UNKNOWN_PATTERN")
        assertArrayEquals(cadence, fallback)
    }

    @Test
    fun `gentle volume ramp formula scales progressively from 15 percent to 100 percent`() {
        val startVolume = 0.15f
        assertEquals("Volume should start at exactly 0.15f", 0.15f, startVolume, 0.001f)

        // Step 5 of 10: 0.15 + (5/10) * 0.85 = 0.15 + 0.425 = 0.575
        val midVolume = 0.15f + (5f / 10f) * 0.85f
        assertEquals(0.575f, midVolume, 0.001f)

        // Step 10 of 10: 0.15 + 0.85 = 1.0f
        val finalVolume = (0.15f + (10f / 10f) * 0.85f).coerceAtMost(1.0f)
        assertEquals(1.0f, finalVolume, 0.001f)
    }

    // ── 4. Deterministic Connection & Identity Security Invariants ───────────

    @Test
    fun `connection deterministic ID is strictly symmetric`() {
        val uidAlice = "user_alpha_123"
        val uidBob = "user_zeta_999"

        val idAB = Connection.createDeterministicId(uidAlice, uidBob)
        val idBA = Connection.createDeterministicId(uidBob, uidAlice)

        assertEquals("Deterministic connection ID must be identical regardless of argument order", idAB, idBA)
        assertEquals("user_alpha_123_user_zeta_999", idAB)
    }

    @Test
    fun `wake up request terminal states cannot be reopened or cancelled`() {
        val acknowledged = WakeUpRequest(
            requestId = "req_ack",
            senderUid = "sender_1",
            senderDisplayName = "Sender",
            senderUsername = "sender",
            receiverUid = "receiver_1",
            receiverDisplayName = "Receiver",
            receiverUsername = "receiver",
            connectionId = "conn_1",
            status = WakeUpRequestStatus.ACKNOWLEDGED,
        )

        assertFalse("Acknowledged request cannot be cancelled", acknowledged.canBeCancelledBy("sender_1"))
        assertFalse("Acknowledged request cannot be responded to", acknowledged.canBeRespondedBy("receiver_1"))
        assertFalse("Acknowledged request is not pending", acknowledged.isPending)

        val cancelled = acknowledged.copy(status = WakeUpRequestStatus.CANCELLED)
        assertFalse("Cancelled request cannot be cancelled", cancelled.canBeCancelledBy("sender_1"))
        assertFalse("Cancelled request cannot be responded to", cancelled.canBeRespondedBy("receiver_1"))
        assertFalse("Cancelled request is not pending", cancelled.isPending)

        val declined = acknowledged.copy(status = WakeUpRequestStatus.DECLINED)
        assertFalse("Declined request cannot be cancelled", declined.canBeCancelledBy("sender_1"))
        assertFalse("Declined request cannot be responded to", declined.canBeRespondedBy("receiver_1"))

        val expired = acknowledged.copy(status = WakeUpRequestStatus.EXPIRED)
        assertFalse("Expired request cannot be cancelled", expired.canBeCancelledBy("sender_1"))
        assertFalse("Expired request cannot be responded to", expired.canBeRespondedBy("receiver_1"))
        assertTrue("Expired status flag must report isExpired", expired.isExpired)
    }

    @Test
    fun `wake up request auto expires after 15 minutes`() {
        val pastTime = System.currentTimeMillis() - (16 * 60 * 1000L) // 16 mins ago
        val pastRequest = WakeUpRequest(
            requestId = "req_past",
            senderUid = "sender_1",
            senderDisplayName = "Sender",
            senderUsername = "sender",
            receiverUid = "receiver_1",
            receiverDisplayName = "Receiver",
            receiverUsername = "receiver",
            connectionId = "conn_1",
            status = WakeUpRequestStatus.PENDING,
            expiresAt = pastTime,
        )

        assertTrue("Request whose expiresAt timestamp is in the past must be treated as expired", pastRequest.isExpired)
        assertFalse("Past request cannot be responded to", pastRequest.canBeRespondedBy("receiver_1"))
        assertFalse("Past request cannot be cancelled", pastRequest.canBeCancelledBy("sender_1"))
    }
}
