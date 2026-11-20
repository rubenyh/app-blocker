# Architecture

## Implemented

The single module `app/app/` uses Compose under namespace `com.example.appblocker` (application ID `com.appblocker.app`). `MainActivity` enables edge-to-edge rendering and hosts `AppBlockerTheme` and `MainNavigation`. `Navigation.kt` uses Navigation3 with the serializable `Settings` start key from `NavigationKeys.kt`.

`ui/settings/SettingsScreen` shows per-app viewer-blocking and entry-point-masking switches plus diagnostics opt-in. `SettingsViewModel` exposes a nullable settings `StateFlow` while loading and saves toggles through `settings/SettingsRepository`. The concrete repository exposes DataStore Preferences as `Flow<BlockerSettings>` and performs atomic updates. The top-level `Context.blockerSettingsDataStore` extension supplies the single process-wide `blocker_settings` store, shared with the service. Viewer blocking defaults on; masking and diagnostics default off. Read IO failures emit defaults, and write IO failures leave the saved state unchanged and show a retry message.

`ui/theme` supplies Material3 colors and typography. The template screen, repository and tests have been removed.

`service/BlockerAccessibilityService` is registered in the manifest with `BIND_ACCESSIBILITY_SERVICE`. It runs independently of any Activity, creates a supervised main-thread coroutine scope on connection, and collects the existing repository's settings into a volatile field. Reconnection cancels the previous scope and resets the policy and conflated trigger channel; unbind and destroy cancel the scope, close the channel and reset policy. Interrupt resets policy.

`res/xml/accessibility_service_config.xml` has no package filter ([ADR-0002](docs/decisions/ADR-0002-unfiltered-events-for-context-exit.md)). It requests window-state, window-content and scroll events with a 100 ms notification timeout, window-content retrieval and view IDs. Non-YouTube window-state events synchronously clear masks and reset policy; no hierarchy is traversed for those events. YouTube events enqueue a trigger while viewer blocking or entry-point masking is enabled. Instagram detection and masking are not implemented. See [docs/PRIVACY.md](docs/PRIVACY.md).

### YouTube detection and policy

`detect/ScreenSnapshot` contains a package name and nodes with view ID, class name, selected flag, bounds and parent index, without text or content descriptions. `SnapshotExtractor` checks the active root's package before traversing YouTube, then performs breadth-first extraction, skipping invisible subtrees. It obtains at most 400 nodes and descends to depth 40; a limit hit, missing root or stale-node runtime exception yields Unknown. Nodes are recycled below API 33 and never cached across events.

`detect/YouTubeRules.detectYouTube` is pure. The captured T-303 fixtures support `Viewer("yt-reel-player-v1")` when a `reel_player_page_container` descends from `reel_recycler` and is at least half the root height. Empty snapshots or nonpositive root height are Unknown; other YouTube screens are None. No tab selection or `reel_time_bar` signal is used.

`policy/BlockPolicy` accepts monotonic milliseconds per call and returns Back or Nothing. A viewer episode sends one Back immediately, permits retries at least 1500 ms apart, and caps attempts at three. Unknown preserves the episode without action. None, a package switch, context exit or disabling blocking ends it.

One main-thread collector consumes `Channel<Unit>(Channel.CONFLATED)`: active root -> extractor -> YouTube rule -> policy with live settings and `SystemClock.elapsedRealtime()` -> `performGlobalAction(GLOBAL_ACTION_BACK)` when requested. A null snapshot is Unknown; a different root package resets policy without Back. Settings disable resets policy immediately, and re-enabling queues a fresh check. Debuggable builds log only detection, rule ID, action and retry-cap state. Fixture tests verify matching and policy, not device behavior; T-305 owns device validation.

### YouTube tab masking

`youTubeEntryPoints` is pure and separate from viewer detection and policy. Rule `yt-shorts-tab-v1` locates the single `pivot_bar` HorizontalScrollView, its direct LinearLayout, and the second of that layout's direct Button children, only when there are four or five buttons. It returns that button's screen bounds; missing or ambiguous structure and invalid bounds return no rectangles. T-303 fixtures from YouTube 21.41.525 support the four-button layout; five-button behavior has synthetic coverage only. This position-based rule is locale-independent but fragile if YouTube reorders tabs. It reads no labels.

With masking enabled, the collector passes those bounds to `overlay/MaskOverlay` only for a known non-viewer YouTube screen. Empty, null, Unknown, Viewer or other-package snapshots clear masks. `MaskOverlay` owns plain touch-consuming views in service-context `TYPE_ACCESSIBILITY_OVERLAY` windows, updates existing layouts and removes extras. Views cannot take focus and are hidden from accessibility traversal. Masks cover the tab without layout reflow. System night mode selects initial white/dark color guesses, which require calibration against YouTube's own theme on the device.

