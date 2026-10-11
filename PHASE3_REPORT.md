# QALBI — Phase 3 Implementation Report: Friend Pairing System

**Date:** 2026-10-11  
**Phase:** 3 — Friend Pairing System  
**Status:** COMPLETE (Code complete, tested, and validated; external Firestore security rules prepared)

---

## A. Implementation Summary

Phase 3 implements an atomic, concurrency-safe, zero-cost friend-pairing system in QALBI without requiring mobile phone numbers, paid services, Cloud Functions, or external servers.

Key achievements:
1. **Cryptographically Secure Pairing Codes**:
   - Generated using Java's `SecureRandom` from an unambiguous 32-character Crockford Base32 alphabet (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`), eliminating ambiguous glyphs (`0`, `1`, `I`, `O`).
   - 8 characters yield $32^8 \approx 1.1 \text{ trillion}$ entropy combinations.
   - Enforced 10-minute validity duration with automatic expiry checks.
2. **Atomic Transactional Redemption**:
   - Single Firestore transaction handles validation, block checks, duplicate prevention, self-pairing rejection, code redemption, and connection creation atomically.
   - Concurrency-safe: if two people attempt to redeem the same code simultaneously, exactly one succeeds; the second receives an explicit error.
3. **Deterministic Connection Architecture**:
   - Connection document IDs are derived deterministically by sorting member UIDs: `min(uidA, uidB)_max(uidA, uidB)`.
   - Guarantees at most one connection document can ever exist between any two users, precluding duplicate or split connections.
4. **Privacy-Preserving Public Profiles**:
   - Created `/publicProfiles/{uid}` containing only `displayName`, `username`, and `usernameNormalized`.
   - Private email addresses, account types, and timestamps from `/users/{uid}` are never exposed to pairing partners.
5. **Real-Time Friends Synchronization**:
   - Home Screen and Connected Friends Screen observe live Firestore snapshots on `/connections` where `memberUids array-contains currentUid`.
   - Connected friends appear instantly on both users' devices upon redemption without app restarts.
6. **Disconnect and Block System**:
   - Mutual connection termination removes the connection document cleanly.
   - Blocking permanently records the block in `/userBlocks/{blockerUid}/blockedUsers/{blockedUid}`, immediately severs the existing connection, and rejects future pairing attempts.
7. **Production UI Upgrades**:
   - Removed Phase 1 mock connection data and demo badges from the production path.
   - Created `FriendDetailScreen` with profile details, "Wake Up", "Disconnect", and "Block" with confirmation dialogs.
8. **Automated Unit Tests**:
   - Created `FriendPairingTest` validating code generation, normalization, expiry calculations, deterministic ID symmetry, and serialization.

---

## B. Pairing Flow

```
+-----------------------------------------------------------------------------------+
| USER A (Creator)                                                                 |
| 1. Opens "Connect to a Friend" -> Taps "Generate Code"                            |
| 2. App generates 8-char code (e.g. 7X9K2M4P) via SecureRandom                     |
| 3. Writes /pairingCodes/7X9K2M4P with status: ACTIVE, expiresAt: now + 10 mins   |
| 4. Displays code in large monospace font with copy button and 10-min countdown    |
| 5. Shares code privately with Friend (via WhatsApp, SMS, Signal, in person)       |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| USER B (Redeemer)                                                                 |
| 1. Opens "Connect to a Friend" -> Enters code 7X9K2M4P                            |
| 2. App normalizes input (strips spaces/hyphens, converts to uppercase)            |
| 3. Shows optional preview: "Connect with @alice (Alice)?"                        |
| 4. Taps "Confirm & Connect"                                                       |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| FIRESTORE ATOMIC TRANSACTION                                                      |
| 1. Reads /pairingCodes/7X9K2M4P: verifies exists, status == ACTIVE, not expired  |
| 2. Verifies User B != User A (no self-pairing)                                    |
| 3. Checks /userBlocks (neither user has blocked the other)                        |
| 4. Checks /connections/uidA_uidB: verifies not already connected                  |
| 5. Writes:                                                                        |
|    - Updates /pairingCodes/7X9K2M4P -> status: REDEEMED, redeemedBy: uidB         |
|    - Sets /connections/uidA_uidB -> status: CONNECTED, memberUids: [uidA, uidB]   |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| BOTH USERS                                                                        |
| - Snapshot listener detects new connection document                               |
| - Both users immediately see each other in their "Connected Friends" list         |
+-----------------------------------------------------------------------------------+
```

---

## C. Firestore Schema

