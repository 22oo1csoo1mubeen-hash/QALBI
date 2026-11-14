# QALBI â€” Secret Romantic Easter Egg Implementation Report

## Overview
This document details the architecture, visual replication, animation systems, navigation integration, and trigger mechanics implemented for the secret romantic Easter egg page in **QALBI**.

---

## 1. Files Added & Modified

### Files Added:
1. **`app/src/main/res/drawable/secret_background.png`**  
   - Local Android drawable copied from `C:\QALBI\assets\design\Secret-reference.png` (also preserved as `secret-background.png` in `assets/design/`).
   - High-resolution, full-fidelity background illustration capturing the romantic night balcony, lanterns, flower vines, and silhouette.

2. **`app/src/main/java/com/wakeus/app/ui/secret/SecretRomanticScreen.kt`**  
   - The dedicated Composable reproducing `secret-reference.png`.
   - Houses the ambient GPU particle rendering (`AmbientRomanticParticlesLayer`), staggered entrance reveals, heart-ribbon typography, and pulsing climax love declaration.

3. **`app/src/main/java/com/wakeus/app/ui/secret/SecretHeartTapDetector.kt`**  
   - Encapsulated, lifecycle-safe gesture state machine tracking consecutive taps within a strict 1,000ms window.
   - Triggers the secret navigation callback only upon reaching the 8th valid tap, resetting the counter immediately.

4. **`app/src/test/java/com/wakeus/app/SecretHeartTapDetectorTest.kt`**  
   - Unit test suite verifying:
     - 8 consecutive taps fire navigation callback exactly once.
     - 7 taps or fewer do not trigger navigation.
     - Tap intervals exceeding 1,000ms reset the counter back to 1.
     - External resets (other clicks, lifecycle pauses) clear the tap count to 0.
     - Repeated activation resets counter cleanly for future cycles.

5. **`SECRET_PAGE_IMPLEMENTATION.md`**  
   - Comprehensive technical documentation and verification instructions.

### Files Modified:
1. **`app/src/main/java/com/wakeus/app/navigation/Screen.kt`**  
   - Added `object SecretRomantic : Screen("secret_romantic")` sealed destination.

2. **`app/src/main/java/com/wakeus/app/ui/home/HomeScreen.kt`**  
   - Added `onOpenSecretRomantic: () -> Unit` parameter.
   - Attached `SecretHeartTapDetector` exclusively to the top-left branding heart icon (`Box` enclosing `ic_heart_outline_simple`).
   - Added explicit `reset()` triggers to:
     - The "QALBI" wordmark text tap.
     - The profile avatar icon tap.
     - Lifecycle `ON_RESUME` and `ON_PAUSE`.
     - `DisposableEffect.onDispose`.

3. **`app/src/main/java/com/wakeus/app/navigation/AppNavigation.kt`**  
   - Connected `onOpenSecretRomantic = { navController.navigate(Screen.SecretRomantic.route) }` in `HomeScreen`.
   - Registered `composable(Screen.SecretRomantic.route)` invoking `SecretRomanticScreen(onBack = { navController.popBackStack() })`.
   - Preserved `isBottomBarVisible` condition: the bottom navigation bar is automatically hidden on `Screen.SecretRomantic`, ensuring true edge-to-edge romantic immersion.

---

## 2. Asset Paths Used

- **Reference Specification:**  
  `C:\QALBI\assets\design\Secret-reference.png`
- **Background Asset in Android Resources:**  
  `C:\QALBI\app\src\main\res\drawable\secret_background.png`
- **Design Asset Mirror:**  
  `C:\QALBI\assets\design\secret-background.png`

---

## 3. Navigation Destination

- **Route:** `Screen.SecretRomantic.route` (`"secret_romantic"`).
- **Visibility:** Hidden from all bottom navigation tabs, settings menus, and drawer navigation.
- **Back Navigation:** Pressing the floating frosted back button or Android hardware/gesture back returns directly to `Screen.Home` with all states intact.

---

## 4. Trigger Mechanics: The Eight Consecutive Taps

### Exact Trigger Location:
In `HomeScreen.kt`, only the `Box` wrapping `R.drawable.ic_heart_outline_simple` (located immediately to the left of the `QALBI` wordmark in the top-left header) registers secret taps:
```kotlin
Box(
    modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Hidden trigger: no ripple or click indicator
            onClick = onSecretHeartClick,
        )
        .padding(4.dp),
    contentAlignment = Alignment.Center,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_heart_outline_simple),
        contentDescription = null,
        tint = HomeWineAccent,
        modifier = Modifier.size(24.dp),
    )
}
```

