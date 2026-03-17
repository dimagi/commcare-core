# Cross-Repo CI for commcare-core

## Problem

When a commcare-core PR passes its own tests, there is no automated way to verify
it won't break downstream consumers (commcare-android and formplayer). Developers
currently coordinate this manually by creating dummy PRs in downstream repos with
updated commcare-core references. This is slow, error-prone, and easy to skip.

## Solution

After commcare-core's own tests pass on a PR, automatically trigger the test suites
of commcare-android and formplayer against the PR's version of commcare-core. Report
results back as commit statuses on the commcare-core PR.

## Architecture

### Flow

```
PR opened/updated on commcare-core (master branch)
  -> build.yml runs commcare-core tests
  -> on success, trigger-downstream.yml runs:
      1. Verifies the build succeeded (workflow_run fires on any conclusion)
      2. Filters to PR events only
      3. Sets "pending" commit statuses for android + formplayer
      4. Triggers workflow_dispatch on commcare-android (default branch)
      5. Triggers workflow_dispatch on formplayer (default branch)
      6. Exits (if dispatch fails, sets "error" status instead)

commcare-android workflow:
  -> clones commcare-core at the provided SHA
  -> runs its tests
  -> reports success/failure as commit status on commcare-core PR

formplayer workflow:
  -> clones commcare-core at the provided SHA
  -> runs its tests
  -> reports success/failure as commit status on commcare-core PR
```

### Components

#### 1. commcare-core: `trigger-downstream.yml` (new workflow)

Trigger: `workflow_run` — fires after the `CommCare Core Build` workflow completes.
The workflow name must match exactly as defined in `build.yml`.

```yaml
on:
  workflow_run:
    workflows: ["CommCare Core Build"]
    types: [completed]
```

**Important:** `workflow_run` fires on any conclusion (success, failure, cancelled).
The workflow must explicitly check `github.event.workflow_run.conclusion == 'success'`
as a job-level condition before proceeding.

Behavior:
- Checks that the triggering build succeeded
- Filters to PR events only by checking `github.event.workflow_run.pull_requests`.
  This field is populated for same-repo PRs (branches within dimagi/commcare-core).
  Fork-based PRs are not supported — this is an acceptable limitation since Dimagi
  uses same-repo branches for development.
- Authenticates as the GitHub App using `actions/create-github-app-token`
- Sets two **pending** commit statuses on the PR's head SHA (with descriptive
  messages like "Waiting for commcare-android tests..."):
  - `cross-repo / commcare-android`
  - `cross-repo / formplayer`
- Fires `workflow_dispatch` to `dimagi/commcare-android` and `dimagi/formplayer`
  targeting their default branch, passing these inputs:
  - `commcare_core_sha` — the PR's head SHA
  - `commcare_core_pr` — the PR number (for linking back in status descriptions)
  - `commcare_core_check_name` — the status check name (e.g. `cross-repo / commcare-android`)
- If a `workflow_dispatch` call fails (HTTP error), sets the corresponding commit
  status to `error` with a description indicating the dispatch failed

No polling. The workflow exits immediately after dispatching.

**Rapid pushes:** Each push to a PR produces a new head SHA with its own set of
commit statuses. Previous SHAs' statuses become irrelevant once a new commit is
pushed, so concurrent dispatches are not a problem.

#### 2. Downstream workflow changes (commcare-android + formplayer)

Each downstream repo's test workflow gains a `workflow_dispatch` trigger:

```yaml
on:
  workflow_dispatch:
    inputs:
      commcare_core_sha:
        description: 'commcare-core SHA to test against'
        required: true
      commcare_core_pr:
        description: 'commcare-core PR number for status reporting'
        required: true
      commcare_core_check_name:
        description: 'Status check name to report back'
        required: true
```

When triggered with these inputs:
1. Check out the downstream repo as normal
2. Clone commcare-core at `commcare_core_sha` instead of the default resolution.
   The exact mechanism is implementation-specific per repo — each repo has its own
   logic for determining which commcare-core commit to check out, and that logic
   must be overridden when these inputs are present.
3. Run the existing test suite
4. On completion, authenticate as the GitHub App and set a commit status on
   `dimagi/commcare-core` at `commcare_core_sha`:
   - State: `success` or `failure`
   - Context: value of `commcare_core_check_name`
   - `target_url`: `${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}`
   - `description`: brief summary (e.g. "Tests passed" or "Tests failed")

When triggered without inputs (existing push/PR triggers): behavior is unchanged.
The workflow checks whether `commcare_core_sha` is provided and branches accordingly.

#### 3. GitHub App

A dedicated GitHub App (e.g. "CommCare Cross-Repo CI") with minimal permissions:

- **Actions**: write — to trigger `workflow_dispatch` on downstream repos
- **Commit statuses**: write — to set pending/success/failure on commcare-core

Installed on all three repos: `commcare-core`, `commcare-android`, `formplayer`.

App ID and private key stored as **repo-level secrets** in each repo (not org-level)
to limit the scope of granted permissions.

Secrets required per repo:
- `CROSS_REPO_CI_APP_ID` — the GitHub App's ID
- `CROSS_REPO_CI_PRIVATE_KEY` — the GitHub App's private key

Tokens are generated at workflow runtime using `actions/create-github-app-token`,
producing short-lived installation tokens.

#### 4. Branch protection (deferred)

After a stabilization period (roughly one week), add `cross-repo / commcare-android`
and `cross-repo / formplayer` as required status checks on commcare-core's `master`
branch protection rules. These join the existing `build` check.

This is a manual step, not automated, to allow the team to gain confidence in the
new workflow before it becomes blocking.

### Re-runs

When a downstream test fails, the developer can:
1. Click the status check link on the commcare-core PR to navigate to the
   downstream workflow run
2. Use GitHub's "Re-run failed jobs" in the downstream repo
3. On success, the re-run updates the commit status on the commcare-core PR
   automatically (the workflow still has the original inputs)

No special re-run mechanism is needed.

### Known limitations

- **Fork-based PRs** are not supported. `workflow_run.pull_requests` is only
  populated for same-repo branches. This is acceptable for Dimagi's workflow.
- **Scope:** Only PRs to `master` are in scope. The `formplayer` branch is not
  included (elimination of the formplayer branch is a separate effort).

### Out of scope

- Removing the `formplayer` branch or dual-branch workflow (separate effort)
- Changes to how commcare-core's own tests run
- Publishing commcare-core as a Maven/Gradle artifact
