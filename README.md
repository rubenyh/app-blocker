# App Blocker

An Android Kotlin app intended to block YouTube Shorts and Instagram Reels while keeping normal videos, messages, profiles and other app features usable. It targets the official, unmodified apps, with sideloaded APK distribution first.

## Current status

**Compose template plus planning only. Nothing blocks yet.** The app displays a greeting screen; service, detection, overlays, onboarding and settings are TODO. Follow [TASKS.md](TASKS.md) for status and [PRODUCT.md](PRODUCT.md) for scope and acceptance criteria. LinkedIn, TikTok, whole-app blocking, patched apps and root are non-goals.

Planned behaviors:

- **A — Reactive blocking:** detect an active Shorts/Reels viewer and press Back, with loop prevention.
- **B — Entry-point masking:** cover detected tabs, shelves and buttons with accessibility overlays that block touches. This is masking, not removal: the target layout does not reflow and the covered area remains a gap.

See [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md) and [ARCHITECTURE.md](ARCHITECTURE.md) for the design and limitations.

## Setup and build

Use Windows 11 and PowerShell with:

- JDK 17, with `JAVA_HOME` pointing to it.
- Android SDK with platform 37; the local SDK is `$env:LOCALAPPDATA\Android\Sdk` (`%LOCALAPPDATA%\Android\Sdk` in Windows notation).
- `adb` and `emulator` on PATH. The existing AVD is `Medium_Phone_API_36.1`, using an API 36.1 Google Play image.

From the repository root:

```powershell
cd app; .\gradlew.bat :app:assembleDebug
```

The Gradle root is `app/`; its single module is `app/app/`. The stack is AGP 9.0.1 with built-in Kotlin, Kotlin 2.3.20, Gradle wrapper 9.1.0, JVM toolchain 17, Compose BOM 2026.03.01, Material3 and Navigation3 1.0.1. `compileSdk` and `targetSdk` are 37; `minSdk` is 24. The namespace is `com.example.appblocker`; the application ID is `com.appblocker.app`.

## Install and run

Start the emulator in a separate PowerShell window:

```powershell
emulator -avd Medium_Phone_API_36.1
```

After Android boots, run from `app/`:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.appblocker.app/com.example.appblocker.MainActivity
```

The installed app currently opens the template UI. See [docs/TESTING.md](docs/TESTING.md) for checks and future device scenarios.

## Permissions (planned)

One accessibility service will read limited UI metadata from the two target apps, navigate Back from viewers and manage overlays. Onboarding will explain this and require affirmative consent before opening Accessibility settings. The service is not implemented or available to enable yet.

For sideloaded apps on Android 13+, open **App info → ⋮ → Allow restricted settings** before enabling the accessibility service. Advanced Protection and sensitive views can prevent the planned approach; see [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md).

## Validation and troubleshooting

The recorded baseline on **2026-11-19** passed `:app:assembleDebug :app:testDebugUnitTest`, including two template unit tests. Lint and instrumented/device tests have **not been run**; this is no evidence of working blocking. Details and a known likely template test failure are in [docs/TESTING.md](docs/TESTING.md).

- **compileSdk warning:** AGP 9.0.1 reports testing only through compileSdk 36.1 while this project uses 37. The baseline build succeeded; no dependency change or warning suppression was needed for that result.
- **First build:** the wrapper downloads Gradle 9.1.0 on first use; allow time and network access for the download and dependencies.
- **Accessibility switch unavailable after sideloading:** once the service exists, complete the restricted-settings step above before trying again.

Read [CONTRIBUTING.md](CONTRIBUTING.md) before making changes.
