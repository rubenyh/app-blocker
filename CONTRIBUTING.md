# Contributing

## Workflow

Pick an item in [TASKS.md](TASKS.md), read its acceptance criteria and affected code, and implement the smallest complete change. Validate it, update affected documentation in the same change, then have the coordinator mark it DONE with exact checks and results, unavailable checks listed as NOT RUN, and Orca identifiers where applicable. Keep changes consistent with [PRODUCT.md](PRODUCT.md) and [ADR-0001](docs/decisions/ADR-0001-accessibility-overlay-masking.md).

Use the [documentation update mapping in AGENTS.md](AGENTS.md#documentation-maintenance-contract): update product, architecture, testing, compatibility, privacy or contributor instructions whenever the corresponding behavior, design, evidence, data handling or workflow changes.

## Validation

Run all Gradle commands from `app/` in PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The recorded 2026-11-19 baseline passed assemble and two template JVM tests. `lintDebug` is **not yet run**. `connectedDebugAndroidTest` is **not yet run** and needs a connected device or emulator; the coordinator owns device work. The current template UI test likely fails because it expects `Hello X!` while the UI renders `Hello X !`; this is a source observation, not a verified failure. T-301 schedules template test deletion. See [docs/TESTING.md](docs/TESTING.md) for manual coverage and evidence rules.

## Coding and review

- Follow Kotlin official style with the existing code's 2-space indentation.
- Keep one module and cohesive packages, following the [planned architecture](ARCHITECTURE.md).
- Reuse existing patterns; avoid speculative abstractions and DI frameworks.
- Keep detection and policy pure and testable on the JVM, with an injected clock for policy timing.
- Do not upgrade dependencies without a concrete need.
- Add meaningful positive and negative detector fixtures and policy tests for loop prevention, cooldown, retry caps and resets. Template test success is not feature coverage.

Review the full affected path, including callers, tests, resources and documentation. Verify normal-app behavior, unknown-screen handling, live toggles, overlay cleanup, accessibility and privacy boundaries. Include validation evidence and remaining risks; do not claim device compatibility from fixtures.

## Commits

Use Conventional Commits in the form `type(scope): summary`.

- Types: `feat`, `fix`, `test`, `docs`, `refactor`, `build`, `chore`, `ci`.
- Scopes: `youtube`, `instagram`, `service`, `policy`, `overlay`, `settings`, `onboarding`, `diagnostics`, `docs`, `agents`, `skills`, `build`.
- Commit regularly at each validated step. Never commit a failing build or tests. Branch-local `wip:` commits must be squashed before merge.
- Never use `--no-verify`, and never amend published commits.
- No push or PR, and no merge to `main`, without the owner's request.
- Exception for the coordinator's `--no-ff` merges of worker branches: use `merge(<task-ids>): summary`, for example `merge(T-204): ...`. Put the Orca run, task and dispatch IDs in the body.

## Worktrees and orchestration

The coordinator session works on the integration branch and owns merges, `TASKS.md`, shared files and device work. Implementation tasks run as Orca workers, each in its own child worktree and branch, created by Orca with `orca orchestration worker-start --worktree new-child` (plus the task's required arguments). Use at most **2 parallel workers**.

Assign a single owner per milestone for shared files: `AndroidManifest.xml`, `libs.versions.toml`, `app/app/build.gradle.kts`, `values/strings.xml`, `Navigation*.kt` and `TASKS.md`. Emulator and adb access is serialized to the coordinator.

Integration flow:

1. Review the diff.
2. Re-run validation in the child worktree.
3. Merge with `git merge --no-ff <worker-branch>`.
4. Re-validate on the integration branch.
5. Release the worker.

Never reset, stash or delete others' work.
