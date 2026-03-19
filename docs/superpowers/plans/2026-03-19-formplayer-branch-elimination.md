# Formplayer Branch Elimination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all references to the now-deleted commcare-core `formplayer` branch across all Dimagi repos, completing the transition to a single `master` branch with cross-repo CI.

**Architecture:** Five phases across four repos plus one PR closure. Phases 1-2 are sequential (formplayer submodule must update before commcare-core cleanup). Phases 3-5 can run in parallel after Phase 2.

**Spec:** `docs/superpowers/specs/2026-03-19-formplayer-branch-elimination-design.md`

---

### Task 1: Update formplayer submodule to track master

**Repo:** `dimagi/formplayer`

**Files:**
- Modify: `.gitmodules:4`
- Update: `libs/commcare` (submodule pointer)

- [ ] **Step 1: Create branch in formplayer repo**

```bash
cd /Users/danielroberts/dimagi/formplayer
git checkout master && git pull
git checkout -b dmr/track-commcare-core-master
```

- [ ] **Step 2: Update .gitmodules to track master**

In `.gitmodules`, change line 4:
```
branch = formplayer
```
to:
```
branch = master
```

- [ ] **Step 3: Update submodule pointer to master HEAD**

```bash
cd /Users/danielroberts/dimagi/formplayer
cd libs/commcare
git fetch origin
git checkout origin/master
cd ../..
git add .gitmodules libs/commcare
```

- [ ] **Step 4: Commit**

```bash
git commit -m "Point commcare-core submodule at master branch

The formplayer branch has been deleted. Both commcare-android and
formplayer now use master, with cross-repo CI ensuring compatibility."
```

---

### Task 2: Update formplayer README

**Repo:** `dimagi/formplayer`

**Files:**
- Modify: `README.md:160-169`

- [ ] **Step 1: Rewrite the "Contributing changes to commcare" section**

Replace lines 160-169 of `README.md`:

```markdown
#### Contributing changes to commcare

Formplayer also has a dependency on the commcare-core repository. The commcare-core `master` branch is not
stable and Formplayer uses a different branch. The submodule repo `libs/commcare` should always be pointing to
the `formplayer` branch.

#### Updating the CommCare version

When updating Formplayer to have a new release of a CommCare version (e.g. 2.34 to 2.35), a PR should be opened from the `commcare_X.Y` branch into
the `formplayer` branch. Once QA has been finished, merge the PR and update the Formplayer submodule.
```

with:

```markdown
#### Contributing changes to commcare

Formplayer depends on the commcare-core repository via the `libs/commcare` submodule, which tracks the `master` branch. Cross-repo CI automatically tests formplayer against commcare-core PRs, so no manual cross-testing is needed.

#### Updating the CommCare version

When updating Formplayer to a new CommCare version (e.g. 2.34 to 2.35), open a PR from the `commcare_X.Y` branch into `master` on commcare-core. Once QA has finished, merge the PR and update the Formplayer submodule.
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "Update README to reflect single-branch commcare-core workflow"
```

- [ ] **Step 3: Push and create PR**

```bash
git push -u origin dmr/track-commcare-core-master
gh pr create --repo dimagi/formplayer \
  --title "Point commcare-core submodule at master branch" \
  --body "$(cat <<'EOF'
## Summary
- Update `.gitmodules` to track `master` instead of the deleted `formplayer` branch
- Update submodule pointer to current `master` HEAD
- Update README to reflect the single-branch workflow and cross-repo CI

## Context
Part of the formplayer branch elimination effort. See commcare-core `docs/formplayer-branch-elimination.md`.

## Test plan
- [ ] CI passes with submodule pointing at master
- [ ] README accurately describes the new workflow
EOF
)"
```

---

> **Merge gate:** The formplayer PR (Tasks 1-2) must be merged before proceeding to Tasks 3-7. This ensures the submodule tracks `master` before the commcare-core cleanup lands.

### Task 3: Delete duplicate PR workflow and script from commcare-core

**Repo:** `dimagi/commcare-core`

**Files:**
- Delete: `.github/workflows/duplicate_pr.yml`
- Delete: `scripts/duplicate_pr.py`

- [ ] **Step 1: Create branch**

```bash
cd /Users/danielroberts/dimagi/commcare-core
git checkout master && git pull
git checkout -b dmr/eliminate-formplayer-branch
```

- [ ] **Step 2: Delete the files**

```bash
git rm .github/workflows/duplicate_pr.yml scripts/duplicate_pr.py
```

- [ ] **Step 3: Commit**

```bash
git commit -m "Delete duplicate PR workflow and script

The duplicate_pr workflow automated cherry-picking PRs between the
master and formplayer branches. With the formplayer branch eliminated
and cross-repo CI in place, this is no longer needed."
```

---

### Task 4: Clean up build.yml

**Repo:** `dimagi/commcare-core`

