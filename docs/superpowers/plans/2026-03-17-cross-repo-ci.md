# Cross-Repo CI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically trigger commcare-android and formplayer test suites when a commcare-core PR passes its own tests, reporting results back as commit statuses.

**Architecture:** Fire-and-forget with callback. A new commcare-core workflow dispatches downstream test runs via `workflow_dispatch`, then exits. Downstream workflows report success/failure back as commit statuses on the commcare-core PR. A dedicated GitHub App provides cross-repo authentication.

**Tech Stack:** GitHub Actions, GitHub Apps, GitHub commit status API

**Spec:** `docs/superpowers/specs/2026-03-17-cross-repo-ci-design.md`

---

## Chunk 1: GitHub App Setup and commcare-core Trigger Workflow

### Task 1: Create the GitHub App (manual)

This task cannot be automated — it requires GitHub UI interaction.

**Steps:**

- [ ] **Step 1: Create the GitHub App**

  Go to https://github.com/organizations/dimagi/settings/apps/new and create an app with:
  - **Name:** `CommCare Cross-Repo CI` (or similar unique name)
  - **Homepage URL:** `https://github.com/dimagi/commcare-core`
  - **Webhook:** Uncheck "Active" (no webhook needed)
  - **Permissions:**
    - Repository permissions > Actions: Read and write
    - Repository permissions > Commit statuses: Read and write
  - **Where can this GitHub App be installed?** Only on this account

- [ ] **Step 2: Generate a private key**

  On the app's settings page, scroll to "Private keys" and click "Generate a private key". Save the downloaded `.pem` file.

- [ ] **Step 3: Note the App ID**

  On the app's settings page, note the "App ID" (a numeric value).

- [ ] **Step 4: Install the app on all three repos**

  Go to the app's "Install" tab and install it on the `dimagi` organization, granting access to:
  - `commcare-core`
  - `commcare-android`
  - `formplayer`

- [ ] **Step 5: Add secrets to commcare-core**

  In `dimagi/commcare-core` > Settings > Secrets and variables > Actions, add:
  - `CROSS_REPO_CI_APP_ID` — the App ID from step 3
  - `CROSS_REPO_CI_PRIVATE_KEY` — the full contents of the `.pem` file from step 2

- [ ] **Step 6: Add secrets to commcare-android**

  Same as step 5, in `dimagi/commcare-android` repo settings.

- [ ] **Step 7: Add secrets to formplayer**

  Same as step 5, in `dimagi/formplayer` repo settings.

---

### Task 2: Create `trigger-downstream.yml` in commcare-core

**Files:**
- Create: `.github/workflows/trigger-downstream.yml`

- [ ] **Step 1: Create the workflow file**

```yaml
name: Trigger Downstream Tests

on:
  workflow_run:
    workflows: ["CommCare Core Build"]
    types: [completed]

jobs:
  trigger-downstream:
    # Only run for successful PR builds, not pushes
    if: >
      github.event.workflow_run.conclusion == 'success' &&
      github.event.workflow_run.event == 'pull_request' &&
      github.event.workflow_run.pull_requests[0]
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        include:
          - repo: dimagi/commcare-android
            check_name: "cross-repo / commcare-android"
            workflow_file: commcare-android-pr-workflow.yml
          - repo: dimagi/formplayer
            check_name: "cross-repo / formplayer"
            workflow_file: build.yml
    steps:
      - name: Generate GitHub App token
        id: app-token
        uses: actions/create-github-app-token@v1
        with:
          app-id: ${{ secrets.CROSS_REPO_CI_APP_ID }}
          private-key: ${{ secrets.CROSS_REPO_CI_PRIVATE_KEY }}
          owner: dimagi
          repositories: commcare-core,commcare-android,formplayer

      - name: Get PR info
        id: pr
        run: |
          echo "sha=${{ github.event.workflow_run.head_sha }}" >> "$GITHUB_OUTPUT"
          echo "pr_number=${{ github.event.workflow_run.pull_requests[0].number }}" >> "$GITHUB_OUTPUT"

      - name: Set pending status
        uses: actions/github-script@v7
        with:
          github-token: ${{ steps.app-token.outputs.token }}
          script: |
            await github.rest.repos.createCommitStatus({
              owner: 'dimagi',
              repo: 'commcare-core',
              sha: '${{ steps.pr.outputs.sha }}',
              state: 'pending',
              context: '${{ matrix.check_name }}',
              description: 'Waiting for downstream tests...'
            });

      - name: Trigger downstream workflow
        id: dispatch
        uses: actions/github-script@v7
        with:
          github-token: ${{ steps.app-token.outputs.token }}
          script: |
            try {
              const [owner, repo] = '${{ matrix.repo }}'.split('/');
              await github.rest.actions.createWorkflowDispatch({
                owner,
                repo,
                workflow_id: '${{ matrix.workflow_file }}',
                ref: 'master',
                inputs: {
                  commcare_core_sha: '${{ steps.pr.outputs.sha }}',
                  commcare_core_pr: '${{ steps.pr.outputs.pr_number }}',
                  commcare_core_check_name: '${{ matrix.check_name }}'
                }
              });
            } catch (error) {
              // If dispatch fails, set error status so it doesn't stay pending forever
              await github.rest.repos.createCommitStatus({
                owner: 'dimagi',
                repo: 'commcare-core',
                sha: '${{ steps.pr.outputs.sha }}',
                state: 'error',
                context: '${{ matrix.check_name }}',
                description: `Failed to trigger: ${error.message}`.substring(0, 140)
              });
              throw error;
            }
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/trigger-downstream.yml
git commit -m "Add workflow to trigger downstream repo tests on PR success"
```

