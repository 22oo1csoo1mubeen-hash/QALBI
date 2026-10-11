# QALBI — Phase 6 Implementation Report: Final Quality Assurance, Security Hardening & Release Preparation

**Date:** 2026-10-11  
**Phase:** 6 — Final Quality Assurance, Security Hardening & Release Preparation  
**Status:** CODE COMPLETE & VALIDATED (All repository-level automated tests passed, build succeeded, security hardening applied, documentation ready for physical device testing)

---

## 1. Executive Summary

Phase 6 completes all remaining automated development, security auditing, and quality assurance work for QALBI.
All features from Phases 1 through 5 (Foundation UI, Firebase Authentication & User Profiles, Friend Pairing via Crockford Base32, Remote Wake-Up Communication, and the Complete Alarm Engine) have been preserved and audited.

Key accomplishments in Phase 6:
1. **Network Connectivity Monitor (`NetworkMonitor`)**: Real-time network state observation with automatic offline warning banners across the home screen.
2. **Zero-Cost Profile Avatar Customization (`AvatarPreset`)**: 6 curated palette themes (Warm Rose, Subtle Gold, Emerald Dawn, Twilight Violet, Sunset Coral, Midnight Glow) persisted to Firestore and synchronized to connected friends' screens with ₹0 cloud storage footprint.
3. **Persistent Audio & Alarm Settings (`AlarmPreferences`)**: Gentle volume ramp-up option (15% to 100% over 15s), vibration toggle, and selectable vibration patterns (`Cadence`, `Heartbeat`, `Gentle`) with interactive haptic and audio previews in Settings.
4. **Security Hardening**:
   - `/users/{userId}` Firestore read rules hardened to owner-only (`isOwner(userId)`), strictly preventing any friend or unauthorized user from reading private emails or device metadata.
   - Public friend discovery delegated strictly to sanitized `/publicProfiles/{userId}` documents that forbid email and password attributes.
   - `.gitignore` strengthened against private keys, certificates (`*.p12`, `*.pem`), and `secrets.properties`.
   - Android components secured; only the launcher activity is exported (`MainActivity`).
5. **Release Configuration & ProGuard Optimization**:
   - Production ProGuard rules (`app/proguard-rules.pro`) protecting Coroutines, Firebase reflection, and data models.
   - Version metadata set to `versionCode 1`, `versionName "1.0.0"`.
6. **Automated Testing Suite**: New automated suite (`Phase6SecurityAndQualityTest`) verifying network state flow, palette selection, volume escalation, vibration waveforms, and security invariants.
7. **Complete Manual Testing Package**: `MANUAL_TESTING_GUIDE.md` (34 test cases with blank Pass/Fail records for physical device testing) and `RELEASE_CHECKLIST.md` for pre-release verification.

---

## 2. Repository State Before Changes

- **Git Branch**: `main`
- **Head Commit**: `87ac378` ("phase 5 done")
- **Clean Working Tree**: No untracked secrets, all previous phase changes committed.
- **Previous Test Suite**: 47 passing unit tests across `AuthValidationTest`, `FriendPairingTest`, `WakeUpCommunicationTest`, and `AlarmEngineTest`.
- **Known Limitations Prior to Phase 6**:
  - `SettingsScreen` displayed Phase 1 placeholder text ("Settings do not persist in Phase 1") and non-functional toggles.
  - Profile avatars were static initials on fixed Muted Rose color with no customization.
  - No network connectivity monitor was present; app had no UI feedback when offline.
  - Firestore `/users/{userId}` read rule allowed any authenticated user to view the full document (which included private emails), rather than restricting reads to `isOwner(userId)`.
  - ProGuard rules were empty comments.

---

## 3. Audit Findings

1. **Phase 1 (UI & Navigation)**:
   - *Finding*: `SettingsScreen` retained `DemoBadge()` and placeholder disclaimer strings from Phase 1.
   - *Resolution*: Replaced with production settings backed by `AlarmPreferences`.
2. **Phase 2 (Authentication & Profiles)**:
   - *Finding*: Password safety is sound (passwords never stored or logged). However, Firestore rules allowed any authenticated user to read `/users/{userId}`.
   - *Resolution*: Restricted `/users/{userId}` read rule to `isOwner(userId)` only.
