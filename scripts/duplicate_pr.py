from enum import Enum

import argparse
import sh


class BranchName(Enum):
    MASTER = "master"
    FORMPLAYER = "formplayer"


def get_git(path=None):
    return sh.git.bake(_tty_out=False, _cwd=path)


def get_target_branch(orig_target_branch:str):
    if orig_target_branch == BranchName.FORMPLAYER.value:
        return BranchName.MASTER.value
    elif orig_target_branch == BranchName.MASTER.value:
        return BranchName.FORMPLAYER.value
    else:
        print("Only following branches are allowed: {}".format(", ".join([key.value for key in BranchName])))
        exit(1)


def git_create_branch(orig_branch_name:str, new_branch_name: str, git=None):
    git = git or get_git()
    try:
        git.checkout(orig_branch_name)
    except sh.ErrorReturnCode_1 as e:
        print(red(e.stderr.decode()))
        exit(1)
    try:
        git.checkout('-b', new_branch_name)
    except sh.ErrorReturnCode_128 as e:
        print(red(e.stderr.decode()))
        exit(1)


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
    parser.add_argument('orig_pr_id', type=str, help="ID of PR to be cloned")
    parser.add_argument('orig_source_branch', type=str, help="Branch name of PR to be duplicated")
    parser.add_argument('orig_target_branch', type=str, help="Name of branch the original PR merged into",
                            choices = [key.value for key in BranchName])
    args = parser.parse_args()

    new_source_branch = "copy_of_" + args.orig_source_branch
    new_target_branch = get_target_branch(args.orig_target_branch)

    print("Creating branch {} from {}".format(new_source_branch, new_target_branch))
    git_create_branch(orig_branch_name=new_target_branch, new_branch_name=new_source_branch)


if __name__ == "__main__":
    main()