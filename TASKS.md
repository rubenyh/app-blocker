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

### T-301 Settings + DataStore — TODO
- **Work:**
  - Replace the template `ui/main` and `data/` with `settings/SettingsRepository` (DataStore Preferences) and a settings screen.
  - Delete the template tests.
- **Acceptance:** the settings unit test passes, and the toggles persist across app restart.

### T-302 Service skeleton + onboarding — TODO (Dep: T-301)
- **Work:**
  - T-302a: the service, `accessibility_service_config.xml` scoped to both packages, and the manifest `<service>` with `BIND_ACCESSIBILITY_SERVICE`.
  - T-302b: onboarding with prominent disclosure, consent, restricted-settings help and service status.
  - Create `docs/PRIVACY.md`.
- **Acceptance:**
  - The service enables on the emulator.
  - It receives YouTube events.
  - It logs only the package and event type.
- **Open design point (resolve with evidence in T-302a):**
  - With `packageNames` restricted to the two targets, the service may not receive an event when the user switches to another app. Overlay cleanup and policy reset depend on that event.
  - Options to test:
    - drop the `packageNames` filter, filter in code by event package and read nothing from other apps;
    - `flagRetrieveInteractiveWindows` with `TYPE_WINDOWS_CHANGED`.
  - Record the chosen option in ARCHITECTURE.md and PRIVACY.md.

### T-303 YouTube evidence capture — TODO (Dep: T-302)
- **Needs the owner** to install YouTube on AVD `Medium_Phone_API_36.1` and sign in with a test account, or use it signed out.
- **Acceptance:** redacted fixtures exist for the Shorts viewer, Shorts tab, Shorts shelf, a normal video, the home feed, search results and a channel page. Redacted means view IDs, classes, flags and bounds only.

### T-304 YouTube rules + BlockPolicy — TODO (Dep: T-303)
- **Acceptance:** unit tests cover these cases:
  - viewer gives exactly one Back;
  - entry point never gives Back;
  - unknown does nothing;
  - an event burst produces one action;
  - cooldown;
  - retry cap;
  - reset on package switch.

### T-305 YouTube device validation (behavior A) — TODO (Dep: T-304)
- **Acceptance:** the docs/TESTING YouTube scenarios pass on the emulator, and `docs/COMPATIBILITY.md` has a measured row.

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
