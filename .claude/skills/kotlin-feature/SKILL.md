---
name: kotlin-feature
description: Implement one TASKS.md item in this project's Kotlin/Compose/AccessibilityService code. Use for any code change that has acceptance criteria in TASKS.md.
---

# kotlin-feature

Implement exactly one `TASKS.md` item. Rules live in `AGENTS.md`; this is the workflow.

## 1. Read before editing
1. Read the task ID, its acceptance criteria and its dependencies in `TASKS.md`.
2. Read every file you will touch, and its neighbors in `app/app/src/main/kotlin/com/example/appblocker/`.
3. Read `PRODUCT.md` and `ARCHITECTURE.md` sections the task cites. Skip the rest.
4. If the task needs a selector or view ID, stop and use the `accessibility-rule` skill. Do not guess.
5. Confirm file ownership. Do not touch shared files (see `AGENTS.md`, Delegation) unless the task owns them.

## 2. Make the smallest cohesive change
1. Reuse existing types and helpers before writing new ones.
2. No new module, DI framework, generic framework or dependency unless the task requires it.
3. Keep enforcement in the service. Never make it depend on an Activity.
4. Keep pure logic (policy, rule matching) free of Android types so JVM tests can cover it.
5. Handle only the two target packages. Unknown screens produce no action.
6. Do not capture text, titles or messages (see `AGENTS.md`, Privacy).

## 3. Lifecycle checklist
Check each item and fix or justify:
- [ ] Service connect: state initializes from current settings.
- [ ] Service interrupt and unbind: overlays removed, listeners released.
- [ ] Coroutine scopes are cancelled on destroy or unbind.
- [ ] Overlay cleanup: every view added to `WindowManager` has a matching removal on every exit path.
- [ ] Configuration changes (rotation, density, dark mode): overlays reposition or rebuild.
- [ ] Stale nodes: `AccessibilityNodeInfo` is read once, never cached across events, and recycled where the API level needs it.
- [ ] Event bursts: processing is cheap, debounced or idempotent. No Back loops.
- [ ] Settings change while the target app is open: the new value applies without restart.

## 4. Tests
1. Write focused JVM unit tests for logic, in `app/app/src/test/`.
2. Cover the happy path, one negative case and each edge case the checklist exposed.
3. Build fixtures with Kotlin test builders. Add no JSON library.
4. Fixtures are not device validation. Never describe them as such.

## 5. Validate
Run the `android-validation` skill. Record Build, Unit tests and Device as PASS, FAIL or NOT RUN.

## 6. Documentation impact
List affected docs using the contract table in `AGENTS.md`. Update the ones in your ownership. Report the rest to the coordinator. The `docs-sync` skill covers the update flow.

## 7. Commit
1. Commit only if the brief authorizes it, and only after checks pass.
2. Use Conventional Commits: `type(scope): summary`.
3. Do not push, open a PR or merge.

## Report
- Diff summary.
- Checks run and results.
- Doc impact list.
- Open issues and a precise next action for anything blocked.
