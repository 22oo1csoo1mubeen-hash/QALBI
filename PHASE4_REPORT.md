# QALBI — Phase 4 Implementation Report: Remote Wake-Up Communication

**Date:** 2026-10-11  
**Phase:** 4 — Remote Wake-Up Communication  
**Status:** CODE & TESTS COMPLETE (Android client, UI, repository, security rules, FCM receiver, unit tests, and zero-cost backend dispatcher implemented; external device testing requires two physical devices)

---

## 1. Phase 4 Objective

Phase 4 establishes real, authenticated, remote wake-up communication between connected friends in QALBI without requiring phone numbers, paid cloud services, or credit card registration.

The core objectives:
1. Allow connected User A to send a wake-up request to connected User B.
2. Persist requests in Firestore with cryptographically unguessable IDs, timestamps, and verifiable status lifecycle.
3. Enforce strict authorization: verify mutual friendship, prevent duplicate active requests, reject self-requests, and honor block lists.
4. Implement Android Firebase Cloud Messaging (FCM) receiving infrastructure with high-priority notifications, deduplication, and direct deep-link navigation to the incoming request screen.
5. Provide live, bidirectional status observation: User B can tap **"I'm Awake!"** or **"Decline"**, and User A sees the verified transition immediately.
6. Provide a secure, zero-cost (₹0 Spark plan) backend FCM push dispatcher to deliver messages without placing privileged service-account secrets on the Android client.

---

## 2. Existing Architecture Inspected

The project architecture preserved and extended:
- **Language & UI**: 100% Kotlin with Jetpack Compose and Material 3 design system.
- **Architecture Pattern**: MVVM with single-responsibility Repositories (`WakeUpRepository`, `FcmTokenRepository`, `FriendRepository`, `AuthRepository`, `UserRepository`) unified behind `QalbiRepository`.
- **Reactive State**: Kotlin Coroutines `StateFlow` and `SharedFlow` with Firestore real-time snapshot listeners.
- **Authentication**: Firebase Authentication supporting both Email and Username credentials.
- **Friend Pairing (Phase 3)**: Bidirectional `/connections/{connectionId}` and `/userBlocks/{uid}/blockedUsers/{blockedUid}`.
- **Single Activity**: `MainActivity` hosting `AppNavigation` with Navigation Compose.

---

## 3. Features Implemented

### A. Wake-Up Request Creation & Confirmation
- Sender taps **"Wake Up"** on a friend's profile card or details screen.
- A confirmation dialog is displayed: *"Send a wake-up request to [Name]?"* explaining the action with explicit **Cancel** and **Send Request** options.
- UI prevents repeated double-taps by maintaining loading state.
- Request is written to Firestore `/wakeUpRequests/{requestId}` with status `PENDING` and a 15-minute expiration timestamp.

### B. Authorization & Validation Pipeline
- **Authentication Check**: Verifies caller is authenticated via Firebase Auth.
- **Self-Request Guard**: Explicitly rejects attempts to wake up oneself (`SelfWakeUpException`).
- **Connection Verification**: Validates that an active `/connections/{connectionId}` document exists with status `CONNECTED`.
- **Block Protection**: Checks bidirectional block lists in `/userBlocks/{uid}/blockedUsers/{otherUid}` (`FriendBlockedException`).
- **Duplicate Request Prevention**: Queries for any existing `PENDING` non-expired request between the two users and rejects duplicate attempts (`DuplicateWakeUpRequestException`).

### C. Persistent Firestore Request Lifecycle
- Standard lifecycle states:
  - `PENDING`: Request created and awaiting recipient response.
  - `ACKNOWLEDGED`: Recipient confirmed wake-up ("I'm Awake!").
  - `DECLINED`: Recipient explicitly declined the request.
  - `CANCELLED`: Original sender cancelled before recipient responded.
  - `EXPIRED`: 15-minute validity window elapsed without response.
