# QALBI — Physical Device Manual Testing Guide

> **Important:** This document is prepared for you to execute on your physical Android devices.
> In accordance with the project specification, all repository-level code, logic, and automated tests have been validated in the repository. Actual hardware execution, OEM battery optimizers, and lock-screen display behavior belong exclusively to you on physical hardware.
>
> All **Actual Result** and **Pass/Fail** fields below are left blank for your recorded entries.

---

## 1. Test Environment Setup & Preconditions

Before commencing physical testing, ensure:
1. **Device A** (e.g., Primary Android device running Android 10–14).
2. **Device B** (e.g., Secondary Android device or partner's phone).
3. Both devices have Google Play Services installed.
4. The QALBI debug APK (`app-debug.apk`) or signed release APK has been transferred to both devices.
5. Your free Firebase backend on the Spark tier is active, and `firestore.rules` are published to Cloud Firestore.

---

## 2. Test Execution Test Suite

### Section A — Installation & Startup

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **A-01** | Fresh APK Installation | Device A running Android 8.0+ | 1. Transfer APK to device.<br>2. Install via Android Package Installer.<br>3. Grant install unknown apps if prompted. | APK installs cleanly without package parse errors or signature mismatch. | | | |
| **A-02** | Initial Launch & Splash | A-01 passed | 1. Tap app launcher icon from home screen. | App launches immediately without crash; midnight blue theme displays with splash screen; navigates to Welcome/Login screen. | | | |
| **A-03** | Version Metadata Verification | App open | 1. Navigate to Settings screen.<br>2. Inspect bottom of Settings card. | Version displays as `v1.0.0 (Release Candidate)`. | | | |

---

### Section B — Authentication & Account Isolation

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **B-01** | Account Registration (Device A) | App on Welcome screen | 1. Tap Register.<br>2. Enter Display Name, unique username, email, and password.<br>3. Tap Register. | Account is created in Firebase Auth; profile document is written to Firestore; user is navigated to Home Screen. | | | |
| **B-02** | Account Registration (Device B) | App on Device B | 1. Tap Register.<br>2. Register second unique user (e.g. `@friend_b`). | Device B user registers successfully and lands on Home Screen. | | | |
| **B-03** | Log Out and Session Clear | Device A logged in | 1. Go to Profile.<br>2. Tap "Log Out". | User is logged out; session state is completely cleared; redirected to Welcome screen. | | | |
| **B-04** | Log Back In | Device A logged out | 1. Tap Sign In.<br>2. Enter credentials created in B-01.<br>3. Tap Sign In. | User logs back in successfully; display name and username match previously registered account. | | | |
| **B-05** | Account Switching Privacy | Device A logged in | 1. Log out.<br>2. Log into Device B account on Device A. | User B profile loads; no cached data, pairing codes, or friend lists from User A are visible. | | | |

---

### Section C — Zero-Cost Profile Avatar Customization

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **C-01** | Avatar Palette Selection | Device A logged in | 1. Tap Profile icon in top bar.<br>2. Under "Avatar Theme", tap the "Emerald Dawn" palette circle. | Avatar background color instantly changes to Emerald Dawn (#2A9D8F); white selection indicator highlights circle; change persists. | | | |
| **C-02** | Avatar Palette Cross-Device Sync | Device A paired with Device B | 1. Change Device A avatar theme to "Sunset Coral" (#E76F51).<br>2. Inspect Device A's card in Device B's connected friends list. | Device B displays Device A's initials on Sunset Coral background. | | | |

---

### Section D — Friend Pairing System (Crockford Base32)

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **D-01** | Pairing Code Generation | Device A on Home Screen | 1. Tap "Connect to a Friend".<br>2. Observe generated code. | An 8-character Crockford Base32 code is generated (e.g. `K9X2-M4B7`); no ambiguous characters (no I, L, O, U); countdown timer starts at 10 minutes. | | | |
| **D-02** | Code Redemption on Device B | Device B on "Connect Friend" screen, Device A code active | 1. Enter the 8-character code on Device B.<br>2. Tap "Connect". | Success dialog appears showing Device A's display name; both devices now show each other in "Connected Friends". | | | |
| **D-03** | Self-Pairing Rejection | Device A on "Connect Friend" screen | 1. Generate code on Device A.<br>2. Attempt to redeem that exact same code on Device A. | App displays clear rejection error: "You cannot connect with your own pairing code." | | | |
| **D-04** | Expired Code Handling | New code generated | 1. Wait until 10-minute validity window passes (or test with past timestamp).<br>2. Attempt redemption. | Code is rejected with message: "This connection code has expired." No connection is created. | | | |
| **D-05** | Reused Code Rejection | Code already redeemed in D-02 | 1. Attempt to redeem the redeemed code on a third account. | Code is rejected with "This code has already been redeemed." | | | |
| **D-06** | Disconnect Friend | Device A connected to Device B | 1. Open Friend Details on Device A.<br>2. Tap "Disconnect Friend" and confirm. | Connection document is removed from Firestore; friend disappears from both devices' friends lists. | | | |
| **D-07** | Block User | Device A connected to Device B | 1. Open Friend Details on Device A.<br>2. Tap "Block User" and confirm. | Friend is blocked and connection severed; User B cannot send requests or re-pair with User A. | | | |

---

### Section E — Remote Wake-Up Communication

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **E-01** | Send Wake-Up Request | Device A & Device B connected | 1. On Device A, tap Friend B.<br>2. Tap "Wake Up [Friend B]". | Request is created in Firestore with status `PENDING`; Device A screen updates to animated waiting pulse; shows "Waking up [Friend B]…". | | | |
| **E-02** | Active Request Banner on Receiver | Device B on Home Screen | 1. With request pending, view Device B home screen. | Prominent banner displays: "[Friend A] is waking you up! ☀️ — Tap to respond: I'm Awake!". | | | |
| **E-03** | "I'm Awake!" Receiver Response | Device B ringing or viewing request | 1. Tap "☀️ I'M AWAKE!" on Device B. | Device B audio & vibration silence immediately; Firestore request status transitions to `ACKNOWLEDGED`. | | | |
| **E-04** | Live Status Update on Sender | E-03 executed | 1. Observe Device A screen while Device B taps "I'm Awake!". | Device A screen updates in real-time to "☀️ [Friend B] is Awake!" with celebration highlight. | | | |
| **E-05** | Receiver Decline | Device A sends new request to Device B | 1. Device B taps "Decline". | Device B stops; request transitions to `DECLINED`; Device A updates to show declined state. | | | |
| **E-06** | Sender Cancellation | Device A sends request | 1. Before Device B responds, Device A taps "Cancel Wake-Up". | Request transitions to `CANCELLED`; Device B stops ringing immediately if it was ringing. | | | |
| **E-07** | 15-Minute Expiry | Device A sends request | 1. Leave request unanswered for 15 minutes. | Request automatically expires; status becomes `EXPIRED`; cannot be acknowledged or restarted. | | | |

---

### Section F — Alarm Engine & Audio Playback

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **F-01** | Bundled Acoustic Chime Playback | Device B receives wake-up alert | 1. Incoming alarm triggers. | Original acoustic chime (`qalbi_alarm.wav`) plays continuously in an infinite loop using `USAGE_ALARM`. | | | |
| **F-02** | Sound Preview in Settings | Device on Settings screen | 1. Tap "Acoustic Chime (QALBI Original)".<br>2. Tap again to stop. | Audio plays once cleanly; stops immediately on second tap. | | | |
| **F-03** | Gentle Volume Ramp-Up | Settings -> "Gentle Volume Ramp-Up" enabled | 1. Trigger incoming alarm. | Audio starts soft (~15% volume) and escalates smoothly over 15 seconds to full volume. | | | |
| **F-04** | Vibration Rhythmic Pulsation | Settings -> Vibration enabled | 1. Trigger incoming alarm. | Device vibration motor pulses according to chosen pattern (`CADENCE`, `HEARTBEAT`, or `GENTLE`). | | | |
| **F-05** | Vibration Pattern Preview | Settings screen | 1. Tap "Heartbeat" or "Gentle" pattern chip. | Device vibrates briefly with selected pattern to preview haptic feel. | | | |
| **F-06** | Immediate Hardware Silencing | Device ringing | 1. Tap "☀️ I'M AWAKE!". | Both audio playback and vibration stop within <100ms of finger touch. | | | |
| **F-07** | Duplicate Push Safety | Device already ringing | 1. Simulate duplicate push notification or event. | Audio does not stutter, restart, or create overlapping playback. | | | |

---

### Section G — Android Background & Lock-Screen Behavior

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **G-01** | App in Foreground | Device B app open on Home | 1. Send wake-up from Device A. | Full incoming request screen opens immediately; chime loops; vibration active. | | | |
| **G-02** | App in Background | Device B app minimized to home screen | 1. Send wake-up from Device A. | Persistent foreground service notification appears; audio loops; tapping notification opens alarm UI. | | | |
| **G-03** | Screen Locked / Keyguard Active | Device B screen turned off and locked | 1. Send wake-up from Device A. | Display turns on (`turnScreenOn`); full-screen alarm alert presents over keyguard (`showWhenLocked`); "I'm Awake!" is accessible without unlocking device first. | | | |
| **G-04** | App Swiped from Recents | Device B swiped away from task manager | 1. Send wake-up from Device A. | If trusted FCM push or background sync is running, foreground notification is posted with alarm audio. | | | |
| **G-05** | App Force-Stopped via Settings | Device B force-stopped in Android Settings | 1. Force-stop app in OS Settings.<br>2. Send wake-up from Device A. | **Expected Android Constraint:** Per Android OS security design, force-stopped apps do not run until manually re-launched by the user. Verify app does not crash when reopened. | | | |
| **G-06** | Notification Permission Denied | Device B denies `POST_NOTIFICATIONS` | 1. Revoke notification permission in Android settings.<br>2. Send wake-up from Device A. | App handles missing permission gracefully without crashing; prompt to enable permissions is shown. | | | |
| **G-07** | Device in Silent / Do Not Disturb | Device B set to Silent mode | 1. Send wake-up from Device A. | Per `USAGE_ALARM` audio attributes, alarm audio plays through the alarm stream unless device DND settings explicitly suppress alarms. | | | |
| **G-08** | Offline Connectivity Warning | Device B WiFi/mobile data turned off | 1. Turn off all network connections.<br>2. Open QALBI Home screen. | Amber offline banner displays: "No internet connection. Requests and status will sync once reconnected." | | | |

---

### Section H — Security & Privacy Invariants

| Test ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Pass/Fail | Notes |
|---|---|---|---|---|---|---|---|
| **H-01** | Private Email Non-Exposure | Device A & Device B connected | 1. Inspect Friend Details on Device B.<br>2. Inspect Firestore `/publicProfiles` document in console. | Device A's email is NEVER visible in Device B's UI and NEVER present in `/publicProfiles/` document. | | | |
| **H-02** | FCM Device Token Concealment | Devices paired | 1. Inspect app UI on both devices.<br>2. Inspect logs. | Device FCM push tokens are never displayed in UI and never printed in unmasked production logs. | | | |
| **H-03** | Unauthorized Request Creation | User C not connected to User A | 1. User C attempts to create wake-up request targeting User A directly. | Firestore security rule rejects write: connection membership check fails. | | | |
| **H-04** | Forged Acknowledgement Denial | Request exists between User A and User B | 1. User C attempts to update status to `ACKNOWLEDGED`. | Firestore security rule rejects update: `request.auth.uid == resource.data.receiverUid` fails. | | | |
| **H-05** | Private Document Read Isolation | User B authenticated | 1. Attempt to read `/users/{UserA_UID}` from User B client. | Access is denied by Cloud Firestore: `isOwner(userId)` enforces that only User A can read their own user profile document. | | | |

---

## 3. Physical Test Execution Summary Sheet

- **Tester Name:** ___________________________
- **Date Tested:** ___________________________
- **Device A Model & Android Version:** ___________________________
- **Device B Model & Android Version:** ___________________________
- **Total Tests Executed:** _____ / 34
- **Passed:** _____
- **Failed:** _____
- **Blocked:** _____

### Tester Signature / Notes:
_______________________________________________________________________________
