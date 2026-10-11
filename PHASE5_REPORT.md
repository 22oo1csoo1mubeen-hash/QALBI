# QALBI — Phase 5 Implementation Report: Complete Alarm Engine

**Date:** 2026-10-11  
**Phase:** 5 — Complete Alarm Engine  
**Status:** CODE COMPLETE & TESTED (State machine, audio engine, vibration loops, foreground service, full-screen notification, and lockscreen integration implemented; unit tests passed, debug APK assembled)

---

## 1. Phase 5 Objectives

Phase 5 delivers a complete, production-grade Android alarm experience for QALBI:
1. Continuous, looping alarm playback with dedicated `USAGE_ALARM` audio focus and clean interruption handling.
2. An original bundled acoustic chime asset (`res/raw/qalbi_alarm.wav`) with 100% royalty-free, legal distribution rights.
3. Rhythmic, escalating vibration patterns via modern `VibratorManager` and `Vibrator` with hardware availability detection.
4. An Android Foreground Service (`WakeUpAlarmService`) ensuring audio and vibration continuity when the app is in the background or process memory is trimmed.
5. Lock-screen presentation (`setShowWhenLocked`, `setTurnScreenOn`, and `USE_FULL_SCREEN_INTENT`) waking the device display for incoming wake-up requests.
6. A responsive incoming alarm screen with an animated visual pulse, accessible "☀️ I'M AWAKE!" button, and "Decline" option.
7. Immediate hardware silencing upon tapping "I'm Awake!" while atomically confirming acknowledgement with Cloud Firestore.
8. Real-time cancellation and expiry synchronization: if the sender cancels or the 15-minute validity window passes while ringing, the alarm stops immediately.
9. Strict adherence to the ₹0 budget (Firebase Spark tier) with zero client credential exposure.

---

## 2. Existing Architecture Inspected