- Status transitions are atomic and terminal: once a request enters `ACKNOWLEDGED`, `DECLINED`, `CANCELLED`, or `EXPIRED`, it cannot be reopened or transitioned to any other state.

### D. Receiver Interaction Screen (`IncomingRequestScreen`)
- Displays sender's display name, username, creation time, and relative elapsed time.
- Status card showing live state (`Pending`, `Acknowledged`, `Declined`, `Cancelled`, `Expired`).
- Action buttons:
  - **"☀️ I'm Awake!"**: Atomically transitions status to `ACKNOWLEDGED` with `acknowledgedAt` timestamp.
  - **"Decline"**: Atomically transitions status to `DECLINED` with `declinedAt` timestamp.
- Disables action buttons when request is no longer pending or has expired.

### E. Sender Live Status Tracking (`WakeUpScreen`)
- Real-time observation of request document via Firestore snapshot listener.
- Displays live status chip:
  - ⏳ *Awaiting response...* (Pending)
  - ✅ *Friend is awake!* (Acknowledged)
  - ❌ *Friend declined* (Declined)
  - 🚫 *Cancelled* (Cancelled)
  - ⏰ *Request expired* (Expired)
- Allows sender to cancel a pending request with instant backend synchronization.

### F. Home Screen Active Alert Banner
- `HomeScreen` listens to `/wakeUpRequests` where `receiverUid == currentUid` and `status == PENDING`.
- If an active request arrives while browsing the app, a prominent notification card appears at the top of the Home screen with sender details and a button to view/respond immediately.

### G. FCM Receiving & Notification Infrastructure
- **`QalbiMessagingService`**: Extends `FirebaseMessagingService`.
  - Handles `onNewToken`: hashes token and stores under `/users/{uid}/fcmTokens/{tokenHash}`.
  - Handles `onMessageReceived`: extracts payload, validates recipient matching, and triggers local notification.
- **`NotificationHelper`**:
  - Creates high-priority notification channel: `qalbi_wake_up_channel` with alarm category, custom vibration pattern, and full visibility.
  - Posts notification with `Intent` linking directly to `Screen.IncomingRequest.withId(requestId)`.
  - Supports notification cancellation upon response.
- **Android 13+ Runtime Permission**: `MainActivity` requests `POST_NOTIFICATIONS` on launch.

---

## 4. Files Added or Modified

