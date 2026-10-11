# QALBI — Pre-Release Verification & Distribution Checklist

> This checklist outlines all critical criteria required prior to distributing QALBI for private or internal testing.

---

## 1. Automated Repository & Build Verification

- [ ] **Gradle Clean Compilation**: `./gradlew.bat compileDebugKotlin` succeeds without errors or deprecation blockers.
- [ ] **Automated Test Suite**: `./gradlew.bat testDebugUnitTest` runs with 100% pass rate across all test suites (`AuthValidationTest`, `FriendPairingTest`, `WakeUpCommunicationTest`, `AlarmEngineTest`, `Phase6SecurityAndQualityTest`).
- [ ] **Debug APK Generation**: `./gradlew.bat assembleDebug` builds `app\build\outputs\apk\debug\app-debug.apk`.
- [ ] **Release Configuration Verified**: `app/build.gradle.kts` specifies `versionCode 1`, `versionName "1.0.0"`, `minSdk 26`, and `targetSdk 34`.
- [ ] **ProGuard & R8 Configuration**: `app/proguard-rules.pro` contains preservation rules for Coroutines, Firebase, and `com.wakeus.app.data.model.**`.

---

## 2. Security Hardening & Zero-Cost Audit

- [ ] **₹0 Budget Verification**: Project runs exclusively under Firebase Spark free tier; Blaze billing is NOT enabled.
- [ ] **No Client Credential Exposure**: Neither `google-services.json`, Firebase service account private keys, nor signing passwords are committed to Git.
- [ ] **Git Repository Hygiene**: Working tree is clean; `.gitignore` blocks `*.jks`, `*.keystore`, `*.key`, `*.p12`, `*.pem`, `serviceAccountKey.json`, and `secrets.properties`.
- [ ] **Firestore Security Rules Published**: `firestore.rules` published in Firebase Console with:
  - `/users/{userId}`: Owner-only read and write (`isOwner(userId)`).
  - `/publicProfiles/{userId}`: Authenticated read; owner-only write with email/password forbidden.
  - `/usernames/{username}`: Atomic creation if not exists; no updates; owner deletion only.
  - `/pairingCodes/{code}`: Single lookup only (no list queries); creator or single-redeemer update only.
  - `/connections/{connectionId}`: Member-only read and delete; 2-member creation only.
  - `/wakeUpRequests/{requestId}`: Sender/receiver only; valid status transitions (`ACKNOWLEDGED`, `DECLINED`, `CANCELLED`, `EXPIRED`).
  - Default deny for all other collections.
- [ ] **Component Export Safety**: `AndroidManifest.xml` exports only `MainActivity` (launcher); internal background service `WakeUpAlarmService` and `QalbiMessagingService` are explicitly `android:exported="false"`.

---

## 3. Remote Push Notification Infrastructure

- [ ] **FCM Device Registration**: FCM token repository registers device token under `/users/{uid}/fcmTokens/{tokenId}` upon login.
- [ ] **Push Dispatcher Status**: Trusted zero-cost FCM dispatcher (`backend/fcm-dispatcher/dispatcher.js`) configured:
  - Downloaded `serviceAccountKey.json` placed in `backend/fcm-dispatcher/` (local execution or free container).
  - Service account key verified NOT tracked in Git.
  - Dispatcher listens in real-time to Firestore `/wakeUpRequests/` and dispatches high-priority pushes.

---

## 4. Physical Two-Device Validation (User Action)

- [ ] **Installation**: App installs cleanly on Device A and Device B.
- [ ] **Two-Account Registration**: Two distinct accounts created without conflict.
- [ ] **Pairing**: 8-character Crockford Base32 code generated on Device A and redeemed on Device B; connection verified on both friends lists.
- [ ] **Wake-Up Transmission**: Device A sends wake-up request; Device B receives and displays alert.
- [ ] **Alarm Playback**: Original acoustic chime loops continuously with escalating rhythmic vibration.
- [ ] **I'm Awake Silencing**: Tapping "I'm Awake!" silences hardware immediately (<100ms) and updates Device A in real-time.
- [ ] **Lockscreen Presentation**: Device B wakes display when screen is turned off and locked; presents full-screen alarm over keyguard.
- [ ] **Background Execution**: Persistent foreground notification presents during background playback.

---

## 5. Release Packaging & Signing (For Private Distribution)

To produce a private release-signed APK:
1. Generate your private release keystore locally on your workstation:
   ```powershell
   keytool -genkey -v -keystore qalbi-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias qalbi
   ```
2. Place `qalbi-release.jks` in a secure location outside the repository (or in project root, protected by `.gitignore`).
3. Define signing properties in `secrets.properties` (ignored by Git) or supply via Gradle command line:
   ```powershell
   .\gradlew.bat assembleRelease -Pandroid.injected.signing.store.file=../qalbi-release.jks -Pandroid.injected.signing.store.password=YOUR_PASS -Pandroid.injected.signing.key.alias=qalbi -Pandroid.injected.signing.key.password=YOUR_PASS
   ```
4. Output release APK: `app\build\outputs\apk\release\app-release.apk`.
5. Verify APK signature using `apksigner`:
   ```powershell
   apksigner verify --verbose app\build\outputs\apk\release\app-release.apk
   ```

---

## 6. Final Go / No-Go Decision Gate

- **Automated Checks**: [PASS / FAIL]
- **Firestore Rules**: [PASS / FAIL]
- **Manual Physical Device Tests**: [PASS / FAIL]
- **Zero-Cost Constraint**: [PASS / FAIL]
- **Final Release Decision**: [GO / NO-GO]