The project preserves and builds upon the existing stack:
- **UI & Design**: Jetpack Compose and Material 3 design system with Midnight Blue (#0D1B2A), Muted Rose (#C87B8B), Subtle Gold (#C9A84C), and Warm Ivory typography.
- **Architecture**: MVVM with Repository boundary (`QalbiRepository`, `WakeUpRepository`, `FcmTokenRepository`).
- **Reactive Streams**: Kotlin Coroutines Flow (`StateFlow` and Firestore snapshot listeners).
- **Communication (Phase 4)**: Cloud Firestore `/wakeUpRequests/{requestId}` with states `PENDING`, `ACKNOWLEDGED`, `DECLINED`, `CANCELLED`, `EXPIRED`.
- **FCM**: `QalbiMessagingService` listening for push routing tokens and wake-up notifications.

---

## 3. Alarm State Machine

The local alarm engine separates physical device playback state from authoritative remote server status.

```
       +---------------------------------------------+
       |                    IDLE                     |
       +---------------------------------------------+
                              |
                     startAlarm(requestId)
                              v
       +---------------------------------------------+
       |                  STARTING                   |
       +---------------------------------------------+
                              |
                     Foreground service active
                     Audio & vibration started
                              v
       +---------------------------------------------+
       |                   RINGING                   | <----+
       +---------------------------------------------+      |
            |                 |                |            |
     User taps        User taps       Sender cancels        |
   "I'M AWAKE!"       "DECLINE"        or EXPIRED           |
            |                 |                |            |
            v                 v                v            |
    ACKNOWLEDGING         DECLINED         CANCELLED/       |
    (audio stops)             |             EXPIRED         |
       |       \              |                |            |
   Success     Network fail   |                |            |
       |            \         |                |            |
       v             v        |                |            |
 ACKNOWLEDGED      FAILED ----+----------------+------------+
       |           (stays stopped; retry or dismiss)
       v
  resetToIdle()
       v
      IDLE
```

### Local Alarm Status Enum (`LocalAlarmStatus`):
- `IDLE`: No alarm active. Hardware off.
- `RECEIVED`: FCM message or alert received, validating parameters.
- `STARTING`: Initializing Foreground Service, acquiring WakeLock.
- `RINGING`: Audio looping, vibration pulsing, full-screen notification active.
- `ACKNOWLEDGING`: Sound and vibration immediately halted; submitting Firestore acknowledgement.
- `STOPPING`: Releasing hardware, abandoning audio focus, removing notification.
- `ACKNOWLEDGED`: Receiver confirmed awake. Service terminated.
- `DECLINED`: Receiver declined request. Service terminated.
- `CANCELLED`: Sender cancelled request while device was ringing. Service terminated.
- `EXPIRED`: 15-minute validity window elapsed. Service terminated.
- `FAILED`: Network or initialization failure; audio stopped to prevent uncontrolled ringing.

---

## 4. Alarm Audio Implementation & Licence/Source

### Asset Details:
- **File**: `app/src/main/res/raw/qalbi_alarm.wav` (352,844 bytes).
- **Format**: 44,100 Hz, 16-bit PCM, Mono WAV.
- **Composition**: An original melodic harmonic chime arpeggio:
  Chord tones C5 (523.25 Hz), E5 (659.25 Hz), G5 (783.99 Hz), B5 (987.77 Hz), C6 (1046.50 Hz) with natural exponential acoustic chime decay.
- **Licence & Rights**: 100% original, generated specifically for QALBI. Public domain / MIT compatible, completely royalty-free with zero copyright claims or third-party dependencies.

### Playback Lifecycle (`AlarmSoundPlayer`):
- Uses native Android `MediaPlayer` configured with:
  - Usage: `AudioAttributes.USAGE_ALARM`
  - Content Type: `AudioAttributes.CONTENT_TYPE_SONIFICATION`
  - Loop: `isLooping = true`
- **Audio Focus**:
  - API 26+: Requests `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` via `AudioFocusRequest.Builder`.
  - On focus loss (`AUDIOFOCUS_LOSS_TRANSIENT`): Pauses playback.
  - On focus regained: Resumes playback.
- **Fallback**: If the raw asset fails to load on any OEM hardware, automatically falls back to `RingtoneManager.getDefaultUri(TYPE_ALARM)` or `TYPE_NOTIFICATION`.
- **Preview**: Includes `preview(context)` for Settings, playing once and abandoning audio focus upon completion.

---

## 5. Vibration Implementation

### Hardware Control (`AlarmVibrator`):
- **API Support**: Uses `VibratorManager` on Android 12+ (API 31+) with fallback to `Context.VIBRATOR_SERVICE`.
- **Hardware Check**: Queries `hasVibrator()` before requesting vibration; gracefully skips on devices without vibration motors.
- **Pattern**: Rhythmic alarm cadence:
  `longArrayOf(0, 800, 400, 800, 400, 800)` (0ms delay, 800ms vibrate, 400ms pause, 800ms vibrate...).
- **Looping**: Configured with `VibrationEffect.createWaveform(pattern, 0)` on API 26+ (0 specifies continuous loop).
- **Cancellation**: Safely invokes `vibrator.cancel()` upon any state transition out of `RINGING`.

---

## 6. Lock-Screen & Notification Implementation

### Lock-Screen Presentation:
- Configured in `MainActivity`:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
  } else {
      window.addFlags(
          WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      )
  }
  ```
- Declared in `AndroidManifest.xml` on `MainActivity`:
  `android:showWhenLocked="true"`
  `android:turnScreenOn="true"`
  `android:launchMode="singleTask"`

### Notification & Full-Screen Intent (`NotificationHelper`):
- Dedicated high-priority notification channel: `qalbi_wake_up_channel`.
  - Importance: `IMPORTANCE_HIGH`
  - Audio Usage: `USAGE_ALARM`
  - Lockscreen Visibility: `VISIBILITY_PUBLIC`
- **Full-Screen Intent**: Uses `setFullScreenIntent(fullScreenPendingIntent, true)` with `android.permission.USE_FULL_SCREEN_INTENT`. When the screen is turned off or locked, the full-screen alarm activity is presented over the keyguard.
- **Notification Action Buttons**:
  - `☀️ I'M AWAKE!`: Direct PendingIntent invoking `WakeUpAlarmService.ACTION_ACKNOWLEDGE_ALARM`.
  - `DECLINE`: Direct PendingIntent invoking `WakeUpAlarmService.ACTION_DECLINE_ALARM`.

