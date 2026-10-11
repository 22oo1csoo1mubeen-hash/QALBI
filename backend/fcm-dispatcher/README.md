# QALBI — Trusted FCM Dispatcher (₹0 Zero-Cost Architecture)

This standalone service bridges real remote Firebase Cloud Messaging (FCM) push notifications between connected users on QALBI without requiring paid Google Cloud infrastructure.

---

## 1. Why is this necessary?

### Security Rule
Under Google and Android security guidelines:
- An Android client **must NEVER** contain Firebase Service Account credentials or server private keys.
- Clients can never dispatch privileged push notifications directly to other users via FCM API v1.

### Cost Rule (Strict ₹0 Budget)
- Firebase Cloud Functions **requires upgrading to the Blaze (Pay-as-you-go) plan** with a linked credit card or billing account.
- Upgrading to Blaze violates the project's strict zero-billing requirement.
- Therefore, this lightweight dispatcher runs in a trusted server, local development PC, or any free compute environment (e.g. Raspberry Pi, free GitHub Action runner, home server) using the official Firebase Admin SDK for **₹0**.

---

## 2. Setup Instructions

### Step 1: Obtain Firebase Service Account Key
1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Select your QALBI project.
3. Click the **Gear icon (Project settings)** in the sidebar.
4. Navigate to the **Service accounts** tab.
5. Click **Generate new private key**, then confirm by clicking **Generate key**.
6. Save the downloaded `.json` file into this directory as:
   ```
   backend/fcm-dispatcher/serviceAccountKey.json
   ```
   *(Note: This file is excluded from Git via `.gitignore` to prevent credential leaks).*

### Step 2: Install Dependencies and Start
From the `backend/fcm-dispatcher` directory, run:

```bash
npm install
npm start
```

### Step 3: Verify Remote Push Delivery
1. When User A taps **Wake Up** on their Android device:
   - The Android client writes a validated request to Firestore: `/wakeUpRequests/{requestId}` (`status: PENDING`).
2. The dispatcher automatically detects the new request in real-time.
3. The dispatcher looks up User B's device tokens under `/users/{receiverUid}/fcmTokens`.
4. The dispatcher sends a high-priority FCM message payload with:
   - Channel: `qalbi_wake_up_channel`
   - Priority: `max`
   - Action: `com.wakeus.app.ACTION_NAVIGATE_WAKEUP`
5. User B's device receives the push notification via `QalbiMessagingService` even if the app is in the background or screen is turned off!
