@AGENTS.md

## Claude Code specifics

### Skills (`.claude/skills/`)
- `kotlin-feature`: when implementing one `TASKS.md` item.
- `accessibility-rule`: when creating or changing a YouTube/Instagram detection rule.
- `android-validation`: before reporting any task as done.
- `docs-sync`: after a code or workflow change.

### Subagents (`.claude/agents/`)
Skills are preloaded into agents through their `skills:` frontmatter.
- `android-platform-researcher`: unknown platform or API behavior.
- `kotlin-implementer`: one well-specified implementation task.
- `android-qa-reviewer`: review a diff before merge.
- `privacy-policy-reviewer`: changes touching permissions, data or overlays.
- `documentation-maintainer`: doc updates after a change.

Run independent read-only research and review agents in parallel. Keep edits to shared files sequential.

### Documentation map
- `PRODUCT.md`: scope, requirements, non-goals.
- `ARCHITECTURE.md`: components and data flow.
- `TASKS.md`: backlog and status.
- `docs/decisions/`: ADRs.
- `docs/TESTING.md`: test workflow and scenarios.
- `docs/COMPATIBILITY.md`: verified app/device behavior.
- `docs/PRIVACY.md`: permissions and data handling.
- `README.md`, `CONTRIBUTING.md`: setup, commits, worktrees.
