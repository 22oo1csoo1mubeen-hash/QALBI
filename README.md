# QALBI — Native Android Application

> **"A little closer, every morning."**  
> قلبي — *"My Heart"* in Arabic

QALBI is a native Android application that lets connected friends or loved ones wake each other remotely — without exchanging mobile numbers.

---

## Current Status: Phase 6 Complete (Quality Assurance, Security Hardening & Release Preparation)

- **Phase 1 — Foundation & UI**: Complete 10-screen foundation with Jetpack Compose & Material 3 (Midnight Blue `#0D1B2A`, Muted Rose `#C87B8B`, Subtle Gold `#C9A84C`, Warm Ivory typography).
- **Phase 2 — Firebase Authentication & User Profiles**: Production-oriented Firebase Authentication & Cloud Firestore integration (Email & Username accounts, atomic username claims in `/usernames/`, password security, session isolation).
- **Phase 3 — Friend Pairing System**: Atomic Friend Pairing under strict ₹0 Spark plan (8-character Crockford Base32 codes in `/pairingCodes/`, deterministic `/connections/{uidA_uidB}` documents, blocklists in `/userBlocks/`).
- **Phase 4 — Remote Wake-Up Communication**:
  - Atomic Firestore request lifecycle (`/wakeUpRequests/{requestId}`) with statuses `PENDING`, `ACKNOWLEDGED`, `DECLINED`, `CANCELLED`, `EXPIRED`.
  - Recipient interaction screen (`IncomingRequestScreen`) with "☀️ I'm Awake!" and "Decline" actions.
  - Sender real-time status tracking (`WakeUpScreen`) with live state feedback and cancellation.
  - Home Screen active incoming wake-up alert banner.
  - FCM receiving service (`QalbiMessagingService`) and high-priority notification channel (`qalbi_wake_up_channel`).
  - Android 13+ `POST_NOTIFICATIONS` runtime permission launcher and deep-link handling.
  - Trusted zero-cost FCM push dispatcher (`backend/fcm-dispatcher/dispatcher.js`) using Firebase Admin SDK.
- **Phase 5 — Complete Alarm Engine**:
  - Looping native alarm audio playback with dedicated `USAGE_ALARM` audio focus and original bundled chime asset (`res/raw/qalbi_alarm.wav`).
  - Controlled rhythmic vibration with hardware capability detection (`AlarmVibrator`).
  - Android Foreground Service (`WakeUpAlarmService`) with persistent notification and full-screen intent.
  - Lockscreen wake-up presentation via `setShowWhenLocked`, `setTurnScreenOn`, and `USE_FULL_SCREEN_INTENT`.
  - Immediate hardware silencing upon tapping "I'm Awake!" with atomic Firestore status confirmation.
  - Remote cancellation and expiry observer: stops ringing immediately if sender cancels or request expires.
- **Phase 6 — Quality Assurance, Security Hardening & Release Preparation**:
  - Real-time network connectivity monitoring (`NetworkMonitor`) with offline banner feedback.
  - Zero-cost profile avatar customizer (6 harmonious palette presets: Warm Rose, Subtle Gold, Emerald Dawn, Twilight Violet, Sunset Coral, Midnight Glow) synchronized across friend cards without third-party storage fees.
  - Persistent Alarm & Audio preferences (`AlarmPreferences`): gentle volume ramp-up (15% to 100% over 15s), vibration toggle, and selectable vibration patterns (`Cadence`, `Heartbeat`, `Gentle`) with instant haptic preview.
  - Firestore security rule hardening: `/users/{userId}` locked to owner-only read (`isOwner(userId)`) protecting emails and private tokens from friend exposure; public discovery delegated exclusively to sanitized `/publicProfiles/{userId}`.
  - Production ProGuard & R8 configuration (`app/proguard-rules.pro`) protecting Coroutines, Firebase reflection, and data models.
  - Release build readiness: `versionCode 1`, `versionName "1.0.0"`, `targetSdk 34`.
  - Complete Physical Device Manual Testing Guide (`MANUAL_TESTING_GUIDE.md`) and Pre-Release Checklist (`RELEASE_CHECKLIST.md`).
  - Comprehensive automated unit test suite with 100% pass rate.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (Material 3) |
