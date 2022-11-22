### How to Create PRs on CommCare Core

[Formplayer](https://github.com/dimagi/formplayer) and [CommCare Android](https://github.com/dimagi/commcare-android) both utilizes commcare-core as the underlining XForm engine.  To minimize the disruptions from changes on one platform to another, we maintain two different branches `formplayer` and `master` for Formplayer and CommCare Android respectively.
We try to keep both these branches in sync with each other the best we can. To do that it's cruicial that you PR your changes against both these branches. Please find more information below on how to create these PRs depending on what platform you work with - 


##### Duplicating A Formplayer change to CommCare Android

1. If you are working on a Formplayer change, you will want to start by checking out `your_feature_branch` from `formplayer` as the base branch. Make changes on `your_feature_branch` and create your original PR against `formplayer` branch.

2. Now you will need to duplicate this PR by making another PR against `master`. To do this, firstly you would want to compare `your_feature_branch` to the `master` and see if the diff only shows the commit that belongs to your branch. If yes, you can directly create another PR from `your_feature_branch` to `master`. 
Otherwise you will need to create another branch by checking out `your_feature_branch_dupe` from `master` as the base branch. You will then need to [cherry-pick](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-commits/cherry-picking-a-commit#about-git-cherry-pick) each of the commits from `your_feature_branch` to `your_feature_branch_dupe`. 
Subsequently you should create a PR from `your_feature_branch_dupe` against `master`. While creating this PR you can remove all the PR template field and simply mention your original `formplayer` PR as a reference. 

3. In order for us to test that your PR against `master` doesn't break anything on CommCare Android, we need to run android side tests with your PR.
To do this - 
    - Check out a new branch say `test_cc_1189` from `master` in [CommCare Android](https://github.com/dimagi/commcare-android)
    - Do an empty commit on your new branch as `git commit --allow-empty -m "Empty-Commit"` and push your branch to github. 
    - Create a dummy test PR with a subject `[Test] CC 1189`. 
    - You can remove all the fields from the PR template here and add a line at the very bottom saying `cross-request: link_to_commcare-core_master_pr`. Without this line the tests won't factor in your CommCare Core `master` PR to run tests with. 
    - Label the PR with `cross requested tag`
    - Label the PR with an appropriate `product/*` tag.
    - You don't need to add any reviewers here as this PR will be closed without merging and is only used to run Android tests

   See https://github.com/dimagi/commcare-android/pull/2609 for an example of such a test PR. 

4. Go back to your CommCare Core `master` PR and add `cross-request: link_to_commcare-android_test_pr` as the last line in the PR description. This PR will now be owned by the Android devs to decide whether any additional changes are required on CommCare Android before merging this PR.


##### Duplicating A CommCare Android change to Formplayer

1. If you are working on a CommCare Android change, you will want to start by checking out `your_feature_branch` from `master` as the base branch. Make changes on `your_feature_branch` and create your original PR against `master` branch.

2. Now you will need to duplicate this PR by making another PR against `formplayer`. To do this, firstly you would want to compare `your_feature_branch` to the `formplayer` and see if the diff only shows the commit that belongs to your branch. If yes, you can directly create another PR from `your_feature_branch` to `formplayer`. 
Otherwise you will need to create another branch by checking out `your_feature_branch_dupe` from `formplayer` as the base branch. You will then need to [cherry-pick](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-commits/cherry-picking-a-commit#about-git-cherry-pick) each of the commits from `your_feature_branch` to `your_feature_branch_dupe`. 
Subsequently you should create a PR from `your_feature_branch_dupe` against `formplayer`. While creating this PR you can remove all the PR template field and simply mention your original `master` PR as a reference. 

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
