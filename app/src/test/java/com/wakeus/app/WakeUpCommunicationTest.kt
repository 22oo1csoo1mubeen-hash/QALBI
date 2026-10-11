package com.wakeus.app

import com.wakeus.app.data.model.AccountType
import com.wakeus.app.data.model.FcmDeviceToken
import com.wakeus.app.data.model.Friend
import com.wakeus.app.data.model.User
import com.wakeus.app.data.model.WakeUpRequest
import com.wakeus.app.data.model.WakeUpRequestStatus
import com.wakeus.app.data.wakeup.DuplicateWakeUpRequestException
import com.wakeus.app.data.wakeup.FriendBlockedException
import com.wakeus.app.data.wakeup.NoActiveConnectionException
import com.wakeus.app.data.wakeup.RequestAlreadyTerminalException
import com.wakeus.app.data.wakeup.RequestExpiredException
import com.wakeus.app.data.wakeup.SelfWakeUpException
import com.wakeus.app.data.wakeup.UnauthorizedRequestActionException
import com.wakeus.app.data.wakeup.WakeUpRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class WakeUpCommunicationTest {

    private val wakeUpRepository = WakeUpRepository { null }

    private val aliceUser = User(
        id = "user_alice_123",
        displayName = "Alice Smith",
        username = "alicesmith",
        email = "alice@example.com",
        accountType = AccountType.EMAIL,
    )

    private val bobFriend = Friend(
        id = "user_bob_456",
        displayName = "Bob Jones",
        username = "bobjones",
        connectionId = "conn_alice_bob",
    )

    @Test
    fun `test wake-up request rejection when attempting to send to oneself`() = runBlocking {
        val selfFriend = Friend(
            id = aliceUser.id,
            displayName = aliceUser.displayName,
            username = aliceUser.username,
        )

        // When unconfigured, repository still catches self wake-up before attempting Firestore
        val result = wakeUpRepository.sendWakeUpRequest(aliceUser, selfFriend)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SelfWakeUpException)
    }

    @Test
    fun `test wake-up request model creation and initial state`() {
        val now = System.currentTimeMillis()
        val request = WakeUpRequest(
            requestId = UUID.randomUUID().toString(),
            senderUid = aliceUser.id,
            senderDisplayName = aliceUser.displayName,
            senderUsername = aliceUser.username,
            receiverUid = bobFriend.id,
            receiverDisplayName = bobFriend.displayName,
            receiverUsername = bobFriend.username,
            connectionId = bobFriend.connectionId,
            status = WakeUpRequestStatus.PENDING,
            createdAt = now,
            expiresAt = now + 15 * 60 * 1000L,
        )

        assertTrue(request.isPending)
        assertFalse(request.isExpired)
        assertEquals(WakeUpRequestStatus.PENDING, request.status)
        assertTrue(request.canBeCancelledBy(aliceUser.id))
        assertFalse(request.canBeCancelledBy(bobFriend.id))
        assertTrue(request.canBeRespondedBy(bobFriend.id))
        assertFalse(request.canBeRespondedBy(aliceUser.id))
    }

    @Test
    fun `test request expiry computation`() {
        val past = System.currentTimeMillis() - 60000L // 1 minute ago
        val expiredRequest = WakeUpRequest(
            requestId = "req_expired_1",
            senderUid = aliceUser.id,
            senderDisplayName = aliceUser.displayName,
            senderUsername = aliceUser.username,
            receiverUid = bobFriend.id,
            receiverDisplayName = bobFriend.displayName,
            receiverUsername = bobFriend.username,
            connectionId = bobFriend.connectionId,
            status = WakeUpRequestStatus.PENDING,
            createdAt = past - 15 * 60 * 1000L,
            expiresAt = past, // expired in the past
        )

        assertTrue(expiredRequest.isExpired)
        assertFalse(expiredRequest.isPending)
        assertFalse(expiredRequest.canBeCancelledBy(aliceUser.id))
        assertFalse(expiredRequest.canBeRespondedBy(bobFriend.id))
    }

    @Test
    fun `test receiver acknowledgement authorization and lifecycle`() {
        val request = WakeUpRequest(
            requestId = "req_100",
            senderUid = aliceUser.id,
            senderDisplayName = aliceUser.displayName,
            senderUsername = aliceUser.username,
            receiverUid = bobFriend.id,
            receiverDisplayName = bobFriend.displayName,
            receiverUsername = bobFriend.username,
            connectionId = bobFriend.connectionId,
            status = WakeUpRequestStatus.PENDING,
        )

        // Only receiver can acknowledge
        assertTrue(request.canBeRespondedBy(bobFriend.id))
        assertFalse(request.canBeRespondedBy(aliceUser.id))
        assertFalse(request.canBeRespondedBy("random_intruder_uid"))

        val acknowledged = request.copy(
            status = WakeUpRequestStatus.ACKNOWLEDGED,
            acknowledgedAt = System.currentTimeMillis(),
        )

        assertEquals(WakeUpRequestStatus.ACKNOWLEDGED, acknowledged.status)
        assertFalse(acknowledged.isPending)
        assertFalse(acknowledged.canBeCancelledBy(aliceUser.id))
        assertFalse(acknowledged.canBeRespondedBy(bobFriend.id))
    }

    @Test
    fun `test receiver decline authorization and lifecycle`() {
        val request = WakeUpRequest(
            requestId = "req_101",
            senderUid = aliceUser.id,
            senderDisplayName = aliceUser.displayName,
            senderUsername = aliceUser.username,
            receiverUid = bobFriend.id,
            receiverDisplayName = bobFriend.displayName,
            receiverUsername = bobFriend.username,
            connectionId = bobFriend.connectionId,
            status = WakeUpRequestStatus.PENDING,
        )

        val declined = request.copy(
            status = WakeUpRequestStatus.DECLINED,
            declinedAt = System.currentTimeMillis(),
        )

        assertEquals(WakeUpRequestStatus.DECLINED, declined.status)
        assertFalse(declined.isPending)
        assertFalse(declined.canBeCancelledBy(aliceUser.id))
        assertFalse(declined.canBeRespondedBy(bobFriend.id))
    }

    @Test
    fun `test sender cancellation authorization and lifecycle`() {
        val request = WakeUpRequest(
            requestId = "req_102",
            senderUid = aliceUser.id,
            senderDisplayName = aliceUser.displayName,
            senderUsername = aliceUser.username,
            receiverUid = bobFriend.id,
            receiverDisplayName = bobFriend.displayName,
            receiverUsername = bobFriend.username,
            connectionId = bobFriend.connectionId,
            status = WakeUpRequestStatus.PENDING,
        )

        // Only sender can cancel
        assertTrue(request.canBeCancelledBy(aliceUser.id))
        assertFalse(request.canBeCancelledBy(bobFriend.id))
        assertFalse(request.canBeCancelledBy("unrelated_user"))

        val cancelled = request.copy(
            status = WakeUpRequestStatus.CANCELLED,
            cancelledAt = System.currentTimeMillis(),
        )

        assertEquals(WakeUpRequestStatus.CANCELLED, cancelled.status)
        assertFalse(cancelled.isPending)
        assertFalse(cancelled.canBeCancelledBy(aliceUser.id))
        assertFalse(cancelled.canBeRespondedBy(bobFriend.id))
    }

    @Test
    fun `test terminal state rejects any further state transitions`() {
        val terminalStates = listOf(
            WakeUpRequestStatus.ACKNOWLEDGED,
            WakeUpRequestStatus.DECLINED,
            WakeUpRequestStatus.CANCELLED,
            WakeUpRequestStatus.EXPIRED,
        )

        for (terminal in terminalStates) {
            val req = WakeUpRequest(
                requestId = "req_term",
                senderUid = aliceUser.id,
                senderDisplayName = aliceUser.displayName,
                senderUsername = aliceUser.username,
                receiverUid = bobFriend.id,
                receiverDisplayName = bobFriend.displayName,
                receiverUsername = bobFriend.username,
                connectionId = bobFriend.connectionId,
                status = terminal,
            )

            assertFalse("Terminal state $terminal must not allow cancellation", req.canBeCancelledBy(aliceUser.id))
            assertFalse("Terminal state $terminal must not allow response", req.canBeRespondedBy(bobFriend.id))
            assertFalse("Terminal state $terminal must not be pending", req.isPending)
        }
    }

    @Test
    fun `test fromMap automatically marks expired requests when expiresAt has elapsed`() {
        val pastTimestamp = System.currentTimeMillis() - 50000L
        val map = mapOf(
            "senderUid" to aliceUser.id,
            "senderDisplayName" to aliceUser.displayName,
            "senderUsername" to aliceUser.username,
            "receiverUid" to bobFriend.id,
            "receiverDisplayName" to bobFriend.displayName,
            "receiverUsername" to bobFriend.username,
            "connectionId" to "conn_test",
            "status" to "PENDING",
            "createdAt" to pastTimestamp - 100000L,
            "expiresAt" to pastTimestamp,
        )

        val deserialized = WakeUpRequest.fromMap("req_map_test", map)
        assertEquals(WakeUpRequestStatus.EXPIRED, deserialized.status)
        assertTrue(deserialized.isExpired)
    }

    @Test
    fun `test FCM device token model and serialization`() {
        val token = FcmDeviceToken(
            token = "fake_fcm_token_xyz_1234567890",
            deviceModel = "Pixel 8 Pro",
        )

        assertEquals("fake_fcm_token_xyz_1234567890", token.token)
        assertEquals("Pixel 8 Pro", token.deviceModel)
        val map = token.toMap()
        assertEquals("fake_fcm_token_xyz_1234567890", map["token"])
        assertEquals("Pixel 8 Pro", map["deviceModel"])
        assertNotNull(map["updatedAt"])
    }

    @Test
    fun `test exception messages accurately reflect business logic`() {
        val selfEx = SelfWakeUpException()
        assertTrue(selfEx.message!!.contains("yourself"))

        val noConnEx = NoActiveConnectionException()
        assertTrue(noConnEx.message!!.contains("connected"))

        val blockedEx = FriendBlockedException()
        assertTrue(blockedEx.message!!.contains("blocked"))

        val dupEx = DuplicateWakeUpRequestException()
        assertTrue(dupEx.message!!.contains("already pending"))

        val termEx = RequestAlreadyTerminalException("CANCELLED")
        assertTrue(termEx.message!!.contains("CANCELLED"))

        val unauthEx = UnauthorizedRequestActionException()
        assertTrue(unauthEx.message!!.contains("not authorized"))
    }
}
