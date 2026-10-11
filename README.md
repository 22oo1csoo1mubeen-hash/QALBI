# QALBI — Native Android Application

> **"A little closer, every morning."**

QALBI (Arabic: قلبي, "my heart") is a native Android application that lets connected friends or loved ones wake each other remotely — without exchanging mobile numbers.

---

## Current Status: Phase 4 Complete (Remote Wake-Up Communication)

- **Phase 1**: Complete 10-screen foundation with Jetpack Compose & Material 3.
- **Phase 2**: Production-oriented Firebase Authentication & Cloud Firestore integration (Email & Username accounts, atomic claims).
- **Phase 3**: Atomic Friend Pairing System under strict ₹0 Spark plan (Crockford Base32 codes, deterministic connections, blocklists).
- **Phase 4**: Remote Wake-Up Communication under strict ₹0 Spark plan:
  - Atomic Firestore request lifecycle (`/wakeUpRequests/{requestId}`) with statuses `PENDING`, `ACKNOWLEDGED`, `DECLINED`, `CANCELLED`, `EXPIRED`.
  - Recipient interaction screen (`IncomingRequestScreen`) with "I'm Awake!" and "Decline" actions.
  - Sender real-time status tracking (`WakeUpScreen`) with live state feedback and cancellation.
  - Home Screen active incoming wake-up alert banner.
  - FCM receiving service (`QalbiMessagingService`) and high-priority notification channel (`qalbi_wake_up_channel`).
  - Android 13+ `POST_NOTIFICATIONS` runtime permission launcher and deep-link handling.
  - Trusted zero-cost FCM push dispatcher (`backend/fcm-dispatcher/dispatcher.js`) using Firebase Admin SDK.
  - Unit test suite (`WakeUpCommunicationTest`) verifying state transitions, duplicate prevention, and authorization.
- **Phase 5**: Complete Alarm Engine under strict ₹0 Spark plan:
  - Looping native alarm audio playback with dedicated `USAGE_ALARM` audio focus and original bundled chime asset (`res/raw/qalbi_alarm.wav`).
  - Controlled rhythmic vibration with hardware capability detection (`AlarmVibrator`).
  - Android Foreground Service (`WakeUpAlarmService`) with persistent notification and full-screen intent.
  - Lockscreen wake-up presentation via `setShowWhenLocked`, `setTurnScreenOn`, and `USE_FULL_SCREEN_INTENT`.
  - Dedicated alarm UI with visual animated pulse, "☀️ I'M AWAKE!" action, and "Decline".
  - Safe stop sequence: immediate hardware silencing on tap with atomic Firestore status confirmation.
  - Remote cancellation and expiry observer: stops ringing immediately if sender cancels or request expires.
  - Sound preview feature in Settings (`SettingsScreen`).
  - Comprehensive unit test suite (`AlarmEngineTest`).

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Build system | Gradle 8.6 with Kotlin DSL |
| Navigation | Navigation Compose 2.7.7 |
| Architecture | MVVM with Repository boundary |
| State | ViewModel + StateFlow |
| Async | Kotlin Coroutines |
| Authentication | Firebase Authentication (Email/Password & synthetic username credentials) |
| Database | Cloud Firestore (User profiles & unique username registry) |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Package | `com.wakeus.app` |

---

## Prerequisites

- **JDK 17** — [Adoptium Temurin](https://adoptium.net/) (free)
- **Android Studio Hedgehog 2023.1.1** or later
- **Android SDK Platform 34** & **Build Tools 34.0.0**
- **Firebase Account** (free Spark plan — ₹0 budget)

---

## Opening and Building

### Opening in Android Studio
1. Open **Android Studio**.
2. Select **Open** → select `c:\QALBI` (containing `settings.gradle.kts`).
3. Allow Gradle to sync.

### Building via Terminal
```powershell
# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug
```
The output APK is generated at:
`app\build\outputs\apk\debug\app-debug.apk`

---

## Connecting Your Free Firebase Backend (₹0 Spark Plan)

1. Go to the [Firebase Console](https://console.firebase.google.com).
2. Create a project on the **Spark plan** (free, no credit card required).
3. Add an Android app with package name `com.wakeus.app`.
4. Download `google-services.json` and save it to `app/google-services.json`.
5. Under **Authentication > Sign-in method**, enable **Email/Password**.
6. Under **Firestore Database**, create a database and paste the contents of `firestore.rules` under the **Rules** tab, then click **Publish**.
7. Rebuild the app: `.\gradlew.bat assembleDebug`. The Google Services Gradle plugin will automatically bind your app to Firebase.

---

## Roadmap

| Feature | Phase | Status |
|---|---|---|
| Jetpack Compose UI & Navigation | Phase 1 | ✅ Complete |
| Firebase Authentication & Profiles | Phase 2 | ✅ Complete |
| Firestore Friend Pairing | Phase 3 | ✅ Complete |
| FCM Remote Wake-Up Communication | Phase 4 | ✅ Complete |
| Complete Alarm Engine | Phase 5 | ✅ Complete |
| Polish, Avatars & Production Release | Phase 6 | Upcoming |

---

## Budget

**₹0.** Every dependency, SDK, and cloud tier operates strictly within zero-cost limits.