| Design Tokens | Midnight Blue (`#0D1B2A`), Muted Rose (`#C87B8B`), Subtle Gold (`#C9A84C`) |
| Build System | Gradle 8.6 with Kotlin DSL |
| Navigation | Navigation Compose 2.7.7 |
| Architecture | MVVM with Repository boundary |
| State | ViewModel + StateFlow / Flow |
| Concurrency | Kotlin Coroutines (Supervised Jobs, Dispatchers.IO / Main) |
| Authentication | Firebase Authentication (Email/Password & synthetic username accounts) |
| Database | Cloud Firestore (Strict owner isolation, atomic username claims) |
| Push Routing | Firebase Cloud Messaging (FCM tokens registered per authenticated device) |
| Alarm Engine | Android MediaPlayer (`USAGE_ALARM`), Foreground Service, VibratorManager |
| Lockscreen | `showWhenLocked`, `turnScreenOn`, `USE_FULL_SCREEN_INTENT` |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Package Name | `com.wakeus.app` |

---

## Prerequisites

- **JDK 17** — [Adoptium Temurin 17](https://adoptium.net/) (free)
- **Android Studio Hedgehog 2023.1.1** or newer
- **Android SDK Platform 34** & **Build Tools 34.0.0**
- **Firebase Account** (free Spark plan — ₹0 budget)

---

## Building and Testing Locally

### Run Automated Unit Tests
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat testDebugUnitTest
```

### Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The output APK is generated at:
`app\build\outputs\apk\debug\app-debug.apk`

### Build Private Signed Release APK
To assemble a production-ready signed APK:
1. Generate your release keystore locally:
   ```powershell
   keytool -genkey -v -keystore qalbi-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias qalbi
   ```
2. Build the signed release bundle:
   ```powershell
   .\gradlew.bat assembleRelease -Pandroid.injected.signing.store.file=qalbi-release.jks -Pandroid.injected.signing.store.password=YOUR_PASS -Pandroid.injected.signing.key.alias=qalbi -Pandroid.injected.signing.key.password=YOUR_PASS
   ```
The signed APK will be at:
`app\build\outputs\apk\release\app-release.apk`

---

## Free Firebase Backend Setup (₹0 Spark Plan)

1. Navigate to the [Firebase Console](https://console.firebase.google.com).
2. Create a project on the default **Spark plan** (free, no credit card required).
3. Add an Android app with package name `com.wakeus.app`.
4. Download `google-services.json` and save it to `app/google-services.json` (protected by `.gitignore`).
5. Under **Authentication > Sign-in method**, enable **Email/Password**.
6. Under **Firestore Database**, create a database and paste the contents of `firestore.rules` under the **Rules** tab, then click **Publish**.
7. Rebuild the app: `.\gradlew.bat assembleDebug`.

---

## Remote Push Dispatcher (Zero-Cost Daemon)

To dispatch high-priority FCM push notifications when wake-up requests are created:
1. Open **Firebase Console -> Project Settings -> Service accounts**.
2. Click **Generate new private key** and save the JSON file as:
   `backend/fcm-dispatcher/serviceAccountKey.json` (protected by `.gitignore`).
3. Run the zero-cost dispatcher daemon:
   ```bash
   cd backend/fcm-dispatcher
   npm install
   npm start
   ```
*Note:* The dispatcher operates in a trusted Node.js environment on your local machine or any free container runner. Private credentials never enter the Android APK.

---

## Manual Testing & Release Guides

- Detailed physical device manual testing procedures: [MANUAL_TESTING_GUIDE.md](file:///c:/QALBI/MANUAL_TESTING_GUIDE.md)
- Pre-release gate verification: [RELEASE_CHECKLIST.md](file:///c:/QALBI/RELEASE_CHECKLIST.md)
- Full Phase 6 Engineering Report: [PHASE6_REPORT.md](file:///c:/QALBI/PHASE6_REPORT.md)

---

## Strict Zero-Cost Assessment

**₹0 Total Cost.**
- Firebase Authentication: Free tier covers up to 50,000 monthly active users.
- Cloud Firestore: Free tier provides 50,000 daily reads, 20,000 daily writes, and 1 GiB storage.
- Firebase Cloud Messaging: 100% free with unlimited push notifications.
- Audio Asset: Original acoustic chime created specifically for QALBI; zero copyright royalties or external licensing fees.
- Zero paid SDKs, zero third-party cloud buckets, zero subscription requirements.
