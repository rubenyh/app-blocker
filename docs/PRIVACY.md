# Privacy

## Implemented

The app declares an AccessibilityService protected by `android.permission.BIND_ACCESSIBILITY_SERVICE`. The user enables it in Android Accessibility settings. This provides the service connection needed for future Shorts/Reels detection and navigation; the current service does not detect or block anything.

The service configuration has no package filter, so Android delivers the service's events from every app ([ADR-0002](decisions/ADR-0002-unfiltered-events-for-context-exit.md)). This is needed to notice when the user leaves YouTube (`com.google.android.youtube`) or Instagram (`com.instagram.android`). The configuration requests window-state, window-content and scroll events, permits window-content retrieval, and requests view IDs.

For all other apps, the code reads only the event's package name and event type, in memory. It stores, logs or acts on nothing from those apps. For the two target apps, the current code also reads only the package name and event type. It does not retrieve nodes, read node text, content descriptions or event text, or capture messages, credentials, titles, screenshots or full accessibility dumps.

Events from unknown packages and apps with both behavior switches off are ignored. For enabled targets, debuggable builds write only the package name and event type to local logcat under `BlockerAccessibility`. Release builds do not log these events. This debug logging is separate from the diagnostics setting; diagnostics are not implemented yet.

Settings are stored locally in the existing `blocker_settings` Preferences DataStore, shared by the settings screen and service. The keys are:

- `youtube_block_viewer`
- `youtube_mask_entry_points`
- `instagram_block_viewer`
- `instagram_mask_entry_points`
- `diagnostics_enabled`

The app declares no network permission and uses no network communication. Android application backup is enabled in the manifest; there are no custom backup rules.

## Planned (not implemented)

- T-304: read view IDs, class names, state flags and bounds for detection, without reading text.
- T-307: opt-in, local diagnostics limited to rule decisions.
- T-306: accessibility overlays covering entry points and blocking touches, without changing the target app's layout.

Android application backup is enabled, which may include the settings above in the user's device backup. Review this in T-307.
