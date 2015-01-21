#!/usr/bin/python
# requires an ApiUser (corehq.apps.api.models.ApiUser) on the remote_host with username/password given

import os
import shlex

import subprocess
from subprocess import PIPE
import sys

def submit_build(environ, host):
    target_url= host + "/builds/post/"
    print environ

    command =  (
        'curl -v '
        '-H "Expect:" '
        '-F "artifacts=@{artifacts}" '
        '-F "username={username}" '
        '-F "password={password}" '
        '-F "build_number={build_number}" '
        '-F "version={version}" '
        '{target_url}'
    ).format(target_url=target_url, **environ)

    p = subprocess.Popen(shlex.split(command), stdout=PIPE, stderr=None, shell=False)
    return command, p.stdout.read(), "" #p.stderr.read()


if __name__ == "__main__":

    print "main"
    print os.environ

    variables = [
        "USERNAME",
        "PASSWORD",
        "ARTIFACTS",
        "REMOTE_HOST",
        "VERSION",
        "BUILD_NUMBER",
    ]
    args = sys.argv[1:]
    environ = None
    try:
        environ = dict([(var, os.environ[var]) for var in variables])
		
        print os.environ
		
    except KeyError:
        if len(args) == len(variables):
            environ = dict(zip(variables, args))
            print os.environ

    if environ:
        print os.environ
    	hosts = environ['remote_host'].split("+")
    	for host in hosts:
	        command, out, err = submit_build(environ, host)
	        print command
	        if out.strip():
	            print "--------STDOUT--------"
	            print out
	        if err.strip():
	            print "--------STDERR--------"
	            print err
    else:
        print("submit_build.py <%s>" % ("> <".join(variables)))
