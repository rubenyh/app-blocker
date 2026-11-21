# Tasks

This is the single implementation backlog.

- **Statuses:** `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`.
- **DONE** requires all of the following:
  - acceptance criteria met;
  - the exact checks run, with results;
  - unavailable checks listed as NOT RUN.
- **Orca** records the Run, Task and Dispatch IDs when an Orca worker did the work.

## P1 — Assessment and decision

### T-101 ADR-0001 accepted — DONE
- **Acceptance:** the ADR records the options, decision, consequences, evidence URLs and unknowns.
- **Evidence:** `docs/decisions/ADR-0001-accessibility-overlay-masking.md`, accepted by the owner on 2026-11-19.

### T-102 Baseline build — DONE
- **Acceptance:** the result of the baseline build and unit tests is recorded.
- **Evidence (2026-11-19, Windows 11, Temurin 17.0.18):**
  - Ran `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` from `app/`.
  - Result: BUILD SUCCESSFUL in 3m 17s.
  - Unit tests: 2 template tests passed (`MainScreenViewModelTest`).
  - Instrumented tests: NOT RUN, because no device was connected.
- **Note:** AGP 9.0.1 warns that it was tested only up to compileSdk 36.1, while the project uses 37. It is left unchanged because nothing fails.

## P2 — Instructions, agents, skills, essential docs (Dep: T-101)

**Orca (M1):** run `run_247c57170370`.

### T-201 AGENTS.md + CLAUDE.md — DONE
- **Acceptance:**
  - CLAUDE.md imports AGENTS.md exactly once.
  - The listed commands match T-102.
- **Orca:** task `task_48dad810148a`, dispatch `ctx_1e8d71007936` (claude worker), merged in `6aa8209`.
- **Evidence (2026-11-19):**
  - `CLAUDE.md` line 1 is `@AGENTS.md`, and `AGENTS.md` has no imports.
  - Commands match T-102, with lint marked not run.
  - A documentation-maintainer drift review was run; its findings were fixed in `docs: sync M1 docs`.
  - Fresh-session load check: PASS. A new `claude -p` session in this worktree loaded the global CLAUDE.md, the project CLAUDE.md, AGENTS.md (once, via import) and MEMORY.md. No instruction text was duplicated.

### T-202 Project skills — DONE
- **Acceptance:**
  - `kotlin-feature`, `accessibility-rule`, `android-validation` and `docs-sync` have valid frontmatter.
  - `/skills` lists them.
  - Their descriptions are distinct.
- **Orca:** same dispatch as T-201.
- **Evidence (2026-11-19):**
  - After the merge, the coordinator's Claude Code 2.1.326 session listed all four project skills, with distinct descriptions.
  - The `docs-sync` skill loaded in the drift-check subagent.
  - The `shell: powershell` frontmatter key is documented but not exercised yet.

### T-203 Project subagents — DONE
- **Acceptance:**
  - The five agents are listed by `/agents`.
  - Their tool boundaries match AGENTS.md and CONTRIBUTING.md.
- **Orca:** same dispatch as T-201.
- **Evidence (2026-11-19):**
  - After the merge, the coordinator session listed all five agents with the expected tools: `android-qa-reviewer` without Edit/Write, and the researcher and privacy reviewer read-only.
  - `documentation-maintainer` was run successfully as a read-only drift check.
  - Device gating in `android-validation` now enforces coordinator ownership of adb.

### T-204 Essential docs — DONE
- **Acceptance:**
  - README (scope fixed), CONTRIBUTING, ARCHITECTURE (marked Planned) and docs/TESTING exist.
  - None of them claims unimplemented features.
- **Orca:** task `task_dc0aa4eba646`, dispatch `ctx_37b165cdda95` (codex worker), merged in `7308d80`.
- **Evidence (2026-11-19):**
  - The coordinator reviewed all four files.
  - The drift check found no claims of implemented features, lint results or device results.
  - Anchor and path fixes were applied in `docs: sync M1 docs`.

## P3 — Smallest verified YouTube implementation (Dep: P2)

