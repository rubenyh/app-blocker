# ADR-0002: Receive events from all packages to detect leaving a target app

- **Status:** Accepted (2026-11-19)
- **Related:** ADR-0001, TASKS.md T-302 open design point

## Context

Two things depend on knowing when the user leaves YouTube or Instagram:

- Overlay masks (T-306) must be removed when the user leaves the app.
- BlockPolicy state (T-304) must be reset when the user leaves the app.

We first scoped the service with `android:packageNames="com.google.android.youtube,com.instagram.android"`.

## Evidence

Test setup: 2026-11-19, AVD `Medium_Phone_API_36.1`, Android 16, debug build, YouTube preinstalled and signed out.

| Config | Switch YouTube → Settings → Home | Result |
|---|---|---|
| `packageNames` set to the two targets | No event received after leaving YouTube | The service cannot see the user leave |
| Same, plus `typeWindowsChanged` and `flagRetrieveInteractiveWindows` | Still no event after leaving YouTube | Adding window events does not help |
| No `packageNames` | `TYPE_WINDOW_STATE_CHANGED` for `com.android.settings` and the launcher. `TYPE_WINDOWS_CHANGED` also arrives (when requested) | The user leaving is observable |

## Options

1. Keep the filter and rely on the API 34+ `attachAccessibilityOverlayToWindow`, which follows the target window.
   - This does not help the policy reset.
   - It does not cover minSdk 24–33.
2. Remove `packageNames` and filter by package in code. **Chosen.**
3. Keep the filter and use a timeout or polling to guess that the user has left.
   - This is unreliable.
   - It risks stale masks.

## Decision

Remove `android:packageNames` from `accessibility_service_config.xml`. The service then receives events from every package.

- **Non-target packages:** the code reads only `event.packageName` and `event.eventType`, and uses them only as a "target context ended" signal. The signal clears masks and resets policy. For these packages the service:
  - retrieves no nodes;
  - performs no actions;
  - logs nothing.
- **Target packages:** handled as before.
- **T-304 may narrow the event types at runtime with `setServiceInfo`.** For example:
  - only `typeWindowStateChanged` while no target is in the foreground;
  - content and scroll events only while a target is in the foreground.

  This optimization reduces cross-app event traffic and is not required for correctness.

## Consequences

- The behavior is correct on all supported API levels without overlay-specific APIs.
- The service receives more events, which costs IPC and battery. T-304 should measure the cost and apply the runtime narrowing if needed.
- Privacy scope widens to package names and event types of all foreground apps. That data is processed in memory and never stored. `docs/PRIVACY.md` and the onboarding disclosure must say so.
