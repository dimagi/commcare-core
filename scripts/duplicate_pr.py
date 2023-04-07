from enum import Enum

import argparse
import sh


class BranchName(Enum):
    MASTER = "master"
    FORMPLAYER = "formplayer"


def get_git():
    return sh.git.bake(_tty_out=False)


def get_target_branch(orig_target_branch:str):
    if orig_target_branch == BranchName.FORMPLAYER.value:
        return BranchName.MASTER.value
    elif orig_target_branch == BranchName.MASTER.value:
        return BranchName.FORMPLAYER.value
    else:
        print("Only following branches are allowed: {}".format(", ".join([key.value for key in BranchName])))
        exit(1)


def git_create_branch(orig_branch_name:str, new_branch_name: str):
    git = get_git()
    try:
        print("orig_branch_name", orig_branch_name)
        if orig_branch_name != BranchName.MASTER.value:
            print("in fetch for create branceh, orig_branchname is", orig_branch_name)
            git_fetch_branch(orig_branch_name)
            print("after fethch in create branch")
        git.checkout(orig_branch_name)
    except sh.ErrorReturnCode_1 as e:
        print(red(e.stderr.decode()))
        exit(1)
    try:
        git.checkout('-b', new_branch_name)
    except sh.ErrorReturnCode_128 as e:
        print(red(e.stderr.decode()))
        exit(1)


def git_fetch_branch(branch_name:str):
    git = get_git()
    # fetch remote branch without switching branches
    input = "{0}:{0}".format(branch_name)
    try:
        print("input", input)
        git.fetch("origin", input)
        print("fetched")
    except sh.ErrorReturnCode_1 as e:
        print(red(e.stderr.decode()))
        exit(1)


def get_new_commits(base_branch: str, curr_branch:str):
    git = get_git()
    print("before merge_base_commit")
    if base_branch != BranchName.MASTER.value:
        print("in fetch for get_new_commits, orig_branchname is", base_branch)
        git_fetch_branch(base_branch)
    base_commit = merge_base_commit(base_branch, curr_branch)
    recent_commit = latest_commit(curr_branch)

    commits_range = "{}..{}".format(base_commit, recent_commit)
    interested_commits = git("rev-list", "--no-merges", commits_range).split()
    return interested_commits


def cherry_pick_new_commits(commits:list[str], branch:str):
    git = get_git()
    git.checkout(branch)
    for commits in reversed(commits):
        try:
            empty_commit_message = "The previous cherry-pick is now empty"
            git("cherry-pick", commits)
        except sh.ErrorReturnCode_1 as e:
            if empty_commit_message in e.stderr.decode():
                print("IN IF")
                git("cherry-pick", "--skip")


def git_push_pr(branch:str):
    git = get_git()
    output = git.push("origin", branch, _err_to_out=True)
    print(output)


def merge_base_commit(branch1: str, branch2:str):
    git = get_git()
    print("in merge_base, before git call.")
    base_commit = git("merge-base", branch1, branch2)
    # base_commit = "793bad7e2b3448da5ed6f6f3900e04568a91e6ea"
    print("base commit is", base_commit)
    return str(base_commit.replace("\n", ""))


def latest_commit(branch:str):
    git = get_git()
    return str(git("rev-parse", branch).replace("\n", ""))


def _wrap_with(code):

    def inner(text, bold=False):
        c = code

        if bold:
            c = "1;%s" % c
        return "\033[%sm%s\033[0m" % (c, text)
    return inner


red = _wrap_with('31')


def main():
    parser = argparse.ArgumentParser(description='Duplicate and push PR between formplayer/master')
    parser.add_argument('orig_source_branch', type=str, help="Branch name of PR to be duplicated")
    parser.add_argument('orig_target_branch', type=str, help="Name of branch the original PR merged into",
                            choices = [key.value for key in BranchName])
    args = parser.parse_args()

    new_source_branch = "copy_of_" + args.orig_source_branch
    new_target_branch = get_target_branch(args.orig_target_branch)

    print("Creating branch {} from {}".format(new_source_branch, new_target_branch))
    git_create_branch(orig_branch_name=new_target_branch, new_branch_name=new_source_branch)

    print("Fetching {}".format(args.orig_source_branch))
    git_fetch_branch(args.orig_source_branch)

    print("Getting new commits from {}".format(args.orig_source_branch))
    new_commits = get_new_commits(args.orig_target_branch, args.orig_source_branch)

    print("Cherry-picking commits from {} to {}".format(args.orig_source_branch, new_source_branch))
    cherry_pick_new_commits(new_commits, new_source_branch)

    print("Pushing {}".format(new_source_branch))
    git_push_pr(new_source_branch)


if __name__ == "__main__":
    main()