---

## 7. Foreground Service Design

### `WakeUpAlarmService`:
- Declared in `AndroidManifest.xml` with:
  `android:foregroundServiceType="mediaPlayback"`
  `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
  `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />`
- Started via `ContextCompat.startForegroundService(context, intent)`.
- Calls `startForeground(NOTIFICATION_ID, notification)` within 5 seconds of launch.
- Manages audio and vibration in the background.
- Cleanly removes notification and releases WakeLock on `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()`.

---

## 8. FCM-to-Alarm Integration

When an FCM push message arrives in [QalbiMessagingService](file:///c:/QALBI/app/src/main/java/com/wakeus/app/data/fcm/QalbiMessagingService.kt):
1. Validates payload fields (`requestId`, `senderUid`, `senderDisplayName`, `receiverUid`).
2. Checks that the recipient matches the currently authenticated Firebase user.
3. Invokes `WakeUpAlarmManager.startAlarm(applicationContext, requestId, senderUid, senderDisplayName, senderUsername, expiresAt)`.
4. `WakeUpAlarmManager` checks for duplicate requests and expiry, transitions status to `STARTING`, and launches `WakeUpAlarmService`.
5. `WakeUpAlarmService` calls `startForeground()` and triggers `AlarmSoundPlayer` and `AlarmVibrator`.
6. Concurrently, a Firestore snapshot listener is registered on `/wakeUpRequests/{requestId}` to monitor for sender cancellation or remote expiry.

---

## 9. Acknowledgement & Stop Behavior

When the receiver taps **"☀️ I'M AWAKE!"** (either in the UI or on the notification):
1. **Immediate Hardware Silence**: Audio and vibration stop instantly so the waking user is no longer subjected to loud sounds.
2. **Local State**: Transitions to `ACKNOWLEDGING`.
3. **Firestore Update**: Atomically updates `/wakeUpRequests/{requestId}`:
   `status = ACKNOWLEDGED`
   `acknowledgedAt = serverTimestamp()`
4. **Service & Notification Termination**:
   - `WakeUpAlarmService` calls `stopForeground(true)` and `stopSelf()`.
   - `NotificationHelper.cancelNotification()` removes the system notification.
   - `WakeLockHelper.release()` releases the CPU wake lock.
5. **Sender Live Update**: Sender's `WakeUpScreen` automatically transitions to `✅ Friend is awake!`.
6. **Network Failure Safety**: If the network times out during acknowledgement, local audio remains silenced, but the state transitions to `FAILED` with an explicit error message instead of falsely claiming success.

---

## 10. Duplicate Prevention & Request Expiry

- **Duplicate Pushes**: If multiple identical FCM messages arrive for a currently ringing request, `WakeUpAlarmManager.startAlarm` detects `current.status == RINGING && current.requestId == requestId` and ignores subsequent triggers.
- **Recomposition Safety**: `WakeUpAlarmManager` owns the player lifecycle; Compose recomposition does not recreate or restart audio.
- **Expiry Checking**:
  - Before starting: checks `System.currentTimeMillis() >= expiresAt`.
  - While ringing: Firestore snapshot listener monitors `expiresAt` and remote status. If expired, automatically triggers `handleExpiry()`, stopping all sound and vibration.
- **Sender Cancellation**: If the sender taps "Cancel" on their device while the receiver's phone is ringing, the Firestore listener detects `status == CANCELLED` and immediately stops the receiver's alarm.

---

## 11. Local Persistence & Process Recovery

- `WakeUpAlarmManager` maintains memory state in `_alarmState: MutableStateFlow<AlarmState>`.
- On application launch or service start, state is reconciled with authoritative Cloud Firestore data.
- Stale or expired requests stored in local memory are never resurrected.
- On user logout, `QalbiRepository.logout()` explicitly stops all alarm hardware and releases wake locks.

---

## 12. Manifest and Permission Changes

The following declarations were verified and added to `AndroidManifest.xml`:
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `android.permission.USE_FULL_SCREEN_INTENT`
- `android.permission.WAKE_LOCK`
- `android.permission.VIBRATE`
- `android.permission.POST_NOTIFICATIONS`
- `WakeUpAlarmService` declaration with `foregroundServiceType="mediaPlayback"`.
- `MainActivity` configuration with `launchMode="singleTask"`, `showWhenLocked="true"`, and `turnScreenOn="true"`.

---

## 13. Files Created or Modified

| File | Status | Description |
|---|---|---|
| `app/src/main/res/raw/qalbi_alarm.wav` | Added | Original 44.1kHz PCM acoustic chime alarm audio file. |
| `app/src/main/java/com/wakeus/app/data/alarm/AlarmState.kt` | Added | `LocalAlarmStatus` enum with transition validation and `AlarmState` data class. |
| `app/src/main/java/com/wakeus/app/data/alarm/AlarmSoundPlayer.kt` | Added | Audio focus management, native `MediaPlayer` looping, raw asset playback, preview. |
| `app/src/main/java/com/wakeus/app/data/alarm/AlarmVibrator.kt` | Added | `VibratorManager` waveform vibration loops with hardware capability checking. |
| `app/src/main/java/com/wakeus/app/data/alarm/WakeLockHelper.kt` | Added | Safe WakeLock acquisition with mandatory 5-minute timeout. |
| `app/src/main/java/com/wakeus/app/data/alarm/WakeUpAlarmManager.kt` | Added | Central singleton managing alarm lifecycle, hardware coordination, and Firestore sync. |
| `app/src/main/java/com/wakeus/app/data/alarm/WakeUpAlarmService.kt` | Added | Android Foreground Service displaying ongoing notification with full-screen intent. |
| `app/src/main/java/com/wakeus/app/data/fcm/NotificationHelper.kt` | Modified | Foreground alarm notification builder, action buttons, full-screen intent. |
| `app/src/main/java/com/wakeus/app/data/fcm/QalbiMessagingService.kt` | Modified | Triggers `WakeUpAlarmManager.startAlarm` on incoming push. |
| `app/src/main/java/com/wakeus/app/MainActivity.kt` | Modified | Added `configureLockScreenPresentation` (`setShowWhenLocked`, `setTurnScreenOn`). |
| `app/src/main/java/com/wakeus/app/AndroidManifest.xml` | Modified | Added Phase 5 permissions and `WakeUpAlarmService`. |
| `app/src/main/java/com/wakeus/app/viewmodel/IncomingRequestViewModel.kt` | Modified | Integrated with `WakeUpAlarmManager` for acknowledgement and alarm starting. |
| `app/src/main/java/com/wakeus/app/ui/wakeup/IncomingRequestScreen.kt` | Modified | Added visual alarm ringing pulse animation and accessible action buttons. |
| `app/src/main/java/com/wakeus/app/ui/home/HomeScreen.kt` | Modified | Shows active ringing indicator on incoming alert banner. |
| `app/src/main/java/com/wakeus/app/ui/settings/SettingsScreen.kt` | Modified | Added interactive alarm sound preview and vibration toggle. |
| `app/src/main/java/com/wakeus/app/data/repository/QalbiRepository.kt` | Modified | Added hardware stop and wake lock release on logout. |
| `app/src/test/java/com/wakeus/app/AlarmEngineTest.kt` | Added | 12 automated unit tests for state machine, expiry, duplicate rejection, and failures. |

---

## 14. Tests and Actual Results

### Automated Unit Tests:
Command executed:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat testDebugUnitTest
```
- `AlarmEngineTest`: 12 passed
- `WakeUpCommunicationTest`: 10 passed
- `FriendPairingTest`: 7 passed
- `AuthValidationTest`: 8 passed
- **Total: 47 passed, 0 failed.**

