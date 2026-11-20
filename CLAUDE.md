@AGENTS.md

## Claude Code specifics

Invoke the project skills listed in AGENTS.md "Workflow procedures" with the Skill tool. Run `android-validation` before reporting any task done.

### Subagents (`.claude/agents/`)
Skills are preloaded into agents through their `skills:` frontmatter.
- `android-platform-researcher`: unknown platform or API behavior.
- `kotlin-implementer`: one well-specified implementation task.
- `android-qa-reviewer`: review a diff before merge.
- `privacy-policy-reviewer`: changes touching permissions, data or overlays.
- `documentation-maintainer`: doc updates after a change.

Run independent read-only research and review subagents in parallel. The 2-worker cap applies to editing Orca workers. Keep edits to shared files sequential. For which document to update, use the contract table in AGENTS.md.
