---
name: android-validation
description: Run this project's Gradle checks and device verification and report results. Use before declaring any code task done.
shell: powershell
---

# android-validation

Run the checks, then report each result separately. Never merge results into one "passed".

## 1. Gradle checks
From `app/`:
```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```
- `assembleDebug` and `testDebugUnitTest` were verified on 2026-11-19.
- `lintDebug` exists through AGP but has not been run on this project yet. Do not assume it passes. Report the actual result.
- A non-zero exit is FAIL. Quote the shortest decisive error line.

## 2. Check for a device
```powershell
adb devices
```
No device listed: Device is NOT RUN. Stop here. Do not start the emulator unless the brief says you own it (the coordinator owns adb and the emulator).

## 3. Device steps (only if a device is listed)
1. Install:
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
2. Enable the service. Once implemented, for emulator test runs only, you may use:
   ```powershell
   adb shell settings put secure enabled_accessibility_services com.appblocker.app/com.example.appblocker.service.BlockerAccessibilityService
   adb shell settings put secure accessibility_enabled 1
   ```
   Warning: this replaces any other enabled accessibility services on that device. Never run it on a personal device. The service class does not exist yet; skip this step until it does.
3. Run the relevant scenarios from `docs/TESTING.md`.
4. Record the app versionName, Android version and what was observed. Observed behavior goes in `docs/COMPATIBILITY.md` via `docs-sync`.

Optional help: the installed `android-cli` skill covers emulator control, screenshots and UI inspection.

## 4. Output
Exactly three lines:
```
Build: PASS|FAIL|NOT RUN - <reason>
Unit tests: PASS|FAIL|NOT RUN - <reason>
Device: PASS|FAIL|NOT RUN - <reason>
```
- Lint failures go in the Build line.
- Fixture-based tests count only toward Unit tests. They are never Device evidence.
- Every NOT RUN needs a reason.