**Files:**
- Modify: `.github/workflows/build.yml:7,11,19-20,23-27`

- [ ] **Step 1: Remove formplayer from push triggers**

In `.github/workflows/build.yml`, remove line 7:
```yaml
      - 'formplayer'
```

- [ ] **Step 2: Remove formplayer from pull_request triggers**

Remove line 11 (after the previous edit, this will be at a different line number — it's the `- 'formplayer'` under `pull_request: branches:`):
```yaml
      - 'formplayer'
```

- [ ] **Step 3: Remove formplayer-specific env vars**

Remove these two lines from the `env:` block of the `build` job:
```yaml
      FORMPLAYER_GRADLE_VERSION: 8.1
      FORMPLAYER_JAVA_VERSION: '17'
```

- [ ] **Step 4: Remove the "Set environment for Formplayer" step**

Remove this entire step:
```yaml
      - name: Set environment for Formplayer
        if: ${{ github.ref_name == 'formplayer' || github.base_ref == 'formplayer' }}
        run: |
          echo "JOB_GRADLE_VERSION=${{ env.FORMPLAYER_GRADLE_VERSION }}" >> "$GITHUB_ENV"
          echo "JAVA_VERSION=${{ env.FORMPLAYER_JAVA_VERSION }}" >> "$GITHUB_ENV"
```

- [ ] **Step 5: Verify the resulting build.yml**

The `build` job should now look like:
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    env:
      JAVA_VERSION: '17'
      JOB_GRADLE_VERSION: 8.1
    steps:
      - uses: actions/checkout@v6
      - name: Set up Java
        uses: actions/setup-java@v5
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'adopt'
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v5
        with:
          gradle-version: ${{ env.JOB_GRADLE_VERSION }}
      - name: Build with Gradle
        run: gradle build
```

The `trigger-downstream` job (lines 40-122) should be **unchanged**.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "Remove formplayer branch logic from build workflow

Remove formplayer from branch triggers, remove formplayer-specific
Gradle/Java version env vars and the conditional step that applied them.
The trigger-downstream job is unchanged."
```

---

### Task 5: Clean up dependabot.yml

**Repo:** `dimagi/commcare-core`

**Files:**
- Modify: `.github/dependabot.yml:9-17`

- [ ] **Step 1: Remove the formplayer branch config block**

Remove lines 9-17 (the blank line plus the entire second config block) from `.github/dependabot.yml`:
```yaml

  # Copy of above config for formplayer
  - package-ecosystem: gradle
    directory: "/"
    target-branch: "formplayer"
    schedule:
      interval: daily
    labels:
      - dependencies
```

The file should now contain only the single `master` config (the first block, which has no explicit `target-branch` since master is the default).

- [ ] **Step 2: Commit**

```bash
git add .github/dependabot.yml
git commit -m "Remove formplayer branch from dependabot config"
```

---

### Task 6: Clean up PR template

**Repo:** `dimagi/commcare-core`

**Files:**
- Modify: `.github/PULL_REQUEST_TEMPLATE.md:57-59`

- [ ] **Step 1: Remove the "Duplicate PR" section**

Remove lines 57-59 from `.github/PULL_REQUEST_TEMPLATE.md`:
```markdown

### Duplicate PR
Automatically duplicate this PR as defined in [contributing.md](https://github.com/dimagi/commcare-core/blob/master/.github/contributing.md).
```

- [ ] **Step 2: Commit**

```bash
git add .github/PULL_REQUEST_TEMPLATE.md
git commit -m "Remove Duplicate PR section from PR template"
```

---

### Task 7: Rewrite contributing.md

**Repo:** `dimagi/commcare-core`

**Files:**
- Modify: `.github/contributing.md` (full rewrite)

- [ ] **Step 1: Replace the entire file content**

Replace `.github/contributing.md` with:

```markdown
### Contributing to CommCare Core

[Formplayer](https://github.com/dimagi/formplayer) and [CommCare Android](https://github.com/dimagi/commcare-android) both use commcare-core as the underlying XForm engine.

#### Creating a PR

All PRs should target the `master` branch. When a PR is opened, CI automatically:

1. Runs the commcare-core test suite
2. Triggers cross-repo CI to test the change against both [commcare-android](https://github.com/dimagi/commcare-android) and [formplayer](https://github.com/dimagi/formplayer)

Cross-repo test results are reported as commit statuses on your PR. If a downstream test fails, you may need to coordinate a fix in the affected repo.

#### PR expectations

- Include a clear description of what the change does and why
- Ensure all CI checks pass before requesting review
- If your change affects formplayer or commcare-android behavior, note this in the PR description

#### Running tests locally

```bash
gradle build
```
```

- [ ] **Step 2: Commit**

```bash
git add .github/contributing.md
git commit -m "Rewrite contributing guide for single-branch workflow

Replace the dual-branch PR duplication process with a description of the
new single-branch workflow backed by cross-repo CI."
```

- [ ] **Step 3: Push and create PR**

```bash
git push -u origin dmr/eliminate-formplayer-branch
gh pr create --repo dimagi/commcare-core \
  --title "Remove formplayer branch infrastructure" \
  --body "$(cat <<'EOF'
## Summary
- Delete `duplicate_pr.yml` workflow and `duplicate_pr.py` script
- Remove formplayer branch from `build.yml` triggers and conditional logic
- Remove formplayer branch from `dependabot.yml`
- Remove "Duplicate PR" section from PR template
- Rewrite `contributing.md` for single-branch workflow with cross-repo CI

## Context
The `formplayer` branch has been deleted and cross-repo CI (PR #1525) now tests
both downstream repos automatically. This PR removes all remaining infrastructure
that supported the dual-branch workflow.

## Test plan
- [ ] CI build passes (build.yml changes are correct)
- [ ] No remaining references to `formplayer` branch in this repo
- [ ] contributing.md accurately describes the new workflow
EOF
)"
```

---

### Task 8: Update staging-branches config

**Repo:** `dimagi/staging-branches`

**Files:**
- Modify: `formplayer-staging.yml:9,11`

- [ ] **Step 1: Create branch**

```bash
cd /Users/danielroberts/dimagi/staging-branches
git checkout master && git pull
git checkout -b dmr/formplayer-staging-track-master
```

- [ ] **Step 2: Update trunk to master**

In `formplayer-staging.yml`, change line 9:
```yaml
    trunk: formplayer
```
to:
```yaml
    trunk: master
```

- [ ] **Step 3: Review feature branches**

Line 11 references `masterToFormplayer2.60` — a branch name tied to the old dual-branch convention. Check if this branch is still needed. If the corresponding feature (`dmr/add-getBulkIdsForIndex-to-SqlStorage` on line 6) has been merged or the staging branch is no longer needed, remove both entries. If it's still active, rename/update the commcare-core branch to target master and update the reference here.

- [ ] **Step 4: Commit**

```bash
git add formplayer-staging.yml
git commit -m "Update formplayer staging to track commcare-core master

The formplayer branch has been eliminated. The libs/commcare submodule
now tracks master."
```

- [ ] **Step 5: Push and create PR**

```bash
git push -u origin dmr/formplayer-staging-track-master
gh pr create --repo dimagi/staging-branches \
  --title "Track commcare-core master in formplayer staging" \
  --body "$(cat <<'EOF'
## Summary
- Change `libs/commcare` trunk from `formplayer` to `master`
- Review/update feature branch references that used the old naming convention

## Context
Part of the formplayer branch elimination effort.

## Test plan
- [ ] Next staging rebuild succeeds with the updated config
EOF
)"
```

---

### Task 9: Update commcare-hq docs

**Repo:** `dimagi/commcare-hq`

**Files:**
- Modify: `docs/formplayer.rst:25`

- [ ] **Step 1: Create branch**

```bash
cd /Users/danielroberts/dimagi/commcare-hq
git checkout master && git pull
git checkout -b dmr/update-formplayer-docs
```

- [ ] **Step 2: Update the commcare-core description**

In `docs/formplayer.rst`, replace line 25:
```rst
* `commcare-core <https://github.com/dimagi/commcare-core>`_: The CommCare engine, this powers both CommCare mobile and formplayer. Mobile uses the ``master`` branch, while formplayer uses the ``formplayer`` branch. The two branches have a fairly small diff.
```
with:
```rst
* `commcare-core <https://github.com/dimagi/commcare-core>`_: The CommCare engine, this powers both CommCare mobile and formplayer. Both use the ``master`` branch.
```

- [ ] **Step 3: Commit**

```bash
git add docs/formplayer.rst
git commit -m "Update formplayer docs: commcare-core uses single master branch"
```

- [ ] **Step 4: Push and create PR**

```bash
git push -u origin dmr/update-formplayer-docs
gh pr create --repo dimagi/commcare-hq \
  --title "Update formplayer docs for single-branch commcare-core" \
  --body "$(cat <<'EOF'
## Summary
- Remove reference to the deleted `formplayer` branch of commcare-core

## Context
Part of the formplayer branch elimination effort. Both commcare-android and formplayer
now use commcare-core's `master` branch, with cross-repo CI ensuring compatibility.

## Test plan
- [ ] Docs render correctly
EOF
)"
```

---

### Task 10: Close commcare-android test PR #2612

**Repo:** `dimagi/commcare-android`

- [ ] **Step 1: Close the PR with a comment**

```bash
gh pr comment 2612 --repo dimagi/commcare-android --body "$(cat <<'EOF'
Closing this standing test PR. Cross-repo CI now automatically tests commcare-core
PRs against commcare-android, making this manual testing harness unnecessary.

See commcare-core PR #1525 for details on the cross-repo CI setup.
EOF
)"
gh pr close 2612 --repo dimagi/commcare-android
```
