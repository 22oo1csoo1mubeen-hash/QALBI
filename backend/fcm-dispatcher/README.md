# QALBI FCM dispatcher

This Node.js service watches Firestore wake-up requests and sends high-priority, data-only FCM messages through the Firebase Admin SDK. Its service-account key must stay server-side.

## Local use

1. Copy .env.example to .env.
2. Put the service-account JSON for the same Firebase project in this folder as serviceAccountKey.json.
3. From this folder, run:

    npm install
    npm start

The dispatcher loads .env for local development. Do not commit .env or serviceAccountKey.json.

## Render

Create a Web Service from the project repository with:

- Root directory: backend/fcm-dispatcher
- Build command: npm install
- Start command: npm start
- Instance type: Free (best-effort only)

Add the Firebase service-account JSON in Render as a Secret File named serviceAccountKey.json. Set GOOGLE_APPLICATION_CREDENTIALS to /etc/secrets/serviceAccountKey.json. Render sets PORT automatically; the service exposes GET and HEAD /health for health checks.

A free external uptime monitor that requests /health every five minutes can reduce idle spin-down. Render may still restart or suspend free services, and free hours are shared across the workspace. This does not provide guaranteed alarm delivery.

## Request flow

1. The Android client creates a PENDING document in Firestore at wakeUpRequests/{requestId}.
2. The dispatcher validates expiry, the active connection, and both block lists.
3. It loads receiver tokens from users/{receiverUid}/fcmTokens and sends data-only FCM.
4. The Android messaging service validates recipient and expiry, then starts the alarm foreground service.

The app's 15-minute expiry is checked against Firestore's 30-minute clock-skew allowance; FCM delivery itself is still capped at 15 minutes. This avoids rejecting otherwise valid requests when a phone clock differs slightly from the dispatcher.

ACCEPTED_BY_FCM means FCM accepted a send; it does not confirm that the device received or displayed the alarm. See the [alarm reliability report](../../docs/architecture/ALARM_RELIABILITY_REPORT.md) and [Moto G34 guide](../../docs/architecture/MOTO_G34_TEST_GUIDE.md).