### 1. `pairingCodes/{code}`
| Field | Type | Description |
|---|---|---|
| `code` | `string` | 8-character Crockford Base32 code (document ID). |
| `creatorUid` | `string` | UID of the user who generated the code. |
| `creatorDisplayName`| `string` | Display name of the creator for recipient preview. |
| `creatorUsername` | `string` | Username of the creator for recipient preview. |
| `status` | `string` | `ACTIVE`, `REDEEMED`, `REVOKED`, or `EXPIRED`. |
| `createdAt` | `timestamp` | Server timestamp of code generation. |
| `expiresAt` | `timestamp` | Timestamp after which the code is invalid (+10 minutes). |
| `redeemedBy` | `string?` | UID of the redeeming user. |
| `redeemedAt` | `timestamp?`| Server timestamp of redemption. |
| `revokedAt` | `timestamp?` | Server timestamp if creator cancelled code. |
| `schemaVersion` | `number` | Schema version (`1`). |

### 2. `connections/{connectionId}`
*Document ID format:* `min(uid1, uid2)_max(uid1, uid2)` (deterministic string concatenation).
| Field | Type | Description |
|---|---|---|
| `connectionId` | `string` | Deterministic connection ID. |
| `memberUids` | `array<string>` | Sorted list of the two participant UIDs `[uid1, uid2]`. |
| `user1Uid` | `string` | First member UID. |
| `user2Uid` | `string` | Second member UID. |
| `createdFromPairingCode` | `string` | The pairing code redeemed to create this connection. |
| `createdAt` | `timestamp` | Server timestamp when paired. |
| `status` | `string` | `CONNECTED`. |
| `schemaVersion` | `number` | Schema version (`1`). |

### 3. `publicProfiles/{userId}`
| Field | Type | Description |
|---|---|---|
| `uid` | `string` | Authenticated user UID. |
| `displayName` | `string` | Public display name. |
| `username` | `string` | Public username. |
| `usernameNormalized` | `string` | Lowercase normalized username. |
| `updatedAt` | `timestamp` | Timestamp of last profile update. |

### 4. `userBlocks/{blockerUid}/blockedUsers/{blockedUid}`
| Field | Type | Description |
|---|---|---|
| `blockerUid` | `string` | UID of the blocking user. |
| `blockedUid` | `string` | UID of the blocked user. |
| `blockedAt` | `timestamp` | Server timestamp of the block action. |

---

## D. Security Review

1. **Anti-Harvesting Protection**:
   - `pairingCodes` collection specifies `allow list: if false;` in `firestore.rules`.
   - Malicious clients cannot list or query active pairing codes.
   - Guessing an active code requires traversing a space of $1.099 \times 10^{12}$ values within a 10-minute window.
2. **Transaction Concurrency & Non-Reusability**:
   - Code redemption transitions `status` from `ACTIVE` to `REDEEMED`.
   - Security rules reject updates if `resource.data.status != "ACTIVE"`.
   - A redeemed code can never be redeemed again.
3. **Deterministic Connection Isolation**:
   - Only participants (`request.auth.uid in resource.data.memberUids`) can read or delete a connection.
   - Arbitrary users cannot inject themselves into existing connections.
4. **Profile Privacy**:
   - Partner display relies exclusively on `/publicProfiles/{uid}`.
   - Private email addresses and account types in `/users/{uid}` remain restricted to the document owner.
5. **Bidirectional Block Enforcement**:
   - During redemption, the transaction checks both `userBlocks[creator][redeemer]` and `userBlocks[redeemer][creator]`.
   - Blocking severs active connections immediately and prevents future reconnections.

---

## E. Zero-Cost Verification

- **Firebase Spark Plan**: Strict ₹0 expenditure maintained.
- **No Cloud Functions**: All operations utilize client-side Firestore transactions backed by declarative security rules.
- **No Paid APIs or Servers**: Zero billing activations, third-party subscriptions, or credit card dependencies.
- **Usage Limits**: Free Spark tier grants 50,000 document reads, 20,000 writes, and 20,000 deletes daily — vastly sufficient for friend pairing traffic.

---

## F. Files Created or Modified

