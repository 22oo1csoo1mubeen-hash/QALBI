# QALBI Alarm Reliability Investigation

**Status:** Code-level changes implemented; Moto G34 root cause and extended-idle behavior still require device evidence.

## Architecture traced

QALBI is a Kotlin/Jetpack Compose single-Activity app organized around ViewModels and repositories. Firebase Authentication owns sessions; Firestore stores users, pairing, connections, and wake-up request state. The sender writes a `PENDING` request to `/wakeUpRequests/{requestId}`. A trusted Node/Firebase Admin daemon watches that collection, verifies the connection and block state, finds the receiver's tokens under `/users/{uid}/fcmTokens`, and submits FCM. On the receiving phone, `QalbiMessagingService` validates the recipient and expiry, `WakeUpAlarmManager` starts playback and the alarm foreground service, and the service posts the full-screen alarm notification. Acknowledgement/decline updates the request in Firestore and stops playback.

## Findings and root-cause confidence

The exact Moto G34 failure is **not confirmed**. The device is not attached to this environment and the Android version, build, app-op state, battery settings, and logs are not available. Code review found multiple real reliability faults that could explain why the app appears to recover after launch:

1. The old dispatcher sent both a notification and data payload. For a background app, Android may display the notification itself without invoking `onMessageReceived()`. That bypassed QALBI's custom alarm startup path and its app-controlled expiry/validation.
2. A separate continuous Firestore-listener foreground service attempted to keep a live listener and partial wake lock indefinitely, and tried to restart itself after the task was swiped away. It duplicated FCM and alarm handling, depended on long-lived background execution, and was not a sound recovery strategy.
3. The dispatcher kept an in-memory one-shot guard. When there were no registered tokens, the request was not marked dispatched but remained in the guard, so a later token registration did not cause a retry until process restart. Other failure branches also lacked bounded retry and request-expiry enforcement.
4. Token registration failures were ignored. `onNewToken()` did not use the token argument, and failed Firestore uploads had no durable retry.
5. FCM messages had no expiry field/TTL; the receiver invented a fresh 15-minute validity window when the field was missing.

These are confirmed source-code defects, but they do not prove which one caused the Moto G34 report. Battery restriction, FCM token state, network policy, force-stop, notification settings, and Motorola software behavior still need physical-device observations.

## Changes made

- Wake-up messages are now high-priority **data-only** FCM messages, so `QalbiMessagingService` owns validation and creates the visible alarm notification. No notification payload is sent to create a competing Android-managed notification path.
- The trusted dispatcher verifies the active connection and both block lists before sending, enforces the Firestore `expiresAt`, sets Android FCM TTL to the remaining lifetime, records acceptance/error metadata, prunes invalid tokens, and retries transient/no-token failures with bounded exponential delays. `ACCEPTED_BY_FCM` explicitly means FCM accepted the send; it does not mean the phone displayed or played an alarm.
- The receiver rejects wrong-user, malformed, and expired payloads. Missing expiry is rejected rather than extended.
- `onNewToken()` uploads the provided token. Authenticated startup uploads the current token; failures are recorded and retried with network-constrained WorkManager. WorkManager is used only for deferrable token maintenance, never for real-time alarm delivery.
- Logout now attempts to remove the current device token before Firebase Authentication signs out, avoiding the previous fire-and-forget race.
- Removed startup of the always-on Firestore listener and its service/boot receiver declarations, plus the automatic battery-optimization exemption prompt. FCM remains the remote delivery route; the alarm media-playback service runs only while an alarm is active.
- Firestore rules now limit request reads to participants, enforce active connection/non-blocked creation, constrain status transitions, and prevent arbitrary deletion. Connection creation requires atomic redemption of an unexpired pairing code; connection reads are member-only and private user documents are owner-readable. The dispatcher independently verifies the live connection and block lists before sending.
- Settings now includes **Background Alarm Reliability** checks and settings shortcuts, Motorola guidance, force-stop/power-off limitations, recent local event timestamps, and a shareable redacted report. The report never includes FCM tokens, auth credentials, passwords, or private message contents.

## What can be verified in the app

The diagnostic checks Android notification permission, the alarm channel, full-screen-intent access where the platform exposes it, battery-optimization exemption state, Android's background-restriction signal, Data Saver state, recorded FCM token-upload status, and the declared alarm service. A successful Firestore token upload is local evidence of the last registration attempt, not live proof that the backend still has that record. Settings cannot prove actual FCM delivery; the device event log shows message receipt only after Android invokes the service.

Motorola menu labels vary by Android and My UX release. The app shows general App battery usage/background-use and background-data guidance and leaves settings changes to the user.

## Testing evidence and remaining work

- `testDebugUnitTest`: **passed**, including the new tests for valid, wrong-recipient, expired, and malformed FCM payloads.
- `assembleDebug`: **passed**. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
- Dispatcher `node --check` and `npm test`: **passed** (3 policy tests for timestamps and TTL bounds).
- Gradle emitted an Android SDK XML version warning, but the tests and APK build completed successfully.
- Firestore rules were reviewed and updated, but no Firestore Emulator is configured here, so rule behavior has not been emulator-tested or published to the Firebase project. Restart the trusted dispatcher after deploying its code update.
- `adb` is unavailable here. No Moto G34 model/build/SDK, battery state, notification/full-screen state, or logcat evidence was collected.
- Two-device delivery, locked-screen presentation, removed-from-recents, Wi-Fi/mobile transitions, reboot, Battery Saver, default vs unrestricted battery settings, and multi-day idle still require the physical-device procedure in [MOTO_G34_TEST_GUIDE.md](MOTO_G34_TEST_GUIDE.md).

## Android limits

QALBI cannot receive internet messages while the phone is powered off. Android force-stop suppresses app background work and push handling until the user launches the app again. FCM high priority is intended for time-sensitive, user-visible events and is not an absolute delivery guarantee; manufacturer power policies, connectivity, notification settings, and FCM service behavior can still affect alarms. Full-screen presentation is subject to Android's permission and notification-channel rules. See [Android full-screen intent behavior](https://developer.android.com/about/versions/14/behavior-changes-14) and [Firebase Android message priority](https://firebase.google.com/docs/cloud-messaging/android-message-priority).