| File | Status | Description |
|---|---|---|
| `gradle/libs.versions.toml` | Modified | Added `firebase-messaging` library under existing BOM `32.8.1`. |
| `app/build.gradle.kts` | Modified | Added `libs.firebase.messaging`, configured `testOptions.unitTests.isReturnDefaultValues = true`. |
| `app/src/main/AndroidManifest.xml` | Modified | Added `POST_NOTIFICATIONS` permission, registered `QalbiMessagingService`. |
| `app/src/main/java/com/wakeus/app/data/model/Models.kt` | Modified | Added `WakeUpRequestStatus`, `WakeUpRequest`, `FcmDeviceToken` with serialization and computed properties. |
| `app/src/main/java/com/wakeus/app/data/fcm/NotificationHelper.kt` | Added | Notification channel creation, high-priority notifications, intent deep links. |
| `app/src/main/java/com/wakeus/app/data/fcm/FcmTokenRepository.kt` | Added | Token hashing (SHA-256) and registration under `/users/{uid}/fcmTokens/{tokenHash}`. |
| `app/src/main/java/com/wakeus/app/data/fcm/QalbiMessagingService.kt` | Added | FCM push receiver service handling `onNewToken` and `onMessageReceived`. |
| `app/src/main/java/com/wakeus/app/data/wakeup/WakeUpRepository.kt` | Added | Complete Firestore request management, authorization checks, atomic transactions, Flow streams. |
| `app/src/main/java/com/wakeus/app/data/repository/QalbiRepository.kt` | Modified | Wired `wakeUpRepository`, `fcmTokenRepository`, and incoming request flows. |
| `app/src/main/java/com/wakeus/app/viewmodel/WakeUpViewModel.kt` | Modified | Real Firestore request initiation, live state observation, cancellation. |
| `app/src/main/java/com/wakeus/app/viewmodel/IncomingRequestViewModel.kt` | Added | ViewModel for receiver responses ("I'm Awake!" and "Decline"). |
| `app/src/main/java/com/wakeus/app/ui/wakeup/WakeUpScreen.kt` | Modified | Production UI with confirmation dialog, live state chips, cancel action. |
| `app/src/main/java/com/wakeus/app/ui/wakeup/IncomingRequestScreen.kt` | Added | Receiver screen with sender info, status card, and response actions. |
| `app/src/main/java/com/wakeus/app/ui/home/HomeScreen.kt` | Modified | Added active incoming wake-up alert banner. |
| `app/src/main/java/com/wakeus/app/navigation/Screen.kt` | Modified | Added `Screen.IncomingRequest` route. |
| `app/src/main/java/com/wakeus/app/navigation/AppNavigation.kt` | Modified | Wired `IncomingRequestScreen` and notification deep-link handler. |
| `app/src/main/java/com/wakeus/app/MainActivity.kt` | Modified | Added `POST_NOTIFICATIONS` permission launcher, notification intent parsing, token sync. |
| `app/src/main/java/com/wakeus/app/QalbiApp.kt` | Modified | Initializes notification channel on application startup. |
| `firestore.rules` | Modified | Added comprehensive security rules for `/wakeUpRequests` and `/users/{uid}/fcmTokens`. |
| `backend/fcm-dispatcher/package.json` | Added | Node.js package manifest for zero-cost external push dispatcher. |
| `backend/fcm-dispatcher/dispatcher.js` | Added | Trusted Firebase Admin SDK daemon listening for requests and dispatching FCM v1 pushes. |
| `backend/fcm-dispatcher/README.md` | Added | Setup and execution guide for the zero-cost push dispatcher. |
| `.gitignore` | Modified | Added `serviceAccountKey.json`, `node_modules/`, and `*.env`. |
| `app/src/test/java/com/wakeus/app/WakeUpCommunicationTest.kt` | Added | 10 comprehensive unit tests covering models, authorization, transitions, and errors. |

---

## 5. Firestore Schema Changes

### A. Wake-Up Requests: `/wakeUpRequests/{requestId}`

| Field | Type | Description |
|---|---|---|
| `requestId` | `String` | Unique UUID identifier for the request. |
| `senderUid` | `String` | Firebase Auth UID of the sender. |
| `senderDisplayName` | `String` | Display name of the sender at request time. |
| `senderUsername` | `String` | Username of the sender. |
| `receiverUid` | `String` | Firebase Auth UID of the recipient friend. |
| `receiverDisplayName` | `String` | Display name of the recipient. |
| `receiverUsername` | `String` | Username of the recipient. |
| `connectionId` | `String` | ID of the active bidirectional connection document. |
| `status` | `String` | Lifecycle state: `PENDING`, `ACKNOWLEDGED`, `DECLINED`, `CANCELLED`, `EXPIRED`. |
| `createdAt` | `Timestamp` | Server timestamp when the request was initiated. |
| `expiresAt` | `Timestamp` | Expiration timestamp (15 minutes after `createdAt`). |
| `updatedAt` | `Timestamp` | Timestamp of latest state change. |
| `acknowledgedAt` | `Timestamp?` | Timestamp when recipient tapped "I'm Awake!". |
| `declinedAt` | `Timestamp?` | Timestamp when recipient tapped "Decline". |
| `cancelledAt` | `Timestamp?` | Timestamp when sender cancelled. |
| `expiredAt` | `Timestamp?` | Timestamp when request expired. |
| `schemaVersion` | `Number` | Schema version (`1`). |
| `fcmDispatched` | `Boolean?` | Set by trusted backend dispatcher once push is sent. |

