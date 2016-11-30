# commcare-cli

A Clojure powered CLI wrapper for CommCare.
Uses JLine for command history and comprehensive input editing.

## Setup

First build the CommCare CLI jar locally (or download it from jenkins [direct link](https://jenkins.dimagi.com/job/commcare-core/lastSuccessfulBuild/artifact/build/libs/commcare-cli.jar))

    $ gradle cliJar

Then setup a local library repository for the CommCare CLI jar. You might need to run `lein compile` once beforehand to install the `lein-localrepo` plugin.

    $ cd util/clojure-cli
    $ lein localrepo install /path/to/the/commcare-cli.jar org.commcare/commcare 2.31.0


## Usage

    $ lein run --app some.ccz --restore user_restore.xml

or

    $ lein run --app some.ccz --username demo --password abc

## Thanks
The Clojure/JLine integration work is taken from Colin Jones' [REPL-y project](https://github.com/trptcolin/reply)