---

## Chunk 2: Downstream Workflow Changes

### Task 3: Modify commcare-android PR workflow

**Files:**
- Modify: `commcare-android/.github/workflows/commcare-android-pr-workflow.yml`

This workflow currently triggers on `pull_request` only. We need to:
1. Add `workflow_dispatch` with our inputs
2. Conditionally override the commcare-core checkout when inputs are provided
3. Report status back to commcare-core on completion

- [ ] **Step 1: Add `workflow_dispatch` trigger and inputs, fix concurrency**

Change the `on:` block from:

```yaml
on:
  pull_request:
```

To:

```yaml
on:
  pull_request:
  workflow_dispatch:
    inputs:
      commcare_core_sha:
        description: 'commcare-core SHA to test against'
        required: false
      commcare_core_pr:
        description: 'commcare-core PR number for status reporting'
        required: false
      commcare_core_check_name:
        description: 'Status check name to report back to commcare-core'
        required: false
```

Also update the concurrency group to include the SHA for cross-repo runs,
so concurrent dispatches for different commcare-core PRs don't cancel each other.
The existing concurrency block uses `github.ref`, which on `workflow_dispatch`
is always `refs/heads/master`:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ inputs.commcare_core_sha || github.ref }}
  cancel-in-progress: true
```

- [ ] **Step 2: Guard cross-request script and add commcare-core checkout**

The existing cross-request script uses `${{ github.event.number }}` which is empty
on `workflow_dispatch`. Guard it and add an alternative checkout path:

Add `if` condition to the existing cross-request steps (download, install deps,
run script):

```yaml
      - name: Download cross request script
        if: ${{ github.event_name == 'pull_request' }}
        run: |
          curl https://raw.githubusercontent.com/dimagi/mobile-deploy/master/requirements.txt -o requirements.txt
          curl https://raw.githubusercontent.com/dimagi/mobile-deploy/master/checkout_cross_request_repo.py -o checkout_cross_request_repo.py
      - name: Install Python dependencies
        if: ${{ github.event_name == 'pull_request' }}
        run: python -m pip install -r requirements.txt
      - name: Run cross request script
        if: ${{ github.event_name == 'pull_request' }}
        run: python checkout_cross_request_repo.py commcare-android ${{ github.event.number }} commcare-core
```

Then add a step for the cross-repo CI path that clones commcare-core at the
specified SHA. This runs instead of the cross-request script on `workflow_dispatch`:

```yaml
      - name: Checkout commcare-core for cross-repo CI
        if: ${{ inputs.commcare_core_sha }}
        uses: actions/checkout@v6
        with:
          repository: dimagi/commcare-core
          ref: ${{ inputs.commcare_core_sha }}
          path: commcare-core
```

Using `actions/checkout` with `ref` is the reliable way to check out a specific
SHA — it handles the git fetch/checkout correctly and avoids issues with bare SHA
fetches.

- [ ] **Step 3: Add GitHub App token generation and status reporting**

Add the token generation step right after the checkout steps (before anything that
might fail), so it's available for the reporting step even if later steps fail:

```yaml
      - name: Generate GitHub App token
        if: ${{ always() && inputs.commcare_core_check_name }}
        id: app-token
        uses: actions/create-github-app-token@v1
        with:
          app-id: ${{ secrets.CROSS_REPO_CI_APP_ID }}
          private-key: ${{ secrets.CROSS_REPO_CI_PRIVATE_KEY }}
          owner: dimagi
          repositories: commcare-core