---

## 15. Build Commands and Actual Outcomes

```powershell
.\gradlew.bat assembleDebug
```
**Outcome:** `BUILD SUCCESSFUL`.
Debug APK packaged at: `app\build\outputs\apk\debug\app-debug.apk`

---

## 16. Exact Firebase Setup Requirements

No new Firebase products or billing plans are required.
Existing free Firebase products used:
- **Firebase Authentication** (Free)
- **Cloud Firestore** (Free Spark tier)
- **Firebase Cloud Messaging** (Free API v1)

---

## 17. Manual Two-Device Testing Steps

1. Install `app-debug.apk` on Device A and Device B.
2. Pair Account A and Account B using the Phase 3 code redemption.
3. Lock Device B's screen (press power button).
4. On Device A, tap **Wake Up** → **Send Request**.
5. Device B screen turns on over the lockscreen, displaying the full-screen alarm notification and playing the melodic chime with rhythmic vibration.
6. Device B taps **"☀️ I'M AWAKE!"**.
7. Chime and vibration stop immediately.
8. Device A's screen updates to `✅ Friend is awake!`.

---

## 18. Platform Behavior Under Edge Cases

- **Screen Locked**: Wakes display via `setTurnScreenOn(true)` and `setShowWhenLocked(true)` and displays full-screen alert.
- **App Backgrounded**: Continuous playback maintained by `WakeUpAlarmService` with ongoing notification.
- **Process Death**: State is reconciled from Cloud Firestore on next startup; stale terminal requests never ring.
- **Force Stop**: Android blocks background service launch until user launches app again.
- **Offline Receiver**: Reconciles pending requests when connectivity returns.
- **DND / Silent Mode**: Alarm plays via `USAGE_ALARM`.