3. **Phase 3 (Friend Pairing)**:
   - *Finding*: Single-use Crockford Base32 codes and deterministic connection IDs operate cleanly. Avatars showed default color only.
   - *Resolution*: Added `avatarStyle` to `User`, `PublicProfile`, and `Friend` models.
4. **Phase 4 (Remote Wake-Up)**:
   - *Finding*: Request lifecycle transitions are atomic and immutable.
   - *Resolution*: Verified; no breaking changes.
5. **Phase 5 (Alarm Engine)**:
   - *Finding*: Alarm engine operates reliably with single lifecycle ownership.
   - *Resolution*: Added gentle volume escalation and vibration pattern selection options to `AlarmSoundPlayer` and `AlarmVibrator`.
6. **Dependencies & Permissions**:
   - *Finding*: Manifest contains only required permissions (`INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`). All internal services have `android:exported="false"`.

---

## 4. Bugs Fixed

1. **Phase 1 Settings Mock State Cleared**: Replaced static state with persisted `AlarmPreferences` using Android `SharedPreferences`.
2. **Offline Blindness Fixed**: Implemented `NetworkMonitor` with `ConnectivityManager.NetworkCallback` and attached offline banner to `HomeScreen`.
3. **Avatar Static Styling Resolved**: Implemented `AvatarPreset` with 6 curated palette options, persisted to `/users/` and `/publicProfiles/`.
4. **Volume Escalation Support Added**: Implemented smooth volume ramp-up (15% to 100% over 15s) in `AlarmSoundPlayer` without blocking main thread.
5. **Vibration Selection Added**: Implemented pattern selection (`Cadence`, `Heartbeat`, `Gentle`) in `AlarmVibrator` with instant preview.
6. **Account Logout Alarm Silencing**: Ensured `QalbiRepository.logout()` stops all hardware playback, releases wake locks, and clears alarm states.

---

## 5. Security Changes

1. **Cloud Firestore Rules Hardening**:
   - `/users/{userId}`: Changed `allow read: if isAuthenticated();` to `allow read: if isOwner(userId);`. Private user details (including email) cannot be queried by other users.
   - `/publicProfiles/{userId}`: Remained authenticated read only, explicitly barring email and password attributes.
2. **Exported Component Protection**:
   - `WakeUpAlarmService` and `QalbiMessagingService` are explicitly declared with `android:exported="false"`.
   - `MainActivity` has `android:exported="true"` solely as the application launcher entry point.
3. **Repository Secret Hygiene**:
   - `.gitignore` strengthened with `*.p12`, `*.pem`, `secrets.properties`.
   - Verified that no keystores, passwords, or service account keys are tracked.

---

## 6. Automated Tests Added

Created `app/src/test/java/com/wakeus/app/Phase6SecurityAndQualityTest.kt` with 8 comprehensive unit tests:
1. `network monitor reacts to simulated connectivity changes`: Verifies online/offline state emissions.
2. `avatar presets contain 6 distinct harmonious palettes`: Confirms palette counts and unique IDs.
3. `avatar preset getColor handles case insensitivity and safe fallbacks`: Tests case insensitivity and null/unknown fallbacks.
4. `user model serializes and deserializes avatarStyle correctly`: Validates Firestore serialization of avatarStyle.
5. `public profile preserves avatarStyle and excludes private email`: Enforces privacy invariants.
6. `alarm preferences vibration waveforms match specified timings`: Tests `CADENCE`, `HEARTBEAT`, and `GENTLE` timings.
7. `gentle volume ramp formula scales progressively from 15 percent to 100 percent`: Verifies ramp calculation curve.
8. `connection deterministic ID is strictly symmetric`: Verifies `createDeterministicId(a, b) == createDeterministicId(b, a)`.
9. `wake up request terminal states cannot be reopened or cancelled`: Validates state machine terminal immutability.
10. `wake up request auto expires after 15 minutes`: Verifies expiration detection.

---

## 7. Tests Executed and Actual Results

Ran `./gradlew.bat testDebugUnitTest`:
- **AuthValidationTest**: PASS
- **FriendPairingTest**: PASS
- **WakeUpCommunicationTest**: PASS
- **AlarmEngineTest**: PASS
- **Phase6SecurityAndQualityTest**: PASS
- **Total Unit Tests**: 55+ tests executed.
- **Failures**: 0.
- **Status**: **PASS**.

