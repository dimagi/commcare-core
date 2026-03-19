# Formplayer Branch Elimination — Design Spec

## Background

commcare-core historically maintained two branches (`master` and `formplayer`) for commcare-android and formplayer respectively. With cross-repo CI in place (PR #1525), both downstream repos are tested against commcare-core PRs automatically. The branches are already reconciled and the `formplayer` branch has been deleted. What remains is cleaning up the infrastructure that supported the dual-branch workflow.

## Approach

Sequential PRs across multiple repos, respecting dependency order.

## Phase 1: Formplayer Repo PR

Update the formplayer repo so its commcare-core submodule tracks `master` instead of the now-deleted `formplayer` branch, and update documentation.

### Changes

- **`.gitmodules`** — change `branch = formplayer` to `branch = master`
- **`libs/commcare`** — update submodule pointer to current `master` HEAD of commcare-core
- **`README.md`** (lines 162-169) — rewrite the "Contributing changes to commcare" and "Updating the CommCare version" sections. Remove references to commcare-core's `master` branch being "not stable" and the `formplayer` branch being the target. Update to reflect that `master` is the single branch used by both downstream repos.

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

## Phase 3: staging-branches Repo PR

Update the formplayer staging config to track `master` instead of `formplayer` for the commcare-core submodule.

### Changes

- **`formplayer-staging.yml`** (line 9) — change `trunk: formplayer` to `trunk: master` under `submodules: libs/commcare:`
- Any feature branches referencing the formplayer branch naming convention (e.g., `masterToFormplayer2.60`) should be reviewed and updated or removed as appropriate

## Phase 4: commcare-hq PR

Update documentation that references the dual-branch strategy.

### Changes

- **`docs/formplayer.rst`** (line 25) — remove the statement "Mobile uses the `master` branch, while formplayer uses the `formplayer` branch. The two branches have a fairly small diff." Replace with a note that both downstream repos use `master`.

## Phase 5: Close commcare-android test PR #2612

PR #2612 on commcare-android is a standing "test PR" used as a manual harness for testing commcare-core changes against Android. This workflow is replaced by cross-repo CI. Close the PR with a comment explaining the replacement.

## Sequencing

1. Merge Phase 1 (formplayer submodule + README update)
2. Merge Phase 2 (commcare-core cleanup)
3. Merge Phase 3 (staging-branches config update)
4. Merge Phase 4 (commcare-hq docs update)
5. Phase 5 (close commcare-android test PR)

Phases 3-5 have no strict ordering dependency on each other and can be done in parallel after Phase 2.

Order matters because formplayer's `.gitmodules` currently references the `formplayer` branch. Although the branch is already deleted (so this is already broken), updating formplayer first ensures a clean transition.

## Out of Scope

- The `formplayer` branch has already been deleted from the commcare-core remote — no branch deletion step is needed.
- No code reconciliation is needed — the branches were already in sync.
- The `trigger-downstream` job in `build.yml` (lines 40-121) requires no changes — it is gated on `pull_request` events, not branch names.
