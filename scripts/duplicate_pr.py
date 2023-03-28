from enum import Enum

import argparse
import sh


class BranchName(Enum):
    MASTER = "master"
    FORMPLAYER = "formplayer"


def get_git(path=None):
    return sh.git.bake(_tty_out=False, _cwd=path)


def main():
    parser = argparse.ArgumentParser(description='Duplicate and push PR between formplayer/master')
    parser.add_argument('orig_pr_id', type=str, help="ID of PR to be cloned")
    parser.add_argument('orig_source_branch', type=str, help="Branch name of PR to be duplicated")
    parser.add_argument('orig_target_branch', type=str, help="Name of branch the original PR merged into",
                            choices = [key.value for key in BranchName])
    args = parser.parse_args()

if __name__ == "__main__":
    main()