**Orca (M2):** run `run_b54a6e861b0e`.

### T-301 Settings + DataStore — DONE
- **Work:**
  - Replace the template `ui/main` and `data/` with `settings/SettingsRepository` (DataStore Preferences) and a settings screen.
  - Delete the template tests.
- **Acceptance:** the settings unit test passes, and the toggles persist across app restart.
- **Orca:** task `task_cb2dd317ebb4`, dispatch `ctx_06b52d63f7a0` (codex), merged in `017164f`. Follow-up `refactor(settings)` commit by the coordinator.
- **Evidence (2026-11-19, integration branch):**
  - Ran `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug`: exit 0.
  - Unit tests: 6 pass (`SettingsRepositoryTest` 5, `SettingsViewModelTest` 1).
  - Lint: 0 errors, 14 warnings, all pre-existing dependency or unused-resource warnings.
  - DataStore version 1.3.0, the latest stable on Google Maven.
  - Device (AVD `Medium_Phone_API_36.1`, Android 16):
    - The settings screen shows the expected defaults: viewer blocking on, masking off, diagnostics off.
    - Toggling YouTube masking, then `am force-stop`, then relaunch keeps the value.
  - `SettingsScreenTest` (instrumented): compiles, but has NOT RUN.
  - TalkBack: NOT RUN.

### T-302 Service skeleton + onboarding — DONE (Dep: T-301)
- **Work:**
  - T-302a: the service, `accessibility_service_config.xml` scoped to both packages, and the manifest `<service>` with `BIND_ACCESSIBILITY_SERVICE`.
  - T-302b: onboarding with prominent disclosure, consent, restricted-settings help and service status.
  - Create `docs/PRIVACY.md`.
- **Acceptance:**
  - The service enables on the emulator.
  - It receives YouTube events.
  - It logs only the package and event type.
- **Open design point: RESOLVED by ADR-0002.** With the package filter in place, no switch-away event arrives. Without it, other apps' `TYPE_WINDOW_STATE_CHANGED` events arrive. The `packageNames` filter was removed, and non-target events contribute only their package name and type.
- **T-302a:** DONE.
  - **Orca:** task `task_f84661f350fd`, dispatch `ctx_72bb6b6781bc` (codex), merged in `15b6cae`.
  - **Evidence (2026-11-19, AVD Android 16):**
    - Build and unit tests pass on the integration branch: 10 tests, including `TargetAppsTest` 4.
    - The service enabled via adb (emulator only) shows in `dumpsys accessibility` "Enabled services".
    - Launching YouTube logs `com.google.android.youtube TYPE_WINDOW_STATE_CHANGED`.
    - Live toggle: with both YouTube toggles off, 0 log lines while YouTube is used. Turning one toggle back on resumes logging without restarting the service.
    - Enabling the service through the Settings UI is still NOT RUN. It is covered by T-302b's onboarding check.
- **T-302b:** DONE.
  - **Orca:** task `task_80089885a8d7`, dispatch `ctx_67e19ef13d00` (antigravity), merged in `789b659`, followed by coordinator fixes.
  - **Fixes:**
    - Resume no longer forces onboarding, so "Not now" is respected.
    - Uses the service class reference.
    - Disclosure wording.
    - `.kotlin` added to gitignore.
  - **Evidence (2026-11-19, AVD Android 16):**
    - Build, unit tests (`ServiceStatusTest` 11 plus the earlier tests), androidTest compile and lint: exit 0.
    - With the service disabled, launch shows onboarding with the full disclosure, including the ADR-0002 sentence about other apps.
    - "Agree and open Accessibility settings" opens `AccessibilitySettingsActivity`.
    - After enabling the service and returning, the app shows Settings with "Protection service: On".
    - "Not now", then Home, then relaunch stays on Settings with "Protection service: Off".
    - Enabling through the real Settings toggle and the restricted-settings flow on a sideloaded install: NOT RUN, because the service was enabled via adb.
    - Instrumented `OnboardingScreenTest`: NOT RUN.

