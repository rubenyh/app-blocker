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

### T-306 YouTube entry-point masking (behavior B) — TODO (Dep: T-305)
- **Acceptance:**
  - Masks follow scroll and rotation.
  - Masks clear on leave and on toggle-off.
  - Taps underneath are blocked.
  - The gap and lag are measured and recorded honestly.

### T-307 Opt-in diagnostics — TODO (Dep: T-304)
- **Acceptance:**
  - Diagnostics are off by default.
  - They are an in-memory ring buffer that the user can view and clear.
  - PRIVACY is updated.

## P4 — Instagram

### T-401 Instagram evidence capture — TODO (Dep: T-302)
- **Acceptance:** fixtures exist for:
  - the Reels tab;
  - a Reel opened from the feed, a profile, a DM and an external link;
  - negative screens: DM thread, profile grid, stories, home feed, search and a normal (non-Reel) video post.

### T-402 Instagram rules — TODO (Dep: T-401, T-304)
- **Acceptance:**
  - Unit tests pass.
  - A Reel opened from a DM goes Back to the DM.
  - A DM is never blocked.

### T-403 Instagram device validation + Reels-tab masking — TODO (Dep: T-402, T-306)
- **Acceptance:** scenarios pass, and `docs/COMPATIBILITY.md` rows are added.

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
