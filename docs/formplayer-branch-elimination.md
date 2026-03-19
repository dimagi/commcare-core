# Eliminating the formplayer branch

## Background

commcare-core has historically maintained two branches:
- `master` — used by commcare-android
- `formplayer` — used by formplayer

These branches have diverged over time, causing compatibility issues. With
cross-repo CI now in place (PR #1525), both downstream repos are tested against
commcare-core PRs automatically, making the dual-branch strategy unnecessary.

## Goal

Eliminate the `formplayer` branch entirely so that both commcare-android and
formplayer work off `master`. The `formplayer` branch and `master` are already
reconciled — no merge or code reconciliation is needed. This is purely an
infrastructure cleanup: remove references to the branch, delete the branch,
and point formplayer's submodule at `master`.

## Current state of the formplayer branch workflow

### commcare-core

**`.github/workflows/build.yml`**
- `push` trigger includes `formplayer` branch (line 7)
- `pull_request` trigger includes `formplayer` branch (line 11)
- `build` job has conditional logic for formplayer-specific Java/Gradle versions
  (lines 23-27): detects `formplayer` branch and sets `FORMPLAYER_GRADLE_VERSION`
  and `FORMPLAYER_JAVA_VERSION` (currently both match master's values: Gradle 8.1,
  Java 17)

**`.github/workflows/duplicate_pr.yml`**
- Entire workflow exists to duplicate PRs between `master` and `formplayer` branches
- Uses `scripts/duplicate_pr.py` to cherry-pick commits
- Can be deleted entirely

**`.github/contributing.md`**
- Documents the dual-branch workflow and PR duplication process
- Needs to be updated to remove formplayer branch references

**`.github/dependabot.yml`**
- Has separate dependency update configs for both `master` and `formplayer` branches
- Remove the `formplayer` branch config

**`.github/PULL_REQUEST_TEMPLATE.md`**
- Has a "Duplicate PR" section at the bottom referencing `contributing.md`
- Can be removed or simplified

### formplayer repo

**`.gitmodules`**
- Submodule tracks `formplayer` branch of commcare-core:
  ```
  [submodule "libs/commcare"]
    path = libs/commcare
    url = https://github.com/dimagi/commcare-core.git
    branch = formplayer
  ```
- Needs to be changed to track `master`

## Files to modify/delete in commcare-core

1. `.github/workflows/build.yml` — remove `formplayer` from branch triggers,
   remove formplayer-specific env vars and conditional logic
2. `.github/workflows/duplicate_pr.yml` — delete entirely
3. `scripts/duplicate_pr.py` — delete entirely (used only by duplicate_pr.yml)
4. `.github/contributing.md` — rewrite to remove dual-branch workflow
5. `.github/dependabot.yml` — remove `formplayer` branch config
6. `.github/PULL_REQUEST_TEMPLATE.md` — remove "Duplicate PR" section

## Files to modify in formplayer

1. `.gitmodules` — change branch from `formplayer` to `master`
2. `libs/commcare` submodule — update to point at `master`

## Sequencing

1. Update formplayer's submodule to track `master`
2. Remove formplayer branch infrastructure from commcare-core
3. Delete the `formplayer` branch on commcare-core remote