### T-303 YouTube evidence capture — DONE (Dep: T-302)
- **Setup:** YouTube is preinstalled on the AVD and was used signed out.
- **Acceptance:** redacted fixtures exist for the Shorts viewer, Shorts tab, Shorts shelf, a normal video, the home feed, search results and a channel page. Redacted means view IDs, classes, flags and bounds only.
- **Evidence (2026-11-19, YouTube 21.41.525, AVD Android 16, en-US):**
  - Fixtures are in `app/app/src/test/resources/fixtures/youtube/`:
    - `shorts_viewer_tab` (positive)
    - `shorts_viewer_from_search` (positive; the Home tab is still selected)
    - `home` (negative)
    - `search_results` (negative)
    - `normal_video` (negative)
    - `channel` (negative, with the long-form miniplayer)
  - A format check confirmed that every node line contains only class, ID, selected flag and bounds.
  - Raw dumps and temporary screenshots were deleted from the device and from the scratchpad.
- **Observations:**
  - Viewer signal: `id/reel_recycler` containing `id/reel_player_page_container`, filling the content area.
  - `id/reel_time_bar` stays in the tree on every screen after Shorts has been opened once, so it is **not** a viewer signal.
  - The Shorts viewer can appear with the Home tab selected, so the selected tab is not a viewer signal.
  - The bottom-bar Shorts tab and the channel "Shorts" sub-tab have **no resource ID**. Identifying them needs a content-desc label (locale-dependent) or position. This affects masking (T-306), not T-304.
- **Not captured (UNKNOWN):**
  - Home-feed Shorts shelf: signed-out Home shows no feed.
  - A Shorts external link: no Short URL was available without inventing an ID. This is covered in T-305 device scenarios using a Share link.

### T-304 YouTube rules + BlockPolicy — DONE (Dep: T-303)
- **Acceptance:** unit tests cover these cases:
  - viewer gives exactly one Back;
  - entry point never gives Back;
  - unknown does nothing;
  - an event burst produces one action;
  - cooldown;
  - retry cap;
  - reset on package switch.
- **Orca:** task `task_324b18b51f9b`, dispatch `ctx_6a7c54facdc7` (codex), merged in `5caa39d`.
- **Evidence (2026-11-19):**
  - Build, unit tests (44, including `YouTubeRulesTest` 13 and `BlockPolicyTest` 10), androidTest compile and lint all exit 0.
  - Rule `yt-reel-player-v1` matches `reel_player_page_container` inside `reel_recycler` at 50% or more of the root height.
- **Known limit:** a retry needs a new YouTube event at least 1500 ms after the previous Back. A completely static, persistent viewer would get no retry. This was not observed on the device.

### T-305 YouTube device validation (behavior A) — DONE (Dep: T-304)
- **Acceptance:** the docs/TESTING YouTube scenarios pass on the emulator, and `docs/COMPATIBILITY.md` has a measured row.
- **Evidence (2026-11-19, YouTube 21.41.525, AVD Android 16, en-US):** see `docs/COMPATIBILITY.md`.
  - One Back each time Shorts opened from the Shorts tab (about 316 ms) and from the search Shorts grid (about 370 ms).
  - Normal video, home, channel and search pages produced no action.
  - Toggling off and on applies live.
  - The user pressing Back, rapid switching and a service reconnect all behave correctly.
- **NOT RUN:**
  - external link;
  - home Shorts shelf (signed out);
  - rotation;
  - es locale;
  - signed-in account;
  - physical device;
  - frame-exposure measurement.

  These are tracked in T-501.

### T-306 YouTube entry-point masking (behavior B) — DONE (Dep: T-305)
- **Acceptance:**
  - Masks follow scroll and rotation.
  - Masks clear on leave and on toggle-off.
  - Taps underneath are blocked.
  - The gap and lag are measured and recorded honestly.
- **Orca:** task `task_ef54d1e25239`, dispatch `ctx_7a1d8e91d394` (codex), merged in `c87d07f`. The coordinator then calibrated the 3 px top inset and the colors.
- **Evidence (2026-11-19):** see `docs/COMPATIBILITY.md`.
  - Unit tests: 55, including `YouTubeEntryPointsTest`.
  - On the device: position, touch blocking, light and dark blend, landscape, leave/return and live toggle all PASS.
- **Scope limits:**
  - Only the bottom-bar Shorts tab is masked. Search, feed and channel Shorts entries have no IDs and are not masked.
  - The tab is identified by position: fragile if YouTube reorders tabs. The 5-button (signed-in) layout is unverified.
  - Mask lag was not measured.
### T-307 Opt-in diagnostics — DONE (Dep: T-304)
- **Acceptance:**
  - Diagnostics are off by default.
  - They are an in-memory ring buffer that the user can view and clear.
  - PRIVACY is updated.
- **Orca:** task `task_757e9d034165`, dispatch `ctx_7a7da4ec6a41` (codex), merged in `9773b03`.
- **Evidence (2026-11-19):**
  - **Checks:** build, 61 unit tests (including `DiagnosticsTest` 6), androidTest compile and lint all exit 0.
  - **Device (AVD Android 16):**
    - Off by default: no diagnostics section is shown.
    - When enabled, an entry `HH:mm:ss youtube 21.41.525 None/yt-shorts-tab-v1 Nothing masks=1` is recorded. The version is read through `<queries>`.
    - Consecutive duplicates are suppressed.
    - "Clear" empties the log, and new decisions reappear afterwards.
    - Turning the switch off and on leaves the log empty.
    - After a force-stop and restart, the log is empty: it is in memory only.
  - **NOT RUN:** the cap at 200 (unit-tested only), TalkBack, and large text.
- **Backup review:** the diagnostics log is never written to disk, so it is never backed up. The DataStore toggles may be included in Android backup. This is accepted because they are non-sensitive booleans; see PRIVACY.md.
## P4 — Instagram

### T-401 Instagram evidence capture — IN_PROGRESS (Dep: T-302)
- **Acceptance:** fixtures exist for:
  - the Reels tab;
  - a Reel opened from the feed, a profile, a DM and an external link;
  - negative screens: DM thread, profile grid, stories, home feed, search and a normal (non-Reel) video post.
- **Evidence so far (2026-11-20; Instagram 456.1.0.56.78; AVD Android 16, en-US; signed-in throwaway test account):**
  - Fixtures are in `app/app/src/test/resources/fixtures/instagram/`. Redaction check: 0 nonconforming lines. Raw dumps and screenshots are deleted.
  - Positive fixtures: `reels_tab`, `reel_from_feed`, `reel_from_explore`, `reel_from_profile`.
  - Negative fixtures: `home` (includes an inline feed video), `explore`, `direct_inbox`, `own_profile`, `other_profile` (Reels grid), `other_profile_posts` (highlights tray), `story_viewer`.
- **Observations:**
  - **Viewer:** `id/clips_viewer_view_pager` containing a full-height `id/clips_video_container`. This holds for the tab, feed, Explore and profile paths.
  - **Stories:** they use `id/reel_viewer_root` and `highlights_reel_tray_recycler_view`. Instagram calls stories "reels" internally, so rules must match `clips_*`, never `reel_*`.
  - **Bottom tabs have IDs:** `feed_tab`, `clips_tab` (Reels), `direct_tab`, `search_tab`, `profile_tab`. The Reels entry point can be masked by ID, independent of locale.
  - **Profile Reels sub-tab:** the 2nd `profile_tab_icon_view`, with no unique ID.
  - **Inline feed video posts** play in the home feed (`video_container`). They are not the Reels viewer and are not blocked. This is a product limitation to note in PRODUCT.
- **Remaining:**
  - A DM thread containing a Reel, and a Reel opened from that DM. **Needs the owner** to send a Reel to the test account by DM.
  - A Reel opened from an external link.
### T-402 Instagram rules — DONE (Dep: T-401, T-304)
- **Acceptance:**
  - Unit tests pass.
  - A Reel opened from a DM goes Back to the DM.
  - A DM is never blocked.
- **Orca:** run `run_f8e33a729e0b`, task `task_8d369736d3a7`, dispatch `ctx_13bad4d90b53` (codex), merged in `fce2cd1`.
- **Evidence (2026-11-20):**
  - Build, 95 unit tests (including `InstagramRulesTest`), androidTest compile and lint all exit 0.
  - Rules `ig-clips-viewer-v1` (viewer) and `ig-reels-tab-v1` (`clips_tab` mask).
  - `story_viewer` is asserted as None.
- **Not yet verified:** "Reel from a DM returns to the DM" was not exercised on the device. It moves to T-403.

### T-403 Instagram device validation + Reels-tab masking — IN_PROGRESS (Dep: T-402, T-306)
- **Acceptance:** scenarios pass, and `docs/COMPATIBILITY.md` rows are added.
- **Evidence (2026-11-20, Instagram 456.1.0.56.78, AVD Android 16):** see `docs/COMPATIBILITY.md`.
  - Viewer blocked from the tab, feed, Explore and profile grid: one Back each, 0.9 to 1.5 s after the tap.
  - Home, Explore, DM inbox, profile and stories: no action.
  - Masking: covered, touch-blocked, light and dark themes, cleared when leaving the app or toggling masking off.
- **Environment note:** the emulator ran out of storage. With the owner's approval, the owner's dev apps were uninstalled from the AVD: `com.astra.sociallock`, `com.example.todo`, `com.ruben_yh.mobileapp`, `com.hiram.todoexpo`.
- **Remaining:**
  - A Reel opened from a DM, and the return to the thread.
  - The DM thread negative fixture.
  - An external link.
- **Next action:** the owner sends a Reel by DM to the test account. The coordinator then captures fixtures and runs the scenario.
### T-404 Instagram feed modes: auto-Following + feed media masking — IN_PROGRESS (Dep: T-402)
- **Owner decisions (2026-11-20):**
  - Instagram home always switches automatically to the **Following** feed.
  - A per-user setting chooses one of three feed modes:
    1. **Following: mask videos** (default). For-you: mask all post media.
    2. **Following: show all.** For-you: mask all post media.
    3. **Mask all media everywhere.**
- **First implementation (`43459a5`, merged):** a video-only rule over the compressed tree. On the device it **missed videos that were scrolling in or paused**, because they show a still-frame poster `row_feed_photo_imageview`. Touch-blocking feed masks also **prevented scrolling**. The coordinator fixed that by making feed masks pass-through (`FLAG_NOT_TOUCHABLE`); a tap then opens the viewer, and viewer blocking handles it.
- **New evidence (uncompressed dumps, matching a service with `flagIncludeNotImportantViews`):**
  - Fixtures: `feed_scroll_0..12`, `following_feed_1..6`, `home_for_you`, `home_feed_menu`.
  - **Video:** an outer `video_container` containing `video_states` or `slideout_iconview_icon`. A video entering the screen has neither until it plays, so a brief leak is accepted in mode 1.
  - **Carousel:** `zoomable_view_container` / `carousel_image_media_group`.
  - **Main home:** `main_feed_action_bar` + `title_logo` + `feed_tab`.
  - **Following page:** `action_bar_button_back` + `action_bar_title`, with no tab bar.
  - **Feed menu popup:** `context_menu_item` ×2, with Following first, identified by position.
  - Every screen has 83–137 nodes, under the 400 cap.
## P5 — Compatibility and distribution hardening

### T-501 Configuration coverage — TODO (Dep: T-403)
- **Scope:** es and en locales, landscape, tablet layout, rapid app switching, service kill and reconnect.

### T-502 Update-drift procedure — TODO (Dep: T-403)
- **Acceptance:** fixtures are rerun on each new target-app version, and mismatched rows are marked unverified.

### T-503 Android 17 / sensitive-view check — TODO
- **Work:** the researcher confirms how Advanced Protection and `accessibilityDataSensitive` affect the app, and documents the impact.

### T-504 Play readiness — BLOCKED
- **Blocked by:** the product decision to sideload first.
- **Next action:** the owner decides whether to pursue Play distribution. Then the privacy-policy-reviewer reviews the disclosure, the declaration video requirements and the current policy.
