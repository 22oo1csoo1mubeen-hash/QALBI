# QALBI — Native Android Application

> **"A little closer, every morning."**

QALBI (Arabic: قلبي, "my heart") is a native Android application that lets connected friends or loved ones wake each other remotely — without exchanging mobile numbers.

---

## Current Status: Phase 3 Complete (Friend Pairing System)

- **Phase 1**: Complete 10-screen foundation with Jetpack Compose & Material 3.
- **Phase 2**: Production-oriented Firebase Authentication & Cloud Firestore integration (Email & Username accounts, atomic claims).
- **Phase 3**: Atomic Friend Pairing System under strict ₹0 Spark plan.
  - Cryptographically secure 8-character single-use pairing codes (Crockford Base32, 10-minute expiry).
  - Concurrency-safe atomic redemption via Firestore transactions (`/pairingCodes/{code}`).
  - Deterministic bidirectional connection documents (`/connections/{connectionId}`).
  - Minimal public profile projections (`/publicProfiles/{uid}`) protecting email addresses.
  - Real-time connected friends listener on Home and Connected Friends screens.
  - Full Disconnect and Block functionality with mutual disconnection and blocklist enforcement.
  - Comprehensive unit test suite (`FriendPairingTest`).
  - Restrictive Firestore security rules (`firestore.rules`).

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
| Firestore Friend Pairing | Phase 3 | Upcoming |
| FCM Remote Wake-Up & Alarm Engine | Phase 4 | Upcoming |
| Profile Photos & Polish | Phase 5 | Upcoming |

---

## Budget

**₹0.** Every dependency, SDK, and cloud tier operates strictly within zero-cost limits.
