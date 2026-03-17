# Cross-Repo CI

When a commcare-core pull request passes its own tests, the test suites of
[commcare-android](https://github.com/dimagi/commcare-android) and
[formplayer](https://github.com/dimagi/formplayer) are automatically run against
the PR's version of commcare-core. Results appear as status checks on the PR.

## How it works

The `trigger-downstream` job in `.github/workflows/build.yml` runs after the
`build` job succeeds on a pull request. It:

1. Sets **pending** commit statuses on the PR (`cross-repo / commcare-android`
   and `cross-repo / formplayer`)
2. Fires `workflow_dispatch` to each downstream repo, passing the PR's head SHA,
   PR number, and check name
3. Exits immediately (fire-and-forget)

Each downstream repo's CI workflow accepts these inputs via `workflow_dispatch`.
When present, it checks out the specified commcare-core SHA instead of its
default, runs tests, and reports the result back as a commit status on the
commcare-core PR.

## Authentication

A dedicated GitHub App (**CommCare Cross-Repo CI**) provides cross-repo access.
It has two permissions: Actions (write) and Commit statuses (write), and is
installed on `commcare-core`, `commcare-android`, and `formplayer`.

Each repo stores the app credentials as repo-level secrets:
- `CROSS_REPO_CI_APP_ID`
- `CROSS_REPO_CI_PRIVATE_KEY`

## Re-running failed checks

Click the status check link on the commcare-core PR to navigate to the downstream
workflow run, then use GitHub's "Re-run failed jobs". The re-run preserves the
original inputs and updates the status on the commcare-core PR when it completes.

## Downstream workflow files

- commcare-android: `.github/workflows/commcare-android-pr-workflow.yml`
- formplayer: `.github/workflows/build.yml`

Both workflows behave normally for their own PRs and pushes. The cross-repo CI
path only activates when `workflow_dispatch` inputs are provided.
