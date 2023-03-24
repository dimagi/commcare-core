import os
import re

import sh

def get_git(path=None):
    return sh.git.bake(_tty_out=False, _cwd=path)


def get_grep():
    return sh.grep.bake(_tty_out=False)

def git_current_branch(git=None):
    git = git or get_git()
    grep = get_grep()
    branch = grep(git.branch(), '^* ').strip()[2:]
    if branch.startswith('('):
        branch = git.log('--pretty=oneline', n=1).strip().split(' ')[0]
    return branch

def main():
    git = get_git(path = '/Users/jonathantang/commcare-core/scripts')
    test = git_current_branch(git)

if __name__ == "__main__":
    main()
    print("Test2")
    print("test3")