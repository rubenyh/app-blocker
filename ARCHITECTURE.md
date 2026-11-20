# Architecture

## Implemented

The single module `app/app/` is a Compose template under namespace `com.example.appblocker` (application ID `com.appblocker.app`). `MainActivity` enables edge-to-edge rendering and hosts `AppBlockerTheme` and `MainNavigation`. `Navigation.kt` uses Navigation3 with the serializable `Main` key from `NavigationKeys.kt`.

`ui/main/MainScreen` shows loading, error or a greeting list and a placeholder OK button. `MainScreenViewModel` maps repository data into a `StateFlow` of UI state. `data/DataRepository` exposes `Flow<List<String>>`; `DefaultDataRepository` emits `Android`. `ui/theme` supplies Material3 colors and typography. There is no blocker service, detection, overlay, persistent settings or onboarding; the manifest declares only the launcher activity.

## Planned

Everything below is planned, not implemented. [TASKS.md](TASKS.md) tracks delivery; [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md) is the accepted decision.

### Components

Keep one module. Packages below are under `com.example.appblocker`; T-301 replaces the placeholder `ui/main` and `data` packages.

| Component | Responsibility |
|---|---|
| `ui/onboarding` | Prominent disclosure, affirmative consent, opening Accessibility settings, restricted-settings help and service status. |
| `ui/settings` | Per-app × per-behavior toggles and diagnostics opt-in. |
| `settings/SettingsRepository` | DataStore Preferences exposed as `Flow<BlockerSettings>`. |
| `service/BlockerAccessibilityService` | Lifecycle, event intake, package filtering, service coroutine scope and applying actions. |
| `detect/ScreenSnapshot` | Package/event metadata and nodes with `viewId`, `className`, selected/visible flags and bounds; no text by default. |
| `detect/SnapshotExtractor` | Bounded breadth-first extraction of a snapshot. |
| `detect/YouTubeRules`, `detect/InstagramRules` | Pure functions returning `Detection { Viewer \| EntryPoints(rects) \| None \| Unknown }`. |
| `policy/BlockPolicy` | Pure state machine with an injected clock; returns `Action { Back \| Mask(rects) \| ClearMask \| Nothing }`. |
| `overlay/MaskOverlay` | `WindowManager` management of `TYPE_ACCESSIBILITY_OVERLAY`; add, update and `removeAll`. |
| `diagnostics/Diagnostics` | Opt-in, in-memory ring buffer of rule decisions. |
| `res/xml/accessibility_service_config.xml` | Scope the service to `com.google.android.youtube,com.instagram.android`. |

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
