# ADR-0001: AccessibilityService with overlay masking on official apps

- **Status:** Accepted (2026-11-19)
- **Deciders:** project owner
- **Supersedes:** none

## Context

The product must block YouTube Shorts and Instagram Reels while the rest of each app keeps working. It has two behaviors:

- **A, reactive blocking:** detect entry into the short-form viewer and leave it.
- **B, interface removal:** make Shorts/Reels entry points disappear, ideally with the remaining layout adapting.

WebView wrappers, browser substitutes and whole-app blocking are excluded. The target apps are the official, unmodified YouTube (`com.google.android.youtube`) and Instagram (`com.instagram.android`) builds.

### Platform constraints (primary sources)

- An AccessibilityService can read another app's window content and act on it only through:
  - `performAction` on a node;
  - `performGlobalAction`, e.g. `GLOBAL_ACTION_BACK`;
  - `dispatchGesture`;
  - overlays.
- No API hides, removes or resizes another app's views.
  - <https://developer.android.com/reference/android/accessibilityservice/AccessibilityService>
  - <https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo>
- An accessibility overlay can cover regions of the screen and consume touches.
  - `TYPE_ACCESSIBILITY_OVERLAY` is one way.
  - On API 34+, `attachAccessibilityOverlayToWindow` is another.
  - The target app's layout is unaffected, so nothing reflows.
- On API 34+, views marked `accessibilityDataSensitive` are hidden from services that are not accessibility tools.
  - <https://developer.android.com/privacy-and-security/risks/tapjacking>
- Sideloaded apps on Android 13+ need the user to "Allow restricted settings" before the service can be enabled.
  - <https://support.google.com/android/answer/12623953>
- Advanced Protection restricts accessibility services to verified accessibility tools.
  - <https://support.google.com/android/answer/16339980>
- Google Play AccessibilityService policy:
  - `isAccessibilityTool` is reserved for disability-support apps.
  - Other apps need prominent disclosure and consent, plus the Permissions Declaration.
  - <https://support.google.com/googleplay/android-developer/answer/10964491>

## Options

| Option | Reactive (A) | Entry points (B) | True removal + reflow | Official unmodified apps | Notes |
|---|---|---|---|---|---|
| 1. AccessibilityService only | Navigate away (Back) | None | No | Yes | Smallest |
| 2. AccessibilityService + accessibility overlay | Navigate away | Cover + block touches | No; the gap stays | Yes | **Chosen** |
| 3. Patched target app (ReVanced-style) | Yes | Real removal | Yes (YouTube) | No | Violates YouTube ToS; no Instagram equivalent found |
| 4. Root / LSPosed hooks | Yes | Real removal | Yes | Hooked at runtime | Needs root; breaks on updates |
| 5. Separate native client | n/a | n/a | Own UI | No | Unofficial APIs; ToS |

## Decision

Use option 2: one AccessibilityService scoped to the two target packages.

- **Behavior A:** detect an active short-form viewer and press Back once. Loop prevention and cooldown apply.
- **Behavior B:** cover detected entry points with accessibility overlays that block touches. Product and user-facing docs describe this as **masking, not removal**: entry points are covered and made non-interactive, and the surrounding layout does not reflow.

True removal with layout reflow is **not pursued**. It requires a different installation model (patched app or root/LSPosed). Adopting one needs a new ADR and explicit owner acceptance.

The distribution target is sideloading first. A prominent disclosure and consent screen is still built, so Play distribution stays possible later. Play approval is not assumed.

## Consequences

- Works with official apps on stock, unrooted devices.
- Detection depends on view IDs and structure that change with target-app updates. Each verified combination is recorded in `docs/COMPATIBILITY.md`, and anything else stays unverified.
- Masks can lag scrolling and leave a visible gap. Any claim about appearance or frame exposure needs measurement.
- Detection fails if the target apps mark the relevant views `accessibilityDataSensitive`, or on devices with Advanced Protection enabled.
- Unknown screens must do nothing. A missed block is preferred over blocking unrelated screens.

## Evidence and open questions

Prior art was reviewed for approach only. Its selectors are not trusted without device confirmation:

- <https://github.com/atick-faisal/Shorts-Blocker> (Back with cooldown, view-ID matching)
- <https://github.com/nethical6/digi-paws> (ID-based detection, `TYPE_ACCESSIBILITY_OVERLAY` UI hider)

Open questions, tracked in TASKS.md:

- Current view IDs and structure of the installed app versions.
- Whether either app marks these views sensitive.
- Overlay latency.
- How `setHideOverlayWindows` interacts with accessibility overlays.
- Android 17 behavior.
- Play review outcome.
