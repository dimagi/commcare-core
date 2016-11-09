# commcare-cli

A Clojure powered CLI wrapper for CommCare. 
Uses JLine for command history and comprehensive input editing.

## Setup

You must setup a local library repository for the (CommCare CLI jar)[https://jenkins.dimagi.com/job/commcare-core/]. You might need to run `lein compile` once beforehand to install the `lein-localrepo` plugin.

    $ lein localrepo install /path/to/the/commcare-cli.jar org.commcare/commcare 2.30.0


## Usage

    $ lein run --app some.ccz --restore user_restore.xml

## Thanks
The Clojure/JLine integration work is taken from Colin Jones' (REPL-y project)[https://github.com/trptcolin/reply]