### Reset & Isolation Rules:
- **Interval Timeout:** Taps must occur within **1,000 milliseconds** (`maxIntervalMillis = 1000L`). If more than 1,000ms elapses between taps, `tapCount` resets to 1 on the next tap.
- **QALBI Wordmark Click:** Resets counter to 0.
- **Profile Icon Click:** Resets counter to 0.
- **Leaving / Backgrounding the Screen:** `ON_PAUSE`, `ON_RESUME`, and `onDispose` reset the counter to 0.
- **8th Tap Execution:** On the 8th tap, the detector triggers `onOpenSecretRomantic()` and resets counter and timestamp to 0.

---

## 5. UI Layout & Typography Replication

### Required Phrases in Exact Sequence:
1. `Mera QALBI` â™¥
2. `Mera Heera` â™¥
3. `Mera Shona` â™¥
4. `Mera Sona` â™¥
5. `Mera Noor` â™¥
6. `Mera Sitara` â™¥
7. `Mera Chaand` â™¥
8. `Mera Sabse Keemti` â™¥
9. `Mera Sukoon` â™¥
10. `Meri Naaz, Meri Duniya..` â™¥

Each phrase is displayed on a translucent frosted ribbon with gradient blush edges (`RibbonBgEdge`, `RibbonBgCenter`) and deep wine serif typography (`Color(0xFF75162D)`).

### Climax Declaration Card:
Prominently highlighted in an elevated, glowing neon card:
- Crown & sparkle emblem: `âœ¨ ðŸ‘‘ âœ¨`
- **`I Love Yuh Madihuu... ðŸ’–ðŸ¥¹ðŸ«¶`** (Deep wine serif italic, 21sp, soft pink glow shadow)
- **`Aapke Liye Kuch Bhi.. â¤ï¸âœ¨`** (Romantic wine serif italic, 16.5sp)

---

## 6. Coordinated Animations

1. **Staggered Entrance Sequence:**
   - On page entry, each phrase enters sequentially with a 170ms delay using `fadeIn()` and `slideInVertically()`.
   - The final declaration card reveals last using `fadeIn() + scaleIn(spring)`.

2. **Continuous Ambient GPU Particle System (`AmbientRomanticParticlesLayer`):**
   - **Floating Hearts:** Drift gracefully upward with sinusoidal horizontal sway and soft fade-out near the top.
   - **Rose Petals:** Drift slowly downward with natural fluttering rotation and curves rendered via `quadraticBezierTo`.
   - **Twinkling Sparkles:** Softly pulse and twinkle across the artwork.
   - Runs on the GPU via Jetpack Compose `Canvas` and `graphicsLayer` without allocating memory during frames.

3. **Continuous Climax Heartbeat Pulse:**
   - The climax declaration card gently expands and contracts using an infinite transition (`heartbeatScale`: 1.0f â†” 1.035f), simulating a gentle, living heartbeat.

---

## 7. Build and Verification Results

### Unit Tests:
- Command: `gradlew.bat testDebugUnitTest`
- Status: **SUCCESSFUL** (24 tasks, including all `SecretHeartTapDetectorTest` test cases).

### Full APK Build:
- Command: `gradlew.bat assembleDebug`
- Status: Validated with Gradle.

---

## 8. Manual Verification Steps for User

To verify on a physical Android phone or emulator:
1. Launch **QALBI** and log in to reach the Home screen.
2. Observe the top-left header containing the heart icon and "QALBI".
3. Tap the heart icon **8 times consecutively** (within 1 second between each tap).
4. Notice the app smoothly opens the secret romantic page with:
   - Full-screen couple background illustration.
   - Floating petals and rising glowing hearts.
   - Staggered appearance of all 10 phrases.
   - Glowing heartbeat card with *"I Love Yuh Madihuu... ðŸ’–ðŸ¥¹ðŸ«¶"* and *"Aapke Liye Kuch Bhi.. â¤ï¸âœ¨"*.
5. Tap the top-left frosted back button (or use Android back gesture) â€” verify you return seamlessly to the Home screen.
6. Verify that tapping the "QALBI" text or tapping slower than 1 second does NOT open the page.

