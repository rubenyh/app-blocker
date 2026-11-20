---
name: docs-sync
description: Update project documentation after a code or workflow change. Use when a diff changes behavior, commands, permissions, tests, architecture or task status.
---

# docs-sync

Keep docs true to the code. The mapping table is in `AGENTS.md` (Documentation maintenance contract). Do not copy it here.

## 1. Read the change
1. Run `git diff` for the working tree, or `git diff <base>...HEAD` for a branch.
2. Run `git log --oneline <base>..HEAD` for the commit range.
3. Run `git status` to spot untracked files.

## 2. Map changes to docs
1. For each change, pick the document from the `AGENTS.md` contract table.
2. Write the list of affected docs before editing.
3. Skip docs the change does not affect.

## 3. Edit only affected docs
1. Make minimal, accurate edits in the same change.
2. State only facts the diff or recorded evidence supports.
3. Do not invent features, results, versions or view IDs.
4. Keep each rule in one place. Link instead of copying.

## 4. Update TASKS.md
1. Set status and evidence for the task.
2. Record only verified results, with the exact check and its outcome.
3. If the coordinator owns `TASKS.md` in this session, report the proposed edit instead of editing.

## 5. Report stale or conflicting docs
List docs that contradict the code or each other. Give file, section and the conflict. Fix only what is in scope; report the rest.

## 6. Hard rules
1. Never mark unverified work DONE. Unavailable checks are NOT RUN.
2. Never downgrade a `PRODUCT.md` requirement to match incomplete code. Report the gap.
3. A consequential design choice needs a new ADR, not an edit to an accepted one.

## 7. Commit
1. Commit as `docs(<scope>): <summary>`.
2. Do not push, open a PR or merge.

## Report
- Docs changed and why.
- Stale or conflicting docs found.
- Anything left NOT RUN.
