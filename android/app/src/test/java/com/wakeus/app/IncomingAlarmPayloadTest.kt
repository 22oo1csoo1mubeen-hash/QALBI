package com.wakeus.app

import com.wakeus.app.data.fcm.IncomingAlarmPayloadResult
import com.wakeus.app.data.fcm.IncomingAlarmPayloadValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingAlarmPayloadTest {
    private val validMessage = mapOf(
        "requestId" to "request-123",
        "type" to "WAKE_UP_REQUEST",
        "senderUid" to "sender-1",
        "senderDisplayName" to "Friend",
        "senderUsername" to "friend",
        "receiverUid" to "receiver-1",
        "expiresAt" to "2000000000000",
    )

    @Test
    fun `accepts a current request targeted at the signed in user`() {
        val result = IncomingAlarmPayloadValidator.validate(validMessage, "receiver-1", 1_999_999_999_000)
        assertTrue(result is IncomingAlarmPayloadResult.Valid)
        assertEquals("request-123", (result as IncomingAlarmPayloadResult.Valid).payload.requestId)
    }

    @Test
    fun `keeps the optional scheduled alarm message within the payload limit`() {
        val result = IncomingAlarmPayloadValidator.validate(
            validMessage + ("customMessage" to "Wake up, friend! ❤️"),
            "receiver-1",
            1_999_999_999_000,
        )
        assertTrue(result is IncomingAlarmPayloadResult.Valid)
        assertEquals("Wake up, friend! ❤️", (result as IncomingAlarmPayloadResult.Valid).payload.customMessage)

        val longMessageResult = IncomingAlarmPayloadValidator.validate(
            validMessage + ("customMessage" to "a".repeat(200)),
            "receiver-1",
            1_999_999_999_000,
        )
        assertTrue(longMessageResult is IncomingAlarmPayloadResult.Valid)
        assertEquals(160, (longMessageResult as IncomingAlarmPayloadResult.Valid).payload.customMessage.length)
    }

    @Test
    fun `rejects request addressed to a different user`() {
        val result = IncomingAlarmPayloadValidator.validate(validMessage, "other-user", 1_999_999_999_000)
        assertEquals("recipient_mismatch", (result as IncomingAlarmPayloadResult.Rejected).reason)
    }

    @Test
    fun `rejects expired request instead of extending its lifetime`() {
        val result = IncomingAlarmPayloadValidator.validate(validMessage, "receiver-1", 2_000_000_000_000)
        assertEquals("expired", (result as IncomingAlarmPayloadResult.Rejected).reason)
    }

    @Test
    fun `rejects payload with missing expiry`() {
        val result = IncomingAlarmPayloadValidator.validate(validMessage - "expiresAt", "receiver-1", 1_900_000_000_000)
        assertEquals("missing_or_invalid_expiry", (result as IncomingAlarmPayloadResult.Rejected).reason)
    }
}
