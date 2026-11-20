# Privacy

## Implemented

The app declares an AccessibilityService protected by `android.permission.BIND_ACCESSIBILITY_SERVICE`. The user enables it in Android Accessibility settings. With YouTube viewer blocking enabled, it detects the Shorts viewer and requests Back, with bounded retries. Instagram blocking is not implemented yet. Device validation of this detection and navigation is pending T-305.

The service configuration has no package filter, so Android delivers the service's events from every app ([ADR-0002](decisions/ADR-0002-unfiltered-events-for-context-exit.md)). This is needed to notice when the user leaves YouTube (`com.google.android.youtube`) or Instagram (`com.instagram.android`). The configuration requests window-state, window-content and scroll events, permits window-content retrieval, and requests view IDs.

For other apps, event handling reads only the event's package name and event type, in memory, to reset the blocking episode on window-state changes. It stores or logs nothing from those events and requests no action in those apps. Instagram currently receives the same treatment.

When YouTube viewer blocking is enabled, queued checks obtain the active root and check its package name before traversing. Only a YouTube hierarchy is traversed: the extractor reads resource IDs, class names, selected and visible flags, screen bounds, and child counts/children to derive parent indices. Invisible nodes and their subtrees are skipped. Extraction is limited to 400 obtained nodes and depth 40. Snapshots stay in memory for the current decision; Android nodes are not cached. No node text, content descriptions or event text are read, and no messages, credentials, titles, screenshots or full accessibility dumps are captured.

Debuggable builds write only the detection type, rule ID, action and retry-cap state to local logcat under `BlockerAccessibility`. Release builds do not log these decisions. This debug logging is separate from the diagnostics setting; diagnostics are not implemented yet.

Settings are stored locally in the existing `blocker_settings` Preferences DataStore, shared by the settings screen and service. The keys are:

- `youtube_block_viewer`
- `youtube_mask_entry_points`
- `instagram_block_viewer`
- `instagram_mask_entry_points`
- `diagnostics_enabled`

The app declares no network permission and uses no network communication. Android application backup is enabled in the manifest; there are no custom backup rules.

## Planned (not implemented)

- T-307: opt-in, local diagnostics limited to rule decisions.
- T-306: accessibility overlays covering entry points and blocking touches, without changing the target app's layout.

Android application backup is enabled, which may include the settings above in the user's device backup. Review this in T-307.
