# Architecture

## Implemented

The single module `app/app/` uses Compose under namespace `com.example.appblocker` (application ID `com.appblocker.app`). `MainActivity` enables edge-to-edge rendering and hosts `AppBlockerTheme` and `MainNavigation`. `Navigation.kt` uses Navigation3 with the serializable `Settings` start key from `NavigationKeys.kt`.

`ui/settings/SettingsScreen` shows per-app viewer-blocking and entry-point-masking switches plus diagnostics opt-in. `SettingsViewModel` exposes a nullable settings `StateFlow` while loading and saves toggles through `settings/SettingsRepository`. The concrete repository exposes DataStore Preferences as `Flow<BlockerSettings>` and performs atomic updates. The top-level `Context.blockerSettingsDataStore` extension supplies the single process-wide `blocker_settings` store, shared with the service. Viewer blocking defaults on; masking and diagnostics default off. Read IO failures emit defaults, and write IO failures leave the saved state unchanged and show a retry message.

`ui/theme` supplies Material3 colors and typography. The template screen, repository and tests have been removed. Detection, overlays and onboarding are not implemented.

`service/BlockerAccessibilityService` is registered in the manifest with `BIND_ACCESSIBILITY_SERVICE`. It runs independently of any Activity, creates a supervised main-thread coroutine scope on connection, and collects the existing repository's settings into a volatile field. Reconnection cancels the previous scope; unbind and destroy cancel it idempotently. Interrupt is a no-op because there are no actions or overlays yet.

`res/xml/accessibility_service_config.xml` filters events to `com.google.android.youtube,com.instagram.android`, requesting window-state, window-content and scroll events with a 100 ms notification timeout. It enables window-content retrieval and reporting view IDs for future detection; the service currently reads only package name and event type. The pure `service/TargetApps.isEnabledFor` gate ignores unknown packages and targets with both behavior toggles off. Accepted events log only package and event type when `ApplicationInfo.FLAG_DEBUGGABLE` is set. There is no detection, blocking, masking or diagnostics. See [docs/PRIVACY.md](docs/PRIVACY.md).

## Planned

Everything below is planned, not implemented. [TASKS.md](TASKS.md) tracks delivery; [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md) is the accepted decision.

### Components

Keep one module. Packages below are under `com.example.appblocker`.

| Component | Responsibility |
|---|---|
| `ui/onboarding` | Prominent disclosure, affirmative consent, opening Accessibility settings, restricted-settings help and service status. |
| `detect/ScreenSnapshot` | Package/event metadata and nodes with `viewId`, `className`, selected/visible flags and bounds; no text by default. |
| `detect/SnapshotExtractor` | Bounded breadth-first extraction of a snapshot. |
| `detect/YouTubeRules`, `detect/InstagramRules` | Pure functions returning `Detection { Viewer \| EntryPoints(rects) \| None \| Unknown }`. |
| `policy/BlockPolicy` | Pure state machine with an injected clock; returns `Action { Back \| Mask(rects) \| ClearMask \| Nothing }`. |
| `overlay/MaskOverlay` | `WindowManager` management of `TYPE_ACCESSIBILITY_OVERLAY`; add, update and `removeAll`. |
| `diagnostics/Diagnostics` | Opt-in, in-memory ring buffer of rule decisions. |

### Event flow

```text
Accessibility event
  -> package filter (YouTube / Instagram)
  -> conflated channel (latest snapshot only)
  -> SnapshotExtractor (bounded BFS)
  -> YouTubeRules / InstagramRules
  -> BlockPolicy + live settings + injected clock
  -> Back | Mask(rects) | ClearMask | Nothing
```

Event bursts are conflated so only the latest snapshot is processed; extraction precedes rules. The service owns Android APIs and applies actions, while rules and policy stay independent of them for JVM testing. Context changes must reset policy and clear masks even when the new package is outside the filter. **Open design point:** a `packageNames` filter may hide the switch-away event. T-302a resolves this with evidence; see TASKS.md.

### Behavior and lifecycle

- Viewer and EntryPoint decisions are separate: only a confirmed viewer can trigger Back; entry points only trigger masking.
- Match view IDs and class names, never text, so rules are locale-independent. Selectors need evidence from the installed target-app version.
- Unknown screens produce no blocking action and clear masks. Stale nodes yield Unknown. Prefer a missed block over affecting normal use.
- Allow at most one Back per viewer entry, with a cooldown and retry cap. Reset on window or package change; prevent repeated Back loops, including when the user presses Back.
- Collect settings live in the service scope so toggles apply while a target app is open.
- Establish the service scope on connection and cancel it when the service ends. Manage masks only for the current context.
- Remove all overlays on package change, window-state change, configuration or rotation change, toggle-off, `onInterrupt`, `onUnbind` and `onDestroy`. A reconnected service starts without stale masks or policy state.

### Data boundaries and constraints

The planned service reads only the two target packages. Snapshots contain package, event type, view IDs, class names, state flags and bounds; text is not extracted by default. Nothing leaves the device. Diagnostics are off by default and contain only local rule decisions in memory, never messages, credentials, titles, screenshots or full accessibility dumps. Evidence must follow [docs/TESTING.md](docs/TESTING.md).

The ADR permits Back navigation and overlay masking on official apps. It does not permit true view removal or layout reflow: masked gaps remain and masks may lag. Patched apps and root require a new accepted ADR. Sensitive views (`accessibilityDataSensitive`) can hide needed metadata, and Advanced Protection can prevent this service from operating; Android 17 impact remains unverified in T-503. Android 13+ sideloads require the user's restricted-settings step. Prominent disclosure and consent are required by planned onboarding; Play approval is not assumed.
