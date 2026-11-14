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

## How the application works

### 1. Set up accounts and connect friends

Each person installs QALBI, creates an account, and signs in through Firebase Authentication. One person generates a temporary, single-use pairing code; the other enters and redeems it. Firestore records the connection so QALBI can show them as friends.

### 2. Create a wake-up request

The sender selects a connected friend, taps **Wake Up**, and confirms. Before saving the request, QALBI checks that the friend connection is active and that the request is allowed. It then writes a `PENDING` request with sender and recipient details and an expiry time to Cloud Firestore. The sender's app watches that request for status changes.

### 3. Deliver the request to the recipient

The recipient app registers its Firebase Cloud Messaging (FCM) device token with Firestore. For background delivery, the trusted Node.js dispatcher must be running on its host:

1. The dispatcher watches Firestore for pending wake-up requests.
2. It verifies the connection and block status, then finds the recipient's registered FCM tokens.
3. It sends a high-priority, data-only push through Firebase Cloud Messaging.
4. QALBI on the recipient's phone checks that the message is for the signed-in user and has not expired.
5. If valid, QALBI starts its alarm foreground service, which plays the alarm and shows the alarm notification.

If QALBI is open on the recipient's phone, its active Firestore listener can also show incoming request state. The dispatcher and FCM are the route for waking the app when it is in the background.

### 4. Respond and finish the request

The recipient can acknowledge or decline from the alarm screen. That response updates the request in Firestore, and the sender sees the new status. The sender can cancel a pending request; requests also expire after 15 minutes. When a request is acknowledged, declined, cancelled, or expires, QALBI stops the corresponding alarm.

### Application architecture

- **Compose screens** display sign-in, friends, pairing, wake-up, incoming alarm, and settings flows.
- **ViewModels** manage each screen's state and user actions.
- **Repositories** validate operations and read or write Firebase data.
- **Firebase Authentication and Firestore** manage identity, connections, tokens, and request state.
- **FCM dispatcher** performs privileged server-side push delivery; its service-account key is never stored in the Android app.
- **Alarm components** validate incoming messages and manage alarm audio, vibration, notification actions, and foreground-service lifetime.

```text
Sender app -> Firestore request -> FCM dispatcher -> Firebase Cloud Messaging
                                                           |
                                                           v
                                             Recipient QALBI alarm
                                                           |
                                      acknowledge / decline / expire
                                                           v
                                         Firestore -> sender status
```

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
