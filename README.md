# BioVault

A fully offline, end-to-end encrypted password manager for Android — built with **Jetpack Compose** and **pure Kotlin**. No cloud sync, no third-party password databases, no bridge overhead. Your credentials never leave your device.

---

## Features

### Core Vault
- **Credential entries** — store site, username, password, TOTP secret, and notes per entry
- **Password generator** — configurable length (8–64), character sets (upper, lower, digits, symbols), ambiguous character exclusion
- **Entropy display** — real-time Shannon entropy calculation with color-coded label (Terrible → Overkill)
- **Strength meter** — animated 5-segment bar with zxcvbn-style scoring
- **Search & filter** — instant search across site, username, and notes via `SnapshotStateList` + `LazyColumn`
- **TOTP live codes** — RFC 6238 compliant 6-digit OTP with animated countdown ring, pure `javax.crypto` — no library

### Security Features
- **Biometric authentication** — `BiometricPrompt` API with Android Keystore-backed key
- **Auto-lock** — `ProcessLifecycleOwner` detects app backgrounding → locks after configurable grace period
- **Screen capture block** — `WindowManager.FLAG_SECURE` applied via Compose `DisposableEffect`, toggleable in settings
- **Clipboard auto-clear** — copies trigger a 30-second `CountDownTimer`; clipboard wiped on expiry, lock, or app close
- **Failed attempt lockout** — exponential backoff (30s, 60s, 120s…) persisted across app kills in `EncryptedSharedPreferences`
- **Biometric key invalidation** — detects `KeyPermanentlyInvalidatedException` when new fingerprint is enrolled; guides user through safe key reset without data loss

---

## Security Architecture

| Layer | Mechanism |
|-------|-----------|
| At-rest encryption | `EncryptedSharedPreferences` (AES-256-GCM) |
| Key protection | Android Keystore — hardware-backed where available |
| Auth gate | `BiometricPrompt` with `BIOMETRIC_STRONG` |
| Session | In-memory `StateFlow` — cleared on lock/kill |
| Screenshots | `FLAG_SECURE` on every window |
| Clipboard | Auto-wiped after 30 seconds |
| Lockout | Exponential backoff, survives process death |
| Key rotation | Automatic on new biometric enrollment |

---

## Tech Stack

| Category | Technology |
|----------|------------|
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin (100%) |
| Architecture | MVVM — `ViewModel` + `StateFlow` + `SnapshotStateList` |
| Encryption | `EncryptedSharedPreferences` + Android Keystore |
| Biometrics | `androidx.biometric:biometric` |
| TOTP | RFC 6238 — pure `javax.crypto.Mac` (HmacSHA1) |
| Lifecycle | `ProcessLifecycleOwner` for app-level auto-lock |
| Serialization | `kotlinx.serialization` (JSON) |
| Concurrency | Kotlin Coroutines + `Handler(mainLooper)` |
| Min SDK | API 29 (Android 10) |

---

## Security Considerations

- **No cloud dependency** — all data stays on-device inside `EncryptedSharedPreferences`
- **No master password** — the Android Keystore key is the only gate; it requires biometric on every unlock
- **Key invalidation by design** — enrolling a new fingerprint destroys the old Keystore key immediately; the user must re-authenticate and generate a fresh key
- **Memory safety** — vault data is never held in plaintext in a `ViewModel` field; `AppLockState` clears the auth gate in-memory on every background event
- **Clipboard hygiene** — password copies are auto-cleared after 30 seconds and immediately on lock or app close
- **Screenshot protection** — `FLAG_SECURE` is active on every screen via `DisposableEffect`; can be toggled in Settings for accessibility needs
- **Brute force resistance** — exponential backoff lockout is persisted to encrypted storage, surviving process kill and device restart

---

## Requirements

| Requirement | Value |
|-------------|-------|
| Min Android version | 10 (API 29) |
| Target Android version | 14 (API 34) |
| Language | Kotlin 1.9+ |
| Build system | Gradle with Kotlin DSL |
| Biometric hardware | Required for unlock (fingerprint or face) |
| Internet permission | Not required — fully offline |