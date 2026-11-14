# QALBI

QALBI is a native Android app for sending personal wake-up requests between connected friends. Android handles the alarm experience; Firebase stores accounts, connections, and requests; a trusted Node.js dispatcher sends remote FCM messages.

## Project layout

| Folder | Purpose |
|---|---|
| android/ | Android Studio project, app module, Gradle wrapper, and Android build configuration |
| backend/fcm-dispatcher/ | Trusted Firebase Admin process that sends FCM wake-up messages |
| firebase/ | Firestore security rules to publish in Firebase Console |
| assets/design/ | Original design references and source assets |
| docs/ | Architecture, setup, testing, release, and historical engineering notes |

## Build the Android app

Requirements: Android Studio, JDK 17, and Android SDK Platform 34.

Place your private Firebase Android configuration at android/app/google-services.json. This file is ignored by Git. Then, from the project root:

    cd android
    .\gradlew.bat assembleDebug

The debug APK is generated at android/app/build/outputs/apk/debug/app-debug.apk.

Run the unit tests with:

    cd android
    .\gradlew.bat testDebugUnitTest

## Firebase setup

1. Create or select the QALBI Firebase project.
2. Register the Android package com.wakeus.app and put its google-services.json at android/app/google-services.json.
3. Enable Email/Password Authentication and create Cloud Firestore.
4. Publish the rules from firebase/firestore.rules in the Firebase Console.

## Dispatcher setup

The dispatcher lives in backend/fcm-dispatcher/. It needs a Firebase service-account JSON for the same Firebase project. Keep that private key out of Git and out of the Android app.

For local development, copy .env.example to .env, place serviceAccountKey.json beside the dispatcher, then run:

    cd backend/fcm-dispatcher
    npm install
    npm start

For Render, configure a Web Service with root directory backend/fcm-dispatcher, build command npm install, and start command npm start. Add the service-account JSON as a Render Secret File named serviceAccountKey.json; set GOOGLE_APPLICATION_CREDENTIALS to /etc/secrets/serviceAccountKey.json.

Render's free Web Service can spin down when idle and may restart or exhaust the shared monthly free-hour allowance. A free uptime monitor can reduce idle spin-down, but this setup is best-effort and does not guarantee uninterrupted alarm delivery.

## Documentation

- [Architecture and alarm reliability](docs/architecture/ALARM_RELIABILITY_REPORT.md)
- [Moto G34 device guide](docs/architecture/MOTO_G34_TEST_GUIDE.md)
- [Manual test guide](docs/guides/MANUAL_TESTING_GUIDE.md)
- [Release checklist](docs/release/RELEASE_CHECKLIST.md)
- [Project overview](docs/project-overview.md)
- [Engineering reports](docs/reports/)