```

Add the status reporting step as the **final step** of the `build-test-assemble`
job (after all existing steps):

```yaml
      - name: Report status to commcare-core
        if: ${{ always() && inputs.commcare_core_check_name }}
        uses: actions/github-script@v7
        with:
          github-token: ${{ steps.app-token.outputs.token }}
          script: |
            const state = '${{ job.status }}' === 'success' ? 'success' : 'failure';
            const description = state === 'success' ? 'Tests passed' : 'Tests failed';
            await github.rest.repos.createCommitStatus({
              owner: 'dimagi',
              repo: 'commcare-core',
              sha: '${{ inputs.commcare_core_sha }}',
              state,
              context: '${{ inputs.commcare_core_check_name }}',
              description,
              target_url: `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
            });
```

- [ ] **Step 4: Handle the `browserstack-tests` job**

The existing workflow has a `browserstack-tests` job (a separate job with its own
steps, not a reusable workflow call) that depends on `build-test-assemble`.
For cross-repo CI runs, we don't want to run browserstack tests (they're slow and
not relevant to commcare-core compatibility). Add a condition to skip it:

```yaml
  browserstack-tests:
    if: ${{ github.event_name == 'pull_request' }}
    needs: build-test-assemble
```

This ensures browserstack tests only run for commcare-android PRs, not cross-repo
dispatches. Note: `github.event.number` (used for `PR_NUMBER` in that job) is also
empty on `workflow_dispatch`, so this guard is necessary to prevent failures.

- [ ] **Step 5: Commit**

```bash
cd /path/to/commcare-android
git checkout -b dmr/cross-repo-ci
git add .github/workflows/commcare-android-pr-workflow.yml
git commit -m "Add cross-repo CI support for commcare-core PRs"
```

---

### Task 4: Modify formplayer build workflow

**Files:**
- Modify: `formplayer/.github/workflows/build.yml`

This workflow currently triggers on push/PR to `master`. It checks out the repo
with `submodules: recursive`, which pulls commcare-core from the `formplayer` branch
of the submodule. We need to:
1. Add `workflow_dispatch` with our inputs
2. Override the submodule checkout when inputs are provided
3. Report status back to commcare-core on completion

- [ ] **Step 1: Add `workflow_dispatch` trigger and inputs**

Change the `on:` block from:

```yaml
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]
```

To:

```yaml
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]
  workflow_dispatch:
    inputs:
      commcare_core_sha:
        description: 'commcare-core SHA to test against'
        required: false
      commcare_core_pr:
        description: 'commcare-core PR number for status reporting'
        required: false
      commcare_core_check_name:
        description: 'Status check name to report back to commcare-core'
        required: false
```

- [ ] **Step 2: Add conditional commcare-core override after checkout**

After the checkout step (line 45-47), add a step to override the submodule.
The submodule normally tracks the `formplayer` branch, but for cross-repo CI
we want to test against the PR's `master`-targeted changes. Use
`actions/checkout` to reliably check out the specific SHA:

```yaml
      - name: Override commcare-core for cross-repo CI
        if: ${{ inputs.commcare_core_sha }}
        uses: actions/checkout@v3
        with:
          repository: dimagi/commcare-core
          ref: ${{ inputs.commcare_core_sha }}
          path: libs/commcare
```

Note: This uses `@v3` to match the existing checkout action version in this
workflow. The checkout overwrites the submodule directory with the specified SHA.

- [ ] **Step 3: Add GitHub App token generation and status reporting**

Add token generation right after the checkout/override steps (using `if: always()`
so it runs even if the override fails):

```yaml
      - name: Generate GitHub App token
        if: ${{ always() && inputs.commcare_core_check_name }}
        id: app-token
        uses: actions/create-github-app-token@v1
        with:
          app-id: ${{ secrets.CROSS_REPO_CI_APP_ID }}
          private-key: ${{ secrets.CROSS_REPO_CI_PRIVATE_KEY }}
          owner: dimagi
          repositories: commcare-core
```

Add as the **final step** of the `build` job:

```yaml
      - name: Report status to commcare-core
        if: ${{ always() && inputs.commcare_core_check_name }}
        uses: actions/github-script@v7
        with:
          github-token: ${{ steps.app-token.outputs.token }}
          script: |
            const state = '${{ job.status }}' === 'success' ? 'success' : 'failure';
            const description = state === 'success' ? 'Tests passed' : 'Tests failed';
            await github.rest.repos.createCommitStatus({
              owner: 'dimagi',
              repo: 'commcare-core',
              sha: '${{ inputs.commcare_core_sha }}',
              state,
              context: '${{ inputs.commcare_core_check_name }}',
              description,
              target_url: `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
            });
