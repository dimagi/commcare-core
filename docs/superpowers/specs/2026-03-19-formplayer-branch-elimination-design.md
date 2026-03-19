# Formplayer Branch Elimination — Design Spec

## Background

commcare-core historically maintained two branches (`master` and `formplayer`) for commcare-android and formplayer respectively. With cross-repo CI in place (PR #1525), both downstream repos are tested against commcare-core PRs automatically. The branches are already reconciled and the `formplayer` branch has been deleted. What remains is cleaning up the infrastructure that supported the dual-branch workflow.

## Approach

Two sequential PRs using Approach 1 (sequential PRs respecting dependency order).

## Phase 1: Formplayer Repo PR

Update the formplayer repo so its commcare-core submodule tracks `master` instead of the now-deleted `formplayer` branch.

### Changes

- **`.gitmodules`** — change `branch = formplayer` to `branch = master`
- **`libs/commcare`** — update submodule pointer to current `master` HEAD of commcare-core

## Phase 2: commcare-core PR

Remove all formplayer branch infrastructure from commcare-core in a single PR.

### Deletions

- **`.github/workflows/duplicate_pr.yml`** — entire workflow for duplicating PRs between branches; no longer needed
- **`scripts/duplicate_pr.py`** — script used only by duplicate_pr.yml

### Modifications

- **`.github/workflows/build.yml`**
  - Remove `formplayer` from `push` branch triggers (line 7)
  - Remove `formplayer` from `pull_request` branch triggers (line 11)
  - Remove `FORMPLAYER_GRADLE_VERSION` and `FORMPLAYER_JAVA_VERSION` env vars (lines 19-20)
  - Remove the "Set environment for Formplayer" conditional step (lines 23-27)

- **`.github/dependabot.yml`** — remove the second gradle config block targeting the `formplayer` branch (lines 10-17)

- **`.github/PULL_REQUEST_TEMPLATE.md`** — remove the "Duplicate PR" section (lines 58-59)

- **`.github/contributing.md`** — full rewrite:
  - Describe the single-branch workflow: all PRs target `master`
  - Explain that cross-repo CI automatically tests both commcare-android and formplayer
  - Include brief contributing guidance (how to run tests, PR expectations)

## Sequencing

1. Merge Phase 1 (formplayer submodule update)
2. Merge Phase 2 (commcare-core cleanup)

Order matters because formplayer's `.gitmodules` currently references the `formplayer` branch. Although the branch is already deleted (so this is already broken), updating formplayer first ensures a clean transition.

## Out of Scope

- The `formplayer` branch has already been deleted from the commcare-core remote — no branch deletion step is needed.
- No code reconciliation is needed — the branches were already in sync.
