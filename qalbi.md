# QALBI — Project Specification

> قلبي — "My Heart" in Arabic

QALBI is a native Android application that lets connected friends or loved ones wake each other remotely — without exchanging mobile numbers.

## Vision
Build a premium, intimate, modern Android app that makes waking a loved one feel personal and warm — not clinical or robotic.

## Budget Constraint
**₹0 — Absolute.** No paid services, APIs, subscriptions, or paid libraries at any phase.

## Technology Stack
- Language: Kotlin
- UI: Jetpack Compose
- Design: Material 3
- Build: Gradle with Kotlin DSL
- Navigation: Navigation Compose
- Architecture: MVVM + Repository
- State: ViewModel + StateFlow
- Async: Kotlin Coroutines
- Min SDK: API 26 (Android 8.0)
- Package: `com.wakeus.app`

## Design Language
- Deep midnight blue (#0D1B2A) base
- Warm ivory (#F5EFE6) text
- Muted rose (#C87B8B) accent
- Subtle gold (#C9A84C) highlight
- Rounded cards, generous spacing, purposeful micro-animations
- Edge-to-edge layouts

## Phases

### Phase 1 — Foundation & UI (CURRENT)
Complete navigable UI with mock data. No real backend. See PHASE1_REPORT.md.

### Phase 2 — Firebase Authentication
Email/password sign-in. Session management. No friend pairing yet.

### Phase 3 — Friend Pairing
Connection code generation and redemption via Firestore. Real paired friends list.

### Phase 4 — Remote Wake-Up
Firebase Cloud Messaging. Android alarm engine. Notification permission. I'm Awake response.

### Phase 5 — Polish & Production
Profile photos, push delivery reliability, analytics, Play Store publication.
