package com.wakeus.app.data.fcm

data class IncomingAlarmPayload(
    val requestId: String,
    val senderUid: String,
    val senderDisplayName: String,
    val senderUsername: String,
    val expiresAt: Long,
    val customMessage: String = "",
)

sealed class IncomingAlarmPayloadResult {
    data class Valid(val payload: IncomingAlarmPayload) : IncomingAlarmPayloadResult()
    data class Rejected(val reason: String) : IncomingAlarmPayloadResult()
}

object IncomingAlarmPayloadValidator {
    fun validate(data: Map<String, String>, authenticatedUid: String?, nowMillis: Long): IncomingAlarmPayloadResult {
        val requestId = data["requestId"]?.takeIf(String::isNotBlank)
            ?: return IncomingAlarmPayloadResult.Rejected("missing_request_id")
        if (data["type"] != "WAKE_UP_REQUEST") return IncomingAlarmPayloadResult.Rejected("unexpected_type")
        val receiverUid = data["receiverUid"]?.takeIf(String::isNotBlank)
            ?: return IncomingAlarmPayloadResult.Rejected("missing_receiver")
        if (authenticatedUid.isNullOrBlank() || receiverUid != authenticatedUid) {
            return IncomingAlarmPayloadResult.Rejected("recipient_mismatch")
        }
        val senderUid = data["senderUid"]?.takeIf(String::isNotBlank)
            ?: return IncomingAlarmPayloadResult.Rejected("missing_sender")
        val expiresAt = data["expiresAt"]?.toLongOrNull()
            ?: return IncomingAlarmPayloadResult.Rejected("missing_or_invalid_expiry")
        if (nowMillis >= expiresAt) return IncomingAlarmPayloadResult.Rejected("expired")

        return IncomingAlarmPayloadResult.Valid(
            IncomingAlarmPayload(
                requestId = requestId,
                senderUid = senderUid,
                senderDisplayName = data["senderDisplayName"]?.takeIf(String::isNotBlank) ?: "Your friend",
                senderUsername = data["senderUsername"]?.takeIf(String::isNotBlank) ?: "friend",
                expiresAt = expiresAt,
                customMessage = data["customMessage"]?.take(160).orEmpty(),
            ),
        )
    }
}
