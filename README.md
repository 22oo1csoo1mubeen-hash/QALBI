<p align="center">
  <img src="assets/brand/readme-banner.svg" alt="QALBI - A little closer, every morning" width="100%">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-E9AEBB?style=flat-square" alt="Platform: Android">
  <img src="https://img.shields.io/badge/Language-Kotlin-E9AEBB?style=flat-square" alt="Language: Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-E9AEBB?style=flat-square" alt="UI: Jetpack Compose">
  <img src="https://img.shields.io/badge/Backend-Firebase-E9AEBB?style=flat-square" alt="Backend: Firebase">
</p>

<p align="center"><em>A personal wake-up call between people who care about each other.</em></p>

## About QALBI

QALBI is a native Android app that lets connected friends send each other wake-up requests without sharing phone numbers. The recipient can answer the request from an incoming alarm screen.

## How it works

```text
Sender Android app
      | creates request
      v
Firebase Firestore -> QALBI FCM dispatcher -> Firebase Cloud Messaging
                                                   |
                                                   v
                                          Recipient Android alarm
```

The dispatcher is a trusted Node.js process. It reads pending Firestore requests and sends data-only FCM messages. The recipient app validates the request before starting the alarm.

## Features

- Private friend pairing with single-use connection codes
- Wake-up requests with acknowledgement, decline, cancellation, and expiry
- High-priority FCM delivery for background wake-up requests
- Alarm playback, vibration, and full-screen lock-screen presentation
- Account, notification, and background-delivery diagnostics
- Firestore rules that restrict access to request participants

## Repository layout

| Path | Contents |
|---|---|
| `android/` | Android Studio project, app module, and Gradle wrapper |
| `backend/fcm-dispatcher/` | Firebase Admin FCM dispatcher |
| `firebase/` | Firestore security rules |
| `assets/design/` | Design references and source artwork |
| `docs/` | Architecture, setup, testing, and release documentation |

## Build the Android app

### Requirements

- Android Studio
- JDK 17
- Android SDK Platform 34

Add your Firebase Android configuration as `android/app/google-services.json`. This project-specific file is ignored by Git and must not be committed.

From the repository root, build a debug APK:

```shell
cd android
./gradlew assembleDebug
```

On Windows PowerShell, use `.\gradlew.bat assembleDebug` after changing into `android/`.

The APK is created at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Configure Firebase

1. Create or select the Firebase project used by QALBI.
2. Register the Android app with package name `com.wakeus.app`.
3. Download its `google-services.json` and put it in `android/app/`.
4. Enable Email/Password Authentication and create a Cloud Firestore database.
5. Publish the rules in `firebase/firestore.rules` from the Firebase Console.

## Run the FCM dispatcher

The dispatcher needs a Firebase service-account key for the **same project** as the Android app. Keep that key private; never add it to Git or the APK.

### Local development

1. Copy `backend/fcm-dispatcher/.env.example` to `.env` in that folder.
2. Put the service-account JSON beside `dispatcher.js` as `serviceAccountKey.json`.
3. Run:

   ```shell
   cd backend/fcm-dispatcher
   npm install
   npm start
   ```

### Render free Web Service

Create a **Web Service** from this repository with:

| Render setting | Value |
|---|---|
| Root Directory | `backend/fcm-dispatcher` |
| Build Command | `npm install` |
| Start Command | `npm start` |
| Instance Type | `Free` |

Then, in **Environment -> Secret Files**, add:

- **Filename:** `serviceAccountKey.json`
- **Contents:** the entire downloaded Firebase service-account JSON file

Add this environment variable:

| Key | Value |
|---|---|
| `GOOGLE_APPLICATION_CREDENTIALS` | `/etc/secrets/serviceAccountKey.json` |

After deployment, the logs should include `Authenticated via Service Account JSON` and `Listening for incoming wake-up requests`. The health endpoint is `https://YOUR-SERVICE.onrender.com/health`.

Render's free service may spin down while idle, restart, or reach its shared monthly free-hour limit. A free uptime monitor checking `/health` every five minutes can reduce idle spin-down, but this is best-effort and does **not** guarantee uninterrupted alarm delivery.

## Important delivery limits

- A phone that is powered off cannot receive an internet push.
- Android force-stop prevents background message handling until QALBI is opened again.
- FCM acceptance does not prove that a device received or played the alarm.
- Delivery can be affected by connectivity, notification permissions, Android power management, and free-host interruptions.

## Documentation

- [Project overview](docs/project-overview.md)
- [Alarm reliability investigation](docs/architecture/ALARM_RELIABILITY_REPORT.md)
- [Moto G34 device guide](docs/architecture/MOTO_G34_TEST_GUIDE.md)
- [Manual testing guide](docs/guides/MANUAL_TESTING_GUIDE.md)
- [Release checklist](docs/release/RELEASE_CHECKLIST.md)
- [Documentation index](docs/README.md)

## Security

Never commit `.env`, `serviceAccountKey.json`, signing keys, or private credentials. The Android `google-services.json` is local project configuration and is ignored by Git in this repository.
