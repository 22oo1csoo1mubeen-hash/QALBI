# QALBI — Native Android Application

> **"A little closer, every morning."**

QALBI (Arabic: قلبي, "my heart") is a native Android application that lets connected friends or loved ones wake each other remotely — without exchanging mobile numbers.

This repository contains the **Phase 1 foundation**: a complete, navigable native Android UI with a clean, extensible architecture ready for real backend integration in later phases.

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
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Package | `com.wakeus.app` |

---

## Prerequisites

- **JDK 17** (or JDK 11 at minimum) — [Adoptium Temurin](https://adoptium.net/) is recommended (free)
- **Android Studio Hedgehog 2023.1.1** or later (includes a bundled JDK and Android SDK)
- **Android SDK Platform 34**
- **Android SDK Build Tools 34.0.0**

> All tools above are free and open-source. No paid accounts or subscriptions are required.

---

## Opening in Android Studio

1. Clone or download this repository.
2. Open **Android Studio**.
3. Choose **Open** → select the `QALBI` root directory (the one containing `settings.gradle.kts`).
4. Android Studio will sync the project automatically. Allow it to download dependencies.
5. If prompted, accept the Android SDK license agreements.

---

## Building and Running

### Run on an emulator or device

```
# In Android Studio: Run ▶ (Shift+F10)
```

Or from the terminal (once Java/JDK is on your PATH):

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

### Generate a debug APK

```bash
.\gradlew.bat assembleDebug
```

The APK will be at:
```
app\build\outputs\apk\debug\app-debug.apk
```

### Run unit tests

```bash
.\gradlew.bat test
```

---

## Phase 1 Limitations

- **No real authentication.** Registration and login are local demos. No data is sent to any server. Passwords are validated in memory only and are never persisted.
- **No connected friends.** The friends list is intentionally empty. A clearly labelled demo friend is available for UI preview.
- **No real connection codes.** Generated codes are UI-only and are not redeemable on any server.
- **No remote wake-up.** The Wake-Up button simulates a send. No notification or alarm is triggered on any device.
- **Settings are not persisted.** Setting toggles reset on app restart.
- **No internet permission.** The app operates entirely offline in Phase 1.

---

## Features Deferred to Later Phases

| Feature | Phase |
|---|---|
| Firebase Authentication | Phase 2 |
| Firestore friend pairing | Phase 3 |
| Connection code server validation | Phase 3 |
| Firebase Cloud Messaging (FCM) | Phase 4 |
| Real remote wake-up alarm | Phase 4 |
| Alarm sound and vibration engine | Phase 4 |
| Notification permissions | Phase 4 |
| Profile photo upload | Phase 5 |

---

## Budget

₹0. Every dependency is free and open-source. No paid services, APIs, or accounts are used.