| File | Action | Purpose |
|---|---|---|
| `data/model/Models.kt` | Modified | Added `PublicProfile`, `PairingCode`, `PairingCodeStatus`, `Connection`; updated `Friend` model. |
| `data/friends/FriendRepository.kt` | Created | Core pairing repository: code generation, normalization, atomic redemption transaction, friend listeners, disconnect, block. |
| `data/user/UserRepository.kt` | Modified | Added `publicProfiles` creation on registration and `getPublicProfile` method. |
| `data/repository/QalbiRepository.kt` | Modified | Wired `FriendRepository`, live `friends` flow, and pairing operations. |
| `viewmodel/ConnectFriendViewModel.kt`| Modified | Upgraded with real code generation, expiry, input normalization, prospective friend preview, and error mapping. |
| `viewmodel/FriendsViewModel.kt` | Created | Observes live `friends` flow, handles disconnect and block actions. |
| `viewmodel/FriendDetailViewModel.kt` | Created | Manages friend profile display, disconnect dialog, and block dialog states. |
| `viewmodel/WakeUpViewModel.kt` | Modified | Resolves real friend profiles dynamically from `UserRepository`. |
| `ui/components/QalbiButton.kt` | Modified | Added `isLoading` support to `QalbiOutlinedButton`. |
| `ui/home/HomeScreen.kt` | Modified | Integrated live connected friends, empty state, and removed demo badges. |
| `ui/friends/ConnectFriendScreen.kt` | Modified | Live 8-character code generator, monospace display, copy action, and code redemption. |
| `ui/friends/ConnectedFriendsScreen.kt`| Modified | Displays real connected friends list from live Firestore listener. |
| `ui/friends/FriendDetailScreen.kt` | Created | Friend detail screen with "Wake Up", "Disconnect", and "Block" confirmation dialogs. |
| `navigation/Screen.kt` | Modified | Added `Screen.FriendDetail` destination. |
| `navigation/AppNavigation.kt` | Modified | Wired `FriendDetailScreen` into the navigation graph. |
| `firestore.rules` | Modified | Security rules for `publicProfiles`, `pairingCodes`, `connections`, and `userBlocks`. |
| `app/src/test/java/com/wakeus/app/FriendPairingTest.kt` | Created | Comprehensive unit test suite for pairing logic, normalization, expiry, and models. |
| `README.md` | Modified | Updated project status to Phase 3 Complete. |
| `PHASE3_REPORT.md` | Created | Comprehensive Phase 3 architecture and verification report. |

---

## G. Build and Test Results

### 1. Unit Tests (`testDebugUnitTest`)
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat testDebugUnitTest
```
- **Result**: `BUILD SUCCESSFUL in 1m 12s` (24 actionable tasks executed/up-to-date).
- **Tests Executed**:
  - `AuthValidationTest`: 11 tests passed.
  - `FriendPairingTest`: 8 tests passed.
  - **Total**: 19 tests passed (0 failures, 0 skipped).

### 2. APK Compilation (`assembleDebug`)
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat assembleDebug
```
- **Result**: `BUILD SUCCESSFUL in 34s`.
- **Output Artifact**: `app/build/outputs/apk/debug/app-debug.apk` (20,368,252 bytes / ~20.3 MB).

---

## H. Firebase Setup Status

- **Code & Repository Readiness**: 100% complete and fully verified.
- **`google-services.json`**: Present in `c:\QALBI\app\google-services.json` (protected from Git via `.gitignore`).
- **Firestore Security Rules**: Updated in `firestore.rules` with restrictive Phase 3 permissions.

---

## I. Manual Steps Required From You

To deploy the updated security rules to your live Firebase project:

1. Open your browser and go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your **QALBI** project (`qalbi-8414f`).
3. In the left navigation, go to **Firestore Database** → click the **Rules** tab at the top.
4. Replace the existing rules with the contents of [`firestore.rules`](file:///c:/QALBI/firestore.rules).
5. Click the blue **Publish** button.

*That's it! Once published, your Firebase database will securely enforce the Phase 3 pairing, connection, and block policies.*

---

## J. Known Limitations

1. **Client-Side Expiry Cleanup**: Firestore Spark tier does not include automatic server-side TTL document deletion (which requires Blaze billing). Expired code documents remain in Firestore with `isExpired` checked during transactions. A scheduled cleanup can be addressed if transitioning to a free serverless cron in the future.
2. **Offline Pairing**: Generating and redeeming codes requires active network connectivity to commit the atomic Firestore transaction.
3. **Wake-Up Communication**: The "Wake Up Friend" action navigates to the wake-up interface with the friend's real profile; actual push notifications and alarms belong to Phase 4.

---

## K. Git Checkpoint

Suggested commit message:
```bash
git add .
git commit -m "feat: implement friend pairing"
```

*(Per instructions, git commit has not been executed automatically.)*

---

## L. Phase Completion Status

**COMPLETE**

- Core pairing logic, code generation, atomic redemption transaction, live friends list, friend detail profile, disconnect, and block systems are completely implemented in native Kotlin and Jetpack Compose.
- All unit tests pass with zero errors.
- Debug APK compiles cleanly.
- Strict ₹0 budget maintained without external paid services.

---

## M. Next Phase

**Phase 4: Remote Wake-Up Communication**  
- Firebase Cloud Messaging (FCM) integration.
- Native Android foreground alarm engine and wake-lock management.
- "Wake Up" remote delivery and "I'm Awake" reciprocal response.