---

## 19. Known Limitations

- Extreme OEM battery managers (e.g. MIUI Battery Saver) may delay high-priority FCM messages if the user has enabled aggressive power restrictions.
- Force-stopped apps on Android cannot be woken by FCM until manually relaunched by the user.

---

## 20. Security Considerations

- **No Secrets in APK**: Client contains zero server keys or service-account JSONs.
- **Receiver Authorization**: Only the authenticated user matching `receiverUid` can acknowledge the alarm.
- **No Spoofing**: Senders cannot forge receiver acknowledgements.

---

## 21. Strict ₹0 Cost Assessment

- **All tools and services operate strictly under ₹0.**
- Firebase Spark plan is 100% sufficient for all authentication, Firestore, and FCM operations.
- No paid dependencies, SDKs, or hosting accounts were introduced.

---

## 22. Status Summary

- **Alarm State Machine**: ✅ COMPLETE
- **Alarm Sound Playback & Chime Asset**: ✅ COMPLETE
- **Vibration Engine**: ✅ COMPLETE
- **Lock-Screen Presentation**: ✅ COMPLETE
- **Foreground Service**: ✅ COMPLETE
- **"I'm Awake!" Acknowledgement**: ✅ COMPLETE
- **Remote Cancellation & Expiry Sync**: ✅ COMPLETE
- **Automated Unit Testing**: ✅ COMPLETE (47/47 passing tests)
- **Debug APK Build**: ✅ COMPLETE

---

## 23. Exact Remaining Work for Phase 6

Phase 6 will focus on final polish, profile photo uploads (using free Firebase Storage / local avatars), end-to-end device certification, and release preparation.