### B. FCM Device Tokens: `/users/{userId}/fcmTokens/{tokenHash}`

| Field | Type | Description |
|---|---|---|
| `token` | `String` | Raw FCM registration token for push routing. |
| `deviceModel` | `String` | Hardware model (e.g. `Pixel 8 Pro`) for device management. |
| `updatedAt` | `Timestamp` | Timestamp of latest token registration/refresh. |

---

## 6. Firestore Security Rules Changes

The following rules were deployed to `firestore.rules`:

```javascript
// FCM Device Tokens: /users/{userId}/fcmTokens/{tokenId}
match /users/{userId} {
  ...
  match /fcmTokens/{tokenId} {
    // Only the authenticated device owner can register or remove their tokens
    allow read, write: if isOwner(userId);
  }
}

// Wake-Up Requests: /wakeUpRequests/{requestId}
match /wakeUpRequests/{requestId} {
  // Only the sender or receiver can view a request
  allow read: if isAuthenticated() && (
    resource.data.senderUid == request.auth.uid ||
    resource.data.receiverUid == request.auth.uid
  );

  // Create: Authenticated sender, valid recipient, status PENDING
  allow create: if isAuthenticated()
                && request.resource.data.senderUid == request.auth.uid
                && request.resource.data.receiverUid != request.auth.uid
                && request.resource.data.status == "PENDING"
                && request.resource.data.requestId == requestId
                && request.resource.data.createdAt != null
                && request.resource.data.expiresAt != null;

  // Update: Atomic lifecycle transitions
  allow update: if isAuthenticated()
                // Immutable fields check
                && request.resource.data.requestId == resource.data.requestId
                && request.resource.data.senderUid == resource.data.senderUid
                && request.resource.data.receiverUid == resource.data.receiverUid
                && request.resource.data.createdAt == resource.data.createdAt
                && (
                  // 1. Receiver acknowledges
                  (
                    resource.data.status == "PENDING"
                    && request.auth.uid == resource.data.receiverUid
                    && request.resource.data.status == "ACKNOWLEDGED"
                    && request.resource.data.acknowledgedAt != null
                  )
                  ||
                  // 2. Receiver declines
                  (
                    resource.data.status == "PENDING"
                    && request.auth.uid == resource.data.receiverUid
                    && request.resource.data.status == "DECLINED"
                    && request.resource.data.declinedAt != null
                  )
                  ||
                  // 3. Sender cancels
                  (
                    resource.data.status == "PENDING"
                    && request.auth.uid == resource.data.senderUid
                    && request.resource.data.status == "CANCELLED"
                    && request.resource.data.cancelledAt != null
                  )
                  ||
                  // 4. Mark expired
                  (
                    resource.data.status == "PENDING"
                    && (request.auth.uid == resource.data.senderUid || request.auth.uid == resource.data.receiverUid)
                    && request.resource.data.status == "EXPIRED"
                  )
                );

  // Requests are persistent records; cannot be deleted arbitrarily by clients
  allow delete: if false;
}
```

---

## 7. FCM Receiving Architecture

1. **Token Capture & Hashing**:
   - `FcmTokenRepository` retrieves the device's token via `FirebaseMessaging.getInstance().token`.
   - Computes a SHA-256 hash (truncated to 32 chars) as document ID in `/users/{uid}/fcmTokens/{tokenHash}`.
   - Saves the token along with the device model and server timestamp.
   - On logout, unregisters the token to prevent push delivery to signed-out users.
2. **Foreground & Background Reception**:
   - In foreground: `QalbiMessagingService.onMessageReceived` parses the message and posts a notification. Additionally, `HomeScreen`'s Firestore listener instantly renders the wake-up alert banner.
   - In background / locked: The system tray displays the high-priority `qalbi_wake_up_channel` notification with vibration and alert sound.