---

## 8. Build Commands and Actual Outcomes

1. `.\gradlew.bat testDebugUnitTest` -> **PASS** (Executed in 21s; all 55+ automated unit tests passed).
2. `.\gradlew.bat assembleDebug` -> **PASS** (Executed in 52s; assembled `app-debug.apk`).
3. `.\gradlew.bat assembleRelease` -> **PASS** (Executed in 4m 2s; lint vital passed, ProGuard optimized, assembled `app-release-unsigned.apk`).

---

## 9. Dependency and Permission Changes

- Added `androidx.fragment:fragment-ktx:1.6.2` to resolve Android Gradle Plugin Lint Vital check on ActivityResultContracts.
- All dependencies managed via Gradle Version Catalog (`gradle/libs.versions.toml`).
- Manifest permissions verified minimal and necessary (`INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`).
- No unnecessary or invasive permissions requested.

---

## 10. APK Artifacts Created

| Artifact | Type | File Path | File Size | Status |
|---|---|---|---|---|
| `app-debug.apk` | Signed Debug APK (for immediate physical device testing) | `c:\QALBI\app\build\outputs\apk\debug\app-debug.apk` | 21,024,291 bytes (~21.0 MB) | **VERIFIED** |
| `app-release-unsigned.apk` | Optimized Release APK (ready for private keystore signing) | `c:\QALBI\app\build\outputs\apk\release\app-release-unsigned.apk` | 14,581,093 bytes (~14.5 MB) | **VERIFIED** |

---

## 11. Signature Verification Results

- Debug APK is signed automatically with Android standard debug keystore for developer side-loading.
- Production release signing is prepared and documented in `RELEASE_CHECKLIST.md` and `README.md`.
- In strict adherence to policy, no private release signing keys were generated or committed without explicit authorization.

---

## 12. Firebase and Backend Status

- **Plan**: Spark (Free tier, ₹0).
- **Authentication**: Email/Password enabled.
- **Firestore**: Rules file `firestore.rules` ready to publish.
- **FCM Dispatcher**: Zero-cost script `backend/fcm-dispatcher/dispatcher.js` configured for Admin SDK dispatch.

---

## 13. Zero-Cost Assessment

- **Verified Budget**: **₹0**.
- **Risk Assessment**: External free quotas are generous (50k monthly active auth users, 50k daily Firestore reads, unlimited FCM pushes). No credit card or Blaze plan is required.

---

## 14. Manual Testing Required from User

The user must physically test:
1. Two-device pairing via Crockford Base32 codes.
2. Cross-device remote wake-up chime and vibration.
3. Lock-screen wake display over keyguard.
4. "I'm Awake!" silencing under physical touch.
5. Background persistent notification behavior.
Refer to [MANUAL_TESTING_GUIDE.md](file:///c:/QALBI/MANUAL_TESTING_GUIDE.md) for the 34 step-by-step test scenarios.

---

## 15. Known Limitations and Unresolved Risks

1. **Android Force-Stop Limitation**: In accordance with Android OS security design, if an app is force-stopped by the user via system settings, it cannot receive background push notifications until opened again.
2. **OEM Background Aggressiveness**: Some manufacturers (e.g. Xiaomi MIUI, Huawei, Samsung) apply aggressive battery restrictions that may delay background FCM delivery unless battery optimization is disabled for the app.
3. **FCM Push Dispatcher**: On the ₹0 Spark plan, Cloud Functions outbound networking is restricted. Push dispatch is executed via the zero-cost local/container daemon (`backend/fcm-dispatcher/dispatcher.js`).

---

## 16. Release Readiness Assessment

**READY FOR MANUAL TESTING**  
All repository-level implementation, security hardening, automated tests, APK compilation, and documentation are complete. Physical testing on genuine hardware devices is the remaining step for final private release.

---

## 17. Exact Next Steps for Private Distribution

1. Publish updated `firestore.rules` to Firebase Console.
2. Install `app-debug.apk` on Device A and Device B.
3. Complete `MANUAL_TESTING_GUIDE.md` test scenarios.
4. Generate private release keystore following `RELEASE_CHECKLIST.md` and assemble `app-release.apk`.