```

- [ ] **Step 5: Commit**

```bash
cd /path/to/formplayer
git checkout -b dmr/cross-repo-ci
git add .github/workflows/build.yml
git commit -m "Add cross-repo CI support for commcare-core PRs"
```

---

## Chunk 3: Testing and Rollout

Testing is split into two stages. Stage 1 tests the downstream callback mechanism
in isolation (before merging anything). Stage 2 tests the full trigger chain
(after merging downstream PRs).

### Task 5: Stage 1 — Test downstream callbacks in isolation

Prerequisites: GitHub App is created, installed, and secrets are configured in
all three repos (Task 1).

- [ ] **Step 1: Push downstream branches**

  Push the `dmr/cross-repo-ci` branches to commcare-android and formplayer:

  ```bash
  cd /path/to/commcare-android
  git push -u origin dmr/cross-repo-ci

  cd /path/to/formplayer
  git push -u origin dmr/cross-repo-ci
  ```

- [ ] **Step 2: Create a throwaway commcare-core PR**

  Create a PR on commcare-core with a no-op change (e.g., add a comment to a file).
  This gives you a real PR number and head SHA to use as test inputs. Note the
  PR number and head SHA.

- [ ] **Step 3: Manually dispatch commcare-android workflow**

  Trigger the workflow on the feature branch (not master) to test without merging:

  ```bash
  gh workflow run commcare-android-pr-workflow.yml \
    --repo dimagi/commcare-android \
    --ref dmr/cross-repo-ci \
    -f commcare_core_sha=<PR head SHA> \
    -f commcare_core_pr=<PR number> \
    -f commcare_core_check_name="cross-repo / commcare-android"
  ```

- [ ] **Step 4: Verify commcare-android callback**

  1. Watch the workflow run in dimagi/commcare-android Actions tab
  2. Verify it checks out the correct commcare-core SHA
  3. Go to the commcare-core PR and confirm a status check appears:
     - Name: `cross-repo / commcare-android`
     - State: `success` or `failure`
     - Link: points back to the commcare-android workflow run

- [ ] **Step 5: Manually dispatch formplayer workflow**

  ```bash
  gh workflow run build.yml \
    --repo dimagi/formplayer \
    --ref dmr/cross-repo-ci \
    -f commcare_core_sha=<PR head SHA> \
    -f commcare_core_pr=<PR number> \
    -f commcare_core_check_name="cross-repo / formplayer"
  ```

- [ ] **Step 6: Verify formplayer callback**

  Same verification as Step 4, but for formplayer.

- [ ] **Step 7: Test re-run behavior**

  If either downstream workflow failed, use GitHub's "Re-run failed jobs" and
  verify the status on the commcare-core PR updates accordingly.

### Task 6: Stage 2 — Full end-to-end validation

- [ ] **Step 1: Merge downstream PRs**

  Create PRs for the `dmr/cross-repo-ci` branches in commcare-android and
  formplayer, and merge them to `master`. The downstream changes are additive
  (existing triggers are unchanged), so this is low risk — and Stage 1 has
  already verified the new functionality works.

- [ ] **Step 2: Push commcare-core branch and create PR**

  Push the `dmr/cross-repo-ci` branch on commcare-core (which includes
  `trigger-downstream.yml`) and create a PR.

- [ ] **Step 3: Verify full trigger chain**

  The PR from Step 2 (or a new test PR) should trigger the full flow:
  1. `build.yml` runs and passes
  2. `trigger-downstream.yml` fires after build completes
  3. Pending statuses appear on the PR (`cross-repo / commcare-android`,
     `cross-repo / formplayer`)
  4. Downstream workflows are triggered on `master`
  5. On completion, statuses update to success/failure on the commcare-core PR
  6. Status links navigate to the downstream workflow runs

- [ ] **Step 4: Test failure reporting**

  Create a commcare-core PR with a deliberately breaking change (e.g., rename a
  public method used by downstream) to verify that failures are correctly reported
  back as `failure` statuses.

### Task 7: Branch protection (deferred)

After ~1 week of stabilization:

- [ ] **Step 1: Add required status checks**

  In commcare-core > Settings > Branches > `master` protection rules, add:
  - `cross-repo / commcare-android`
  - `cross-repo / formplayer`

  as required status checks.