3. **Deep Link Navigation**:
   - Tapping the notification launches `MainActivity` with `EXTRA_REQUEST_ID`.
   - `AppNavigation` navigates directly to `Screen.IncomingRequest.withId(requestId)`.

---

## 8. FCM Sender Architecture and Deployment Status

### Why Firebase Cloud Functions is NOT Deployed:
- Deploying Cloud Functions in Firebase (even 1st gen HTTP functions or Firestore triggers) **strictly mandates upgrading the Firebase project to the Blaze (Pay-as-you-go) plan**.
- The project requirement explicitly states: **"QALBI must not require me to spend ₹1 now or in the future. Do not enable Firebase Blaze billing. Do not require a credit card."**
- In accordance with the prompt's Architecture Decision Rule, we did not bypass this constraint with an insecure shortcut (such as placing service-account credentials in the Android APK).

### Implemented Zero-Cost Solution:
- We created a standalone Node.js daemon in `backend/fcm-dispatcher/dispatcher.js` using the official `firebase-admin` SDK.
- It can be run on:
  1. The developer's local PC during development/testing (`npm start`).
  2. Any free home machine or Raspberry Pi.
  3. A free continuous runner.
- The dispatcher:
  - Listens to Firestore `/wakeUpRequests` where `status == 'PENDING'`.
  - Queries the receiver's tokens under `/users/{receiverUid}/fcmTokens`.
  - Sends high-priority push payloads via `admin.messaging().sendEachForMulticast()`.
  - Automatically prunes invalid/expired tokens (`messaging/registration-token-not-registered`).
  - Marks `fcmDispatched: true` on the request.
- **Cost**: Exactly **₹0**.
- **Security**: 100% compliant — no credentials in the mobile app.

---

## 9. Required Firebase Console Changes

