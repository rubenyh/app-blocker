# Product

## Problem

Short-form video feeds such as YouTube Shorts and Instagram Reels are designed for endless scrolling. Users want to keep YouTube and Instagram for everything else, including videos, subscriptions, search, messages, profiles and stories, without being pulled into the short-form feed.

## Users

People who use YouTube and Instagram on Android and want short-form video gone from those apps. Users do not want to give up either app.

## Scope

- **Platform:** Android, Kotlin, Jetpack Compose.
- **Target apps:**
  - YouTube (`com.google.android.youtube`): Shorts.
  - Instagram (`com.instagram.android`): Reels.
- **Mechanism:** one AccessibilityService plus accessibility overlays on the official, unmodified apps. See [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md).
- **Distribution:** sideloaded APK first.

## Requirements

### A — Reactive blocking

When the user enters the Shorts or Reels viewer through any path (tab, feed shelf, search, profile, message or external link), the app leaves the viewer immediately. Other app functionality keeps working.

### B — Entry-point removal (delivered as masking)

Shorts and Reels entry points, such as tabs, shelves and buttons, should look absent.

What is technically possible on official apps:

- Entry points are **covered by an overlay and made non-interactive**.
- The target app's layout **does not reflow**: the covered area stays as a gap.
- True removal with reflow would need a patched app or root. That is out of scope unless a future ADR accepts it.

Requirement B stays a product requirement. It must not be silently dropped in favor of A alone.

### Behavior rules

- Detection distinguishes an **active viewer** (block with Back) from a **visible entry point** (mask only, never Back).
- Unknown or unrecognized screens are left alone. A missed block is better than blocking normal use or looping.
- At most one Back per viewer entry, with a cooldown, and a retry cap after which the app stops and records a diagnostic.
- Per-app and per-behavior toggles apply immediately, including while a target app is open.
- Overlays are removed as soon as their context ends: the user leaves the app, the screen changes, protection turns off, the service stops, or the screen rotates.

### Onboarding and settings

- Explain in plain language what the accessibility permission is used for, and require affirmative consent before sending the user to system settings.
- Explain the Android 13+ "Allow restricted settings" step for sideloaded installs.
- Show whether the service is running.
- All screens are accessible: TalkBack labels, scalable text and sufficient contrast.

### Privacy

- The service reads only the two target packages.
- It extracts only the UI metadata needed for detection: view IDs, class names, state flags and bounds. It does not extract text by default.
- Diagnostics are opt-in, local and limited to rule decisions. They contain no messages, credentials, titles, full accessibility dumps or screenshots.

Details live in `docs/PRIVACY.md` once the service exists.

## Non-goals

- LinkedIn, TikTok and other apps (TikTok is entirely short-form).
- WebView, browser wrappers or whole-app blocking.
- Patched apps, root or LSPosed modules.
- Usage statistics, time limits and accounts.
- Network communication of any kind.

## Acceptance criteria

1. With YouTube protection on, opening a Short from each supported path returns to the previous screen. Normal videos, search, subscriptions and the home feed keep working.
2. With Instagram protection on, opening a Reel from each supported path returns to the previous screen. A Reel opened from a DM returns to the DM thread. DMs, profiles, stories and the feed keep working.
3. With masking on, verified entry points are covered and taps on them do nothing. Masks disappear whenever their screen is gone.
4. Turning a toggle off takes effect without restarting the target app.
5. No repeated Back loops in any tested scenario.
6. Every claim of the form "works on X" is backed by a row in `docs/COMPATIBILITY.md` with the app version, Android version, locale and date.

## Product decisions

| Date | Decision |
|---|---|
| 2026-11-19 | Accessibility + overlay masking on official apps; true removal deferred (ADR-0001). |
| 2026-11-19 | Sideload first; disclosure still built so Play is possible later. |
| 2026-11-19 | Scope limited to YouTube and Instagram. |

## Open questions

- Does Play approve this use of the AccessibilityService API? Not assumed. This is task T-504.
- How noticeable is the masked gap in practice? Measure in T-306 and T-403.
- Do target-app updates or Android 17 Advanced Protection break detection? See T-502 and T-503.
