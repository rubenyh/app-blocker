# Privacy

## Implemented

The app declares an AccessibilityService protected by `android.permission.BIND_ACCESSIBILITY_SERVICE`. The user enables it in Android Accessibility settings. This provides the service connection needed for future Shorts/Reels detection and navigation; the current service does not detect or block anything.

The service configuration limits events to YouTube (`com.google.android.youtube`) and Instagram (`com.instagram.android`). It requests window-state, window-content and scroll events, permits window-content retrieval and requests view IDs. The current code reads only each event's package name and event type. It does not retrieve nodes, read node text, content descriptions or event text, or capture messages, credentials, titles, screenshots or full accessibility dumps.

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

The effect of the package filter on receiving switch-away events remains unverified. The coordinator must test it on a device before choosing a cleanup mechanism for future overlays and policy state; the service currently keeps the two-package filter.
