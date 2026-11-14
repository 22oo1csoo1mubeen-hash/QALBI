# QALBI project overview

## Purpose

QALBI lets connected friends send each other a wake-up request without sharing mobile numbers. The receiving Android device validates the incoming request, plays the alarm, and lets the recipient acknowledge or decline it.

## Architecture

- Android app: Kotlin, Jetpack Compose, MVVM, repositories, coroutines, and StateFlow. Source is under android/app/src.
- Firebase Authentication: Owns user sessions.
- Cloud Firestore: Stores profiles, pairings, connections, blocks, tokens, and wake-up request state. Security rules are in firebase/firestore.rules.
- FCM dispatcher: A trusted Node.js process in backend/fcm-dispatcher observes pending requests and sends high-priority, data-only FCM messages.
- Alarm receiver: QalbiMessagingService validates the recipient and request expiry, then starts the alarm foreground service and full-screen notification.

The dispatcher must run on a connected host for remote wake-up delivery while the receiving app is in the background. The Android app alone cannot use Firebase Admin credentials to send privileged messages.

## Main Android packages

- data/auth, data/user, data/friends, and data/wakeup: Firebase-facing repositories and application data.
- data/fcm: FCM token registration, message validation, and alarm diagnostics.
- data/alarm: alarm playback, vibration, wake locks, notification actions, and foreground-service lifecycle.
- ui/: Compose screens and reusable UI components.
- viewmodel/: screen state and user actions.
- navigation/: app routes and top-level navigation.

## Operational limits

FCM delivery depends on network access, Firebase/Google Play services, Android notification permissions, and device power-management behavior. A powered-off phone cannot receive a push. Android force-stop suppresses background delivery until the user launches the app again. Free hosting tiers can also restart or suspend the dispatcher.


