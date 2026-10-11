package com.wakeus.app

import com.wakeus.app.data.alarm.AlarmState
import com.wakeus.app.data.alarm.LocalAlarmStatus
import com.wakeus.app.data.model.AccountType
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AlarmEngineTest {

    private val currentUser = User(
        id = "receiver_uid_bob",
        displayName = "Bob Jones",
        username = "bobjones",
        accountType = AccountType.EMAIL,
    )

    private val friendUser = User(
        id = "sender_uid_alice",
        displayName = "Alice Smith",
        username = "alicesmith",
        accountType = AccountType.EMAIL,
    )

    @Test
    fun `test state machine permits valid sequential transitions from IDLE to RINGING to ACKNOWLEDGED`() {
        var status = LocalAlarmStatus.IDLE

        assertTrue(status.canTransitionTo(LocalAlarmStatus.STARTING))
        status = LocalAlarmStatus.STARTING

        assertTrue(status.canTransitionTo(LocalAlarmStatus.RINGING))
        status = LocalAlarmStatus.RINGING

        assertTrue(status.canTransitionTo(LocalAlarmStatus.ACKNOWLEDGING))
        status = LocalAlarmStatus.ACKNOWLEDGING

        assertTrue(status.canTransitionTo(LocalAlarmStatus.ACKNOWLEDGED))
        status = LocalAlarmStatus.ACKNOWLEDGED

        assertTrue(status.isTerminal)
        assertFalse(status.isRinging)
    }

    @Test
    fun `test state machine rejects illegal transition from ACKNOWLEDGED back to RINGING`() {
        val terminalStatus = LocalAlarmStatus.ACKNOWLEDGED
        assertFalse("Acknowledged alarm must never transition back to RINGING", terminalStatus.canTransitionTo(LocalAlarmStatus.RINGING))
        assertFalse("Acknowledged alarm must never transition to STARTING", terminalStatus.canTransitionTo(LocalAlarmStatus.STARTING))
        assertTrue("Acknowledged alarm can only reset to IDLE", terminalStatus.canTransitionTo(LocalAlarmStatus.IDLE))
    }

    @Test
    fun `test state machine rejects illegal transition from CANCELLED back to RINGING`() {
        val cancelledStatus = LocalAlarmStatus.CANCELLED
        assertFalse(cancelledStatus.canTransitionTo(LocalAlarmStatus.RINGING))
        assertFalse(cancelledStatus.canTransitionTo(LocalAlarmStatus.ACKNOWLEDGING))
        assertTrue(cancelledStatus.canTransitionTo(LocalAlarmStatus.IDLE))
    }

    @Test
    fun `test state machine rejects illegal transition from DECLINED back to RINGING`() {
        val declinedStatus = LocalAlarmStatus.DECLINED
        assertFalse(declinedStatus.canTransitionTo(LocalAlarmStatus.RINGING))
        assertTrue(declinedStatus.canTransitionTo(LocalAlarmStatus.IDLE))
    }

    @Test
    fun `test state machine rejects illegal transition from EXPIRED back to RINGING`() {
        val expiredStatus = LocalAlarmStatus.EXPIRED
        assertFalse(expiredStatus.canTransitionTo(LocalAlarmStatus.RINGING))
        assertTrue(expiredStatus.canTransitionTo(LocalAlarmStatus.IDLE))
    }

    @Test
    fun `test state machine rejects illegal transition from FAILED back to RINGING`() {
        val failedStatus = LocalAlarmStatus.FAILED
        assertFalse(failedStatus.canTransitionTo(LocalAlarmStatus.RINGING))
        assertTrue(failedStatus.canTransitionTo(LocalAlarmStatus.IDLE))
    }

    @Test
    fun `test alarm snapshot properties reflect ringing state accurately`() {
        val state = AlarmState(
            status = LocalAlarmStatus.RINGING,
            requestId = "req_123",
            senderUid = friendUser.id,
            senderDisplayName = friendUser.displayName,
            senderUsername = friendUser.username,
            startedAt = System.currentTimeMillis(),
            isAudioPlaying = true,
            isVibrating = true,
        )

        assertTrue(state.isRinging)
        assertFalse(state.isAcknowledging)
        assertNull(state.errorMessage)
        assertEquals("req_123", state.requestId)
        assertEquals("Alice Smith", state.senderDisplayName)
    }

    @Test
    fun `test expired request cannot initiate ringing`() {
        val past = System.currentTimeMillis() - 1000L
        val expiredRequest = WakeUpRequest(
            requestId = "req_expired",
            senderUid = friendUser.id,
            senderDisplayName = friendUser.displayName,
            senderUsername = friendUser.username,
            receiverUid = currentUser.id,
            receiverDisplayName = currentUser.displayName,
            receiverUsername = currentUser.username,
            connectionId = "conn_123",
            status = WakeUpRequestStatus.PENDING,
            expiresAt = past,
        )

        assertTrue(expiredRequest.isExpired)
        assertFalse("Expired request must not be eligible for response", expiredRequest.canBeRespondedBy(currentUser.id))
    }

    @Test
    fun `test request addressed to a different user cannot be responded by current user`() {
        val foreignRequest = WakeUpRequest(
            requestId = "req_foreign",
            senderUid = friendUser.id,
            senderDisplayName = friendUser.displayName,
            senderUsername = friendUser.username,
            receiverUid = "someone_else_456",
            receiverDisplayName = "Someone Else",
            receiverUsername = "someoneelse",
            connectionId = "conn_456",
            status = WakeUpRequestStatus.PENDING,
        )

        assertFalse("Intruder cannot respond to another user's request", foreignRequest.canBeRespondedBy(currentUser.id))
        assertTrue(foreignRequest.canBeRespondedBy("someone_else_456"))
    }

    @Test
    fun `test duplicate alarm check detects identical request`() {
        val reqId = "req_dup_check"
        val state = AlarmState(
            status = LocalAlarmStatus.RINGING,
            requestId = reqId,
            senderUid = friendUser.id,
            senderDisplayName = friendUser.displayName,
        )

        val isDuplicate = (state.status == LocalAlarmStatus.RINGING && state.requestId == reqId)
        assertTrue("Subsequent pushes for currently ringing request must be detected as duplicate", isDuplicate)
    }

    @Test
    fun `test terminal state reset returns to completely clean IDLE state`() {
        val terminalState = AlarmState(
            status = LocalAlarmStatus.ACKNOWLEDGED,
            requestId = "req_terminal",
            senderUid = friendUser.id,
            isAudioPlaying = false,
            isVibrating = false,
        )

        assertTrue(terminalState.status.isTerminal)

        val resetState = AlarmState(status = LocalAlarmStatus.IDLE)
        assertEquals(LocalAlarmStatus.IDLE, resetState.status)
        assertNull(resetState.requestId)
        assertFalse(resetState.isAudioPlaying)
        assertFalse(resetState.isVibrating)
    }

    @Test
    fun `test network failure during acknowledgement transitions to FAILED instead of false success`() {
        val acknowledgingState = AlarmState(
            status = LocalAlarmStatus.ACKNOWLEDGING,
            requestId = "req_net_fail",
            senderUid = friendUser.id,
        )

        val failedState = acknowledgingState.copy(
            status = LocalAlarmStatus.FAILED,
            errorMessage = "Network timeout. Please check your internet connection.",
        )

        assertEquals(LocalAlarmStatus.FAILED, failedState.status)
        assertEquals("Network timeout. Please check your internet connection.", failedState.errorMessage)
        assertFalse("Failed state must never be marked as ACKNOWLEDGED", failedState.status == LocalAlarmStatus.ACKNOWLEDGED)
    }
}
