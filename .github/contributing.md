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
