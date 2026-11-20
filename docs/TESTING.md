# Testing

## Current evidence and automated checks

On **2026-11-19**, the baseline in [TASKS.md](../TASKS.md#t-102-baseline-build--done) passed assemble and two template JVM unit tests. No lint, instrumented or blocker device results have been recorded. These documents do not establish compatibility.

Run from `app/` in PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

`lintDebug` and `connectedDebugAndroidTest` are **NOT RUN** in the baseline; the latter needs a connected device. `MainScreenTest` likely fails: it expects `Hello X!`, but `Greeting` renders `Hello X !`. This is a source observation, not a verified failure. T-301 schedules deletion of the template tests.

Planned JVM coverage focuses on positive and negative detector fixtures (viewer, entry points, normal videos, home feed, DMs, profiles and stories), stale/unknown snapshots, and policy loop prevention: one Back per entry, event bursts, cooldown, retry cap and package/window reset. Use an injected clock. Keep instrumented coverage to a minimal smoke test for launching the real UI and exercising onboarding/settings and service status once implemented; use manual device scenarios for target-app integration.

## Device setup (coordinator only)

Device/adb work is serialized to the coordinator. This is a future blocker-validation procedure: the current APK has no accessibility service.

1. Start the existing Google Play AVD in a separate PowerShell window:

   ```powershell
   emulator -avd Medium_Phone_API_36.1
   ```

2. After boot, install YouTube and Instagram from the Play Store. Use the owner's own test accounts, or YouTube signed out. Do not use another person's account or private content for evidence.
3. Build, then install the debug APK from `app/`:

   ```powershell
   .\gradlew.bat :app:assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   adb shell am start -n com.appblocker.app/com.example.appblocker.MainActivity
   ```

4. Once onboarding/service exist, accept the disclosure and enable the service in Android **Settings → Accessibility**. For Android 13+ sideloads, first use **App info → ⋮ → Allow restricted settings**. The current template cannot complete this step.

For **emulator tests only**, after the planned service is implemented and declared, adb can enable it instead. **The `settings put ... enabled_accessibility_services` command replaces the entire list of enabled services, disabling any others; use a dedicated test emulator and restore its prior configuration afterward.** The application ID and fully qualified planned service class differ:

```powershell
adb shell settings get secure enabled_accessibility_services
adb shell settings put secure enabled_accessibility_services com.appblocker.app/com.example.appblocker.service.BlockerAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

Record the original list before replacing it. A missing service, restricted settings or Advanced Protection can prevent activation; adb is not evidence that normal user onboarding works.

Record installed target-app versions for every device run:

```powershell
adb shell dumpsys package com.google.android.youtube | Select-String versionName
adb shell dumpsys package com.instagram.android | Select-String versionName
```

## Planned manual scenarios

These are expected results, **not passed checks**. Test A (viewer blocking) and B (entry-point masking) independently with the relevant per-app toggles. To exercise a path normally covered by B, temporarily disable B and leave A enabled; then enable B and check that the entry point is covered and taps are blocked without Back. The target layout must retain its gap.

| Scenario | Expected result |
|---|---|
| Normal YouTube video | Playback and navigation remain usable; no Back or unrelated mask. |
| Shorts via tab, home shelf, search, channel and external link, each separately. The YouTube app has no message path, so that path is N/A. | A leaves each confirmed viewer once and returns to the prior context without a loop; B masks verified visible entry points and consumes touches. |
| Instagram Reels via tab, feed, search/explore, profile, DM and external link (each separately) | A leaves each confirmed viewer once; DM-origin Reels return to the thread. B masks verified entry points without navigating Back. |
| DM threads and profiles, plus stories and normal feeds | Ordinary content remains usable; only verified short-form entry points may be masked with B enabled. |
| Rapid switching between target and unrelated apps | Masks disappear when context ends; policy resets and unrelated apps are unaffected. |
| User presses Back during detection | No extra navigation or repeated Back loop. |
| Protection toggled while target app is open | Each app/behavior toggle applies live; disabling masking clears overlays without restarting the target app. |
| Service disabled/re-enabled; App Blocker force-stopped | Masks disappear on shutdown; after explicit reopening/re-enabling, status is accurate and no stale mask or policy state survives. Record actual restart behavior. |
| Rotation | Old masks clear, new verified bounds align after rotation and unrelated UI stays usable. |
| Spanish locale | The same viewer/entry-point decisions work without matching text; normal Spanish UI stays unaffected. |
| Overlay cleanup on every exit path | No overlay remains after package change, window-state change, configuration/rotation change, toggle-off, `onInterrupt`, `onUnbind` or `onDestroy`; unknown/stale snapshots also clear masks. Exercise lifecycle callbacks where reproducible and list unavailable paths as NOT RUN. |

## Evidence and privacy

Record **date, device/AVD, Android version, target-app versions, locale, scenario and result**, plus toggles and exact checks used. Verified rows belong in `docs/COMPATIBILITY.md`, created on the first real verification; that file does not exist yet. Fixture or unit results never count as device validation. Record failures and unavailable scenarios honestly.

Make no zero-frame or “complete disappearance” claim without measurement. When measuring exposure, lag or gaps, use only the owner's own test accounts and record with:

```powershell
adb shell screenrecord /sdcard/appblocker-test.mp4
```

Stop with Ctrl+C, inspect the recording and record the measured result. Delete the recording afterward, including local copies. Remove the device copy with:

```powershell
adb shell rm /sdcard/appblocker-test.mp4
```

Redact UI dumps to IDs, classes, flags and bounds before retaining fixtures; exclude text, messages, credentials and titles. Delete raw dumps in the same session. Do not commit screenshots, recordings or raw dumps containing private content. Planned runtime diagnostics remain opt-in and limited to in-memory rule decisions; see [ARCHITECTURE.md](../ARCHITECTURE.md) and [ADR-0001](decisions/ADR-0001-accessibility-overlay-masking.md).