Masks clear on context exit, masking toggle-off, interrupt, unbind, destroy, reconnect, configuration change and before Back. Configuration changes and enabling masking enqueue a fresh check. Window add/update/remove failures are caught so they do not crash the service. Debug-only mask logs contain only rule ID and rectangle count. Device positioning, touch blocking, colors and lifecycle behavior remain unverified until coordinator validation.

### Local diagnostics

`diagnostics/Diagnostics` is a process-wide pure Kotlin object shared by the service and settings ViewModel. Its read-only `StateFlow` exposes at most 200 decision entries, dropping the oldest when full. Recording defaults off; the service settings collector and the ViewModel's collected settings synchronize opt-in, and disabling clears the log. Clear removes entries without disabling recording. Nothing is persisted or sent over the network; process death loses the log.

After each YouTube decision while diagnostics is enabled, the service records wall-clock time, package name, cached target version name (nullable), rule ID (nullable), detection (`Viewer`, `None`, `Unknown`), action (`Back`, `Nothing`), mask count and retry-cap state. Consecutive decisions with the same detection, action, mask count and rule ID are skipped. Version names use `getPackageInfo` once per package per service lifetime, caching null when the package is unavailable; manifest queries expose only the two target packages for this lookup.

Settings shows the retained count, Clear, and the newest 50 entries in reverse time order while enabled. Each plain-text row shows local `HH:mm:ss`, short package name, version, detection/rule ID, action, mask count and an optional cap marker. Rows scroll horizontally to preserve one line and full text access. There is no export or share action. See [docs/PRIVACY.md](docs/PRIVACY.md) for data boundaries.

## Planned

Everything below is planned, not implemented. [TASKS.md](TASKS.md) tracks delivery; [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md) is the accepted decision.

### Components

Keep one module. Packages below are under `com.example.appblocker`.

| Component | Responsibility |
|---|---|
| `ui/onboarding` | Prominent disclosure, affirmative consent, opening Accessibility settings, restricted-settings help and service status. |
| `detect/InstagramRules` | Evidence-backed Instagram viewer detection (T-402). |

### Planned event-flow extensions

```text
Accessibility event
  -> package filter (YouTube / Instagram)
  -> conflated channel (latest snapshot only)
  -> SnapshotExtractor (bounded BFS)
  -> YouTubeRules / InstagramRules
  -> BlockPolicy + live settings + injected clock
  -> Back | Nothing (viewer policy); show / removeAll (separate masking path)
```

The implemented flow above handles YouTube viewer blocking and tab masking. Instagram rules extend it later.

### Behavior and lifecycle

- Viewer and EntryPoint decisions are separate: only a confirmed viewer can trigger Back; entry points only trigger masking.
- Match view IDs and class names, never text, so rules are locale-independent. Selectors need evidence from the installed target-app version.
- Unknown screens produce no blocking action and clear masks. Stale nodes yield Unknown. Prefer a missed block over affecting normal use.
- Preserve the implemented viewer episode, cooldown and retry cap while adding Instagram and masking; prevent repeated Back loops, including when the user presses Back.
- Collect settings live in the service scope so toggles apply while a target app is open.
- Establish the service scope on connection and cancel it when the service ends. Manage masks only for the current context.
- Remove all overlays on package change, window-state change, configuration or rotation change, toggle-off, `onInterrupt`, `onUnbind` and `onDestroy`. A reconnected service starts without stale masks or policy state.

### Data boundaries and constraints

The service reads only event package name and type from non-target events (ADR-0002); node extraction currently handles only YouTube, checking the active root package first. Snapshots contain package, view IDs, class names, selected flags, bounds and parent indices, without text or content descriptions. Nothing leaves the device. Opt-in diagnostics contain only local rule decisions in memory, never messages, credentials, titles, screenshots or full accessibility dumps. Evidence must follow [docs/TESTING.md](docs/TESTING.md).

The ADR permits Back navigation and overlay masking on official apps. It does not permit true view removal or layout reflow: masked gaps remain and masks may lag. Patched apps and root require a new accepted ADR. Sensitive views (`accessibilityDataSensitive`) can hide needed metadata, and Advanced Protection can prevent this service from operating; Android 17 impact remains unverified in T-503. Android 13+ sideloads require the user's restricted-settings step. Prominent disclosure and consent are required by planned onboarding; Play approval is not assumed.