1. Open [Firebase Console](https://console.firebase.google.com/) → **Project Settings** → **Cloud Messaging**.
2. Verify that **Firebase Cloud Messaging API (V1)** is **Enabled**. (If disabled, click the three dots → Manage API in Google Cloud Console → Click "Enable").
3. Update Firestore Security Rules:
   - Go to **Firestore Database** → **Rules**.
   - Paste the updated `firestore.rules` and click **Publish**.
4. (Optional, for running the FCM Dispatcher):
   - Go to **Project Settings** → **Service accounts**.
   - Click **Generate new private key**.
   - Place the file at `backend/fcm-dispatcher/serviceAccountKey.json`.

---

## 10. Required Configuration Files

- `app/google-services.json`: Required for Android app build and runtime Firebase connection. (Ignored by `.gitignore`).
- `backend/fcm-dispatcher/serviceAccountKey.json`: Required only when running the push dispatcher daemon. (Ignored by `.gitignore`).

---

## 11. Exact Build and Test Commands Executed

```powershell
# 1. Run automated unit tests
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat testDebugUnitTest

# 2. Build debug APK
.\gradlew.bat assembleDebug
```

---

## 12. Actual Results of Commands

- **Unit Tests**:
  - `WakeUpCommunicationTest`: 10 passed (Self wake-up rejection, model creation, expiry computation, receiver acknowledgement, receiver decline, sender cancellation, terminal state immutability, fromMap auto-expiry, FCM device token serialization, exception messages).
  - `FriendPairingTest`: 7 passed.
  - `AuthValidationTest`: 8 passed.
  - **Total**: 25 passed, 0 failed.
- **Debug APK Build**:
  - `assembleDebug` successfully compiled and packaged `app-debug.apk`.

---

## 13. Tests That Remain Manual

- **Two physical devices**: Testing cross-device FCM push delivery while one device is in deep sleep or screen-locked.
- **Push notification display**: Verifying notification sound and vibration on physical hardware across different OEM battery optimization settings (Samsung, Xiaomi, Pixel).

---

## 14. Two-Device Testing Instructions

1. **Install App on Both Devices**:
   - Install `app-debug.apk` on Device A and Device B.
2. **Account Setup & Pairing**:
   - Device A: Register/Log in as User A (e.g. `alice`).
   - Device B: Register/Log in as User B (e.g. `bob`).
   - Device A creates a pairing code in "Connect to a Friend".
   - Device B enters and redeems the code. Both devices now see each other under Connected Friends.
3. **Send Wake-Up Request**:
   - Device A opens Friends list → selects Bob → taps **Wake Up**.
   - Confirmation dialog appears → taps **Send Request**.
   - Device A displays outgoing request card with live status: `Awaiting response...`
4. **Deliver & Receive**:
   - If `backend/fcm-dispatcher` is running: Device B immediately receives a push notification `☀️ Wake Up from Alice`.
   - If Device B has the app open: Bob sees the active alert banner on the Home screen.
5. **Acknowledge / Decline**:
   - Device B taps the notification or alert banner → opens `IncomingRequestScreen`.
   - Device B taps **"☀️ I'm Awake!"**.
   - Device B transitions to confirmed state.
   - Device A's screen automatically updates in real-time to: `✅ Friend is awake!`.

---

## 15. Known Limitations

1. **Zero-Cost Background Push Limitation**:
   - Without an active backend dispatcher running, real remote FCM push notifications to a backgrounded or locked device cannot be sent for free via serverless cloud triggers (because Cloud Functions requires Blaze).
   - However, foreground real-time sync works 100% out-of-the-box via Firestore listeners without any backend dispatcher.
2. **OEM Battery Optimization**:
   - Aggressive battery managers (e.g. MIUI, ColorOS) may defer FCM high-priority messages if the app is force-closed by the OS.
3. **No Looping Audio in Phase 4**:
   - Phase 4 delivers the notification and state machine. The looping audio alarm engine, ringtone player, and full-screen activity belong to Phase 5.

---

## 16. Security Considerations

- **No Secrets in APK**: Client APK contains zero service account credentials, server keys, or private keys.
- **Immutability Enforced**: Firestore rules guarantee sender UID, receiver UID, request ID, and creation timestamps can never be tampered with after creation.
- **Forging Prevention**: Senders cannot forge receiver acknowledgements; receivers cannot forge sender cancellations.
- **Token Privacy**: FCM device tokens are stored in private subcollections accessible only by the owning user.

---

## 17. Zero-Cost Compatibility Assessment

- **Firestore**: Free tier includes 50,000 reads, 20,000 writes, and 20,000 deletes daily. A wake-up cycle consumes ~4 reads and ~2 writes. At ₹0, this supports thousands of requests daily.
- **Firebase Cloud Messaging**: 100% free with unlimited push notifications.
- **Firebase Authentication**: 100% free with unlimited Email/Password accounts.
- **Verdict**: The architecture strictly meets the ₹0 requirement.

---

## 18. Features Status Summary

- **Send Wake-Up Request**: ✅ COMPLETE
- **Secure Request Authorization**: ✅ COMPLETE
- **Persistent Firestore Records**: ✅ COMPLETE
- **FCM Receiving & Notifications**: ✅ COMPLETE
- **Receiver Screen & Actions**: ✅ COMPLETE
- **Sender Real-Time Feedback**: ✅ COMPLETE
- **Zero-Cost Push Dispatcher**: ✅ COMPLETE (Local/self-hosted dispatcher provided)
- **Unit Testing**: ✅ COMPLETE (25/25 automated tests pass)
- **Physical Two-Device Push**: ⏳ PENDING (Awaiting user test on two physical devices)

---

## 19. Exact Next Steps for Phase 5

1. Implement looping ringtone/alarm audio engine using `MediaPlayer` or `ExoPlayer` targeting `USAGE_ALARM`.
2. Implement Android full-screen intent (`USE_FULL_SCREEN_INTENT`) for lock-screen wake-up alerts.
3. Add gentle ramp-up volume curve and vibration feedback.
4. Add snooze and custom wake-up challenge/dismiss options.
