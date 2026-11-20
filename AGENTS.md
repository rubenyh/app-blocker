# AGENTS.md

Shared rules for all coding agents working in this repository.

## Project

Android (Kotlin) app that blocks YouTube Shorts and Instagram Reels. Planned design, not implemented yet (see `TASKS.md`): an AccessibilityService will press Back on viewer entry and mask entry points with overlays. This is masking, not removal.
Docs: [PRODUCT.md](PRODUCT.md), [ARCHITECTURE.md](ARCHITECTURE.md), [TASKS.md](TASKS.md), [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md).
Layout: Gradle root `app/`, module `app/app/`, sources `app/app/src/main/kotlin/com/example/appblocker/`, `applicationId` `com.appblocker.app`.

## Session start

1. Read this file and the `IN_PROGRESS` / `BLOCKED` items in `TASKS.md`.
2. Load other docs only when the task needs them.

## Commands

PowerShell, run from `app/`:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
emulator -avd Medium_Phone_API_36.1
```

`assembleDebug` and `testDebugUnitTest` were verified on 2026-11-19. `lintDebug` exists through AGP but has not been run yet.

## Boundaries

- One app module. No speculative modules, DI frameworks or generic frameworks.
- Enforcement lives in the service and never depends on an Activity.
- The service handles only the two target packages.
- Unknown screens trigger no Back and no new mask; existing masks are cleared.
- Never invent selectors or view IDs. Rules need captured evidence (see the `accessibility-rule` skill).
- No dependency upgrades without a concrete need.

## Privacy

- No text, message, credential or title capture.
- No full accessibility dumps or screenshots kept by default.
- Diagnostics are opt-in and local.

## Validation

- Report build, unit tests and real-device result separately, each as PASS, FAIL or NOT RUN with the reason.
- Fixtures are never device validation.
- Workflow: `android-validation` skill.

## Documentation maintenance contract

| Change | Document |
|---|---|
| Product behavior or scope | `PRODUCT.md` and acceptance criteria |
| Component boundaries or data flow | `ARCHITECTURE.md` |
| Consequential design choice | a new ADR in `docs/decisions/` |
| Progress or blockers | `TASKS.md` |
| Setup or commands | `README.md` / `CONTRIBUTING.md` |
| Test workflow | `docs/TESTING.md` |
| Verified app or device behavior | `docs/COMPATIBILITY.md` |
| Permissions or data handling | `docs/PRIVACY.md` |
| Repeated workflow change | `AGENTS.md`, the skill, or the agent file |

Update only affected docs, in the same change. Workflow: `docs-sync` skill.

## DONE contract

A task is DONE only when:
- Acceptance criteria are met.
- The exact checks run are recorded.
- Unavailable checks are listed as NOT RUN.
- Docs are updated.
- Anything blocked has a precise next action.

Never downgrade a `PRODUCT.md` requirement to match incomplete code.

## Delegation and orchestration

- The main session is the coordinator. It owns product decisions, integration, merges, device/emulator work and `TASKS.md`.
- Implementation runs as supervised Orca workers in child worktrees.
- Shared files have a single owner per milestone: `AndroidManifest.xml`, `libs.versions.toml`, `app/app/build.gradle.kts`, `values/strings.xml`, `Navigation*.kt`, `TASKS.md`.
- The emulator/adb is a single shared resource. Only the coordinator uses it, unless a worker brief explicitly delegates device work.
- Run at most 2 parallel workers.
- Details: [CONTRIBUTING.md](CONTRIBUTING.md#worktrees-and-orchestration).

## Commits

- Conventional Commits: `type(scope): summary`.
- Types: `feat`, `fix`, `test`, `docs`, `refactor`, `build`, `chore`, `ci`.
- Scopes: `youtube`, `instagram`, `service`, `policy`, `overlay`, `settings`, `onboarding`, `diagnostics`, `docs`, `agents`, `skills`, `build`.
- Commit at each validated step. Never commit a failing build or tests.
- Branch-local `wip:` commits are allowed but must be squashed before merge.
- No push, PR, or merge to `main` unless the owner asks.
- Details: [CONTRIBUTING.md](CONTRIBUTING.md#commits).

## Workflow procedures

Step-by-step procedures live in `.claude/skills/<name>/SKILL.md`. Any agent, including non-Claude workers, should read the one matching its task:
- `kotlin-feature`: code changes.
- `accessibility-rule`: detection rules and evidence capture.
- `android-validation`: checks and reporting.
- `docs-sync`: documentation updates.

The Claude-specific subagent index is in `CLAUDE.md`.
