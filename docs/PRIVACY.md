# Privacy

## Implemented

The app declares an AccessibilityService protected by `android.permission.BIND_ACCESSIBILITY_SERVICE`. The user enables it in Android Accessibility settings. With the respective app's viewer blocking enabled, it detects the YouTube Shorts or Instagram Reels viewer and requests Back, with bounded retries. Instagram Stories are not a viewer signal. Device validation: see [COMPATIBILITY.md](COMPATIBILITY.md); Instagram remains unverified on device.

The service configuration has no package filter, so Android delivers the service's events from every app ([ADR-0002](decisions/ADR-0002-unfiltered-events-for-context-exit.md)). This is needed to notice when the user leaves YouTube (`com.google.android.youtube`) or Instagram (`com.instagram.android`). The configuration requests window-state, window-content and scroll events, permits window-content retrieval, and requests view IDs.

For other apps, event handling reads only the event's package name and event type, in memory, to reset the blocking episode on window-state changes. It stores or logs nothing from those events and requests no action in those apps.

When YouTube or Instagram viewer blocking or entry-point masking is enabled, queued checks obtain the active root and check its package name before traversing. Only YouTube and Instagram hierarchies are traversed: the extractor reads resource IDs, class names, selected and visible flags, screen bounds, and child counts/children to derive parent indices. Each app's own toggles gate its decisions. Invisible nodes and their subtrees are skipped. Extraction is limited to 400 obtained nodes and depth 40. Snapshots stay in memory for the current decision; Android nodes are not cached. No node text, content descriptions or event text are read, and no messages, credentials, titles, screenshots or full accessibility dumps are captured.

YouTube tab masking uses the second Button's position under the bottom bar, without reading its label. Service-owned accessibility overlays cover that rectangle and consume touches without changing YouTube's layout. Masks clear when the context ends, masking is disabled, a viewer or unknown screen appears, the configuration changes, or the service is interrupted or ends. The position-based rule is locale-independent but can break if YouTube reorders tabs; see [ARCHITECTURE.md](../ARCHITECTURE.md#youtube-tab-masking).

Debuggable builds write only the detection type, rule ID, action and retry-cap state to local logcat under `BlockerAccessibility`; mask decisions add only rule ID and rectangle count. Release builds do not log these decisions. This debug logging is separate from the diagnostics setting; the opt-in in-memory diagnostics described below do not use logcat.

Instagram masking uses the `clips_tab` resource ID and bounds for the bottom tab, plus resource IDs, parent indices and bounds for inline feed video posts. An outer `video_container` must contain `media_group` and no `row_feed_photo_imageview` descendant; photo posts are excluded. Feed rectangles are clipped to the feed area below the top action bar and above the bottom tabs. The shared overlay covers these rectangles and consumes touches without changing Instagram's layout. No video, photo, label or other content is read or retained. The same mask cleanup paths apply. The profile Reels sub-tab and Explore grid remain outside this masking rule; inline feed overlay behavior and scroll lag still need device validation.

Settings are stored locally in the existing `blocker_settings` Preferences DataStore, shared by the settings screen and service. The keys are:

- `youtube_block_viewer`
- `youtube_mask_entry_points`
- `instagram_block_viewer`
- `instagram_mask_entry_points`
- `diagnostics_enabled`

## Opt-in local diagnostics

Diagnostics default off. When enabled, the process-wide log retains at most 200 YouTube and Instagram rule decisions in memory, dropping the oldest entries. Consecutive decisions from the same package with the same detection, action, mask count and rule ID are skipped. Settings displays the latest 50 entries and offers Clear; turning diagnostics off clears the entire log. Process death also loses the log. There is no export or share feature.

Each entry contains only:

- `timeMillis`: decision time in milliseconds, displayed as local time;
- `packageName`: target package name;
- `appVersion`: target app version name, or null if unavailable;
- `ruleId`: matched viewer or mask rule ID, or null;
- `detection`: `Viewer`, `None` or `Unknown`;
- `action`: `Back` or `Nothing`;
- `masks`: mask count only;
- `capHit`: whether the retry cap has been reached.

Entries contain no text, content descriptions, node lists, bounds, messages, credentials, titles, screenshots or accessibility dumps. They are never written to disk or sent over the network, and are not included in backup because they are never on disk. The persisted diagnostics opt-in setting is separate from these entries.

The manifest's `<queries>` package visibility lists only `com.google.android.youtube` and `com.instagram.android`. It is used only to read those two target apps' version names through the package manager; lookup results, including unavailable versions, are cached for the service lifetime.

The app declares no network permission and uses no network communication. Android application backup is enabled in the manifest with no custom backup rules, so the settings above may be included in the user's device backup; the diagnostic entries cannot be included.
