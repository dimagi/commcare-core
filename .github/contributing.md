### How to Create PRs on CommCare Core

[Formplayer](https://github.com/dimagi/formplayer) and [CommCare Android](https://github.com/dimagi/commcare-android) both utilizes commcare-core as the underlining XForm engine.  To minimize the disruptions from changes on one platform to another, we maintain two different branches `formplayer` and `master` for Formplayer and CommCare Android respectively.
We try to keep both these branches in sync with each other the best we can. To do that it's cruicial that you PR your changes against both these branches. Please find more information below on how to create these PRs depending on what platform you work with - 


##### Duplicating A Formplayer change to CommCare Android

1. If you are working on a Formplayer change, you will want to start by checking out `your_feature_branch` from `formplayer` as the base branch. Make changes on `your_feature_branch` and create your original PR against `formplayer` branch.

2. Now you will need to duplicate this PR by making another PR against `master`. Make sure the branch for this PR is is merged and is not deleted. Then create the comment `duplicate this PR <starting-commit-id> <ending-commit-id>`. The `ending-commit-id` should be the last non-merge commit in the PR. This should result in a Github Actions workflow duplicating your PR against `master`. Go to the duplicate PR, close and re-open it to run the Github checks against it.

3. In order for us to test that your PR against `master` doesn't break anything on CommCare Android, you need to run android side tests with your PR.
To do this - 
    - Go to the [Android test PR](https://github.com/dimagi/commcare-android/pull/2612)
    - Change "cross-request" PR in description to the duplicate PR created in step 2 above
    - Make a comment saying `@damagatchi retest this please` ([example](https://github.com/dimagi/commcare-android/pull/2612#issuecomment-1453132625))

4. Request a review from `@shubham1g5`on your duplicate commcare-core PR. Android devs will now be responsible to make any further Android compatibility changes for this PR if required and merge it.  


##### Duplicating A CommCare Android change to Formplayer

1. If you are working on a CommCare Android change, you will want to start by checking out `your_feature_branch` from `master` as the base branch. Make changes on `your_feature_branch` and create your original PR against `master` branch.

2. Now you will need to duplicate this PR by making another PR against `formplayer`. Make sure the branch for this PR is is merged and is not deleted. Then create the comment `duplicate this PR <starting-commit-id> <ending-commit-id>`. The `ending-commit-id` should be the last non-merge commit in the PR. This should result in a Github Actions workflow duplicating your PR against `formplayer`. Go to the duplicate PR, close and re-open it to run the Github checks against it.

3. In order for us to test that your PR against `formplayer` doesn't break anything on Formplayer, we need to run formplayer side tests with your PR.
To do this - 
    - Check out a new branch say `test_cc_1189` from `master` in [Formplayer](https://github.com/dimagi/formplayer)
    - Point the submodule in `libs/commcare` to your CommCare Core `your_feature_branch_dupe` branch and push your formplayer branch - 
````
cd libs/commcare
git fetch; git checkout your_feature_branch_dupe
cd ../..
git add libs/commcare
git commit -m "update submodule to 1189 head"
git push origin test_cc_1189
````
    - Create a dummy test PR in Formplayer with a subject `[Test] CC 1189`. 
    - You don't need to add any reviewers here as this PR will be closed without merging and is only used to run Formplayer tests

4. Go back to your CommCare Core `formplayer` PR and add `cross-request: link_to_formplayer_test_pr` as the last line in the PR description. This PR will now be owned by the Formplayer devs to decide whether any additional changes are required on Formplayer before merging this PR.
