
Configgy is a library for handling config files and logging for a scala
daemon. The idea is that it should be simple and straightforward, allowing you
to plug it in and get started quickly, writing small useful daemons without
entering the shadowy world of java frameworks.


Changes in version 1.5 include these features:

  - ScribeHandler for sending log entries to a scribe server
  - ThrottledLogger for squelching repetitive messages
  - ConfigMap.toConfigString() will generate a config file as a string
  - Config.fromString() will parse a string into a Config object
  - command-line "-D" options can override/append config items
  - no longer squash all config keys to lowercase

and these bug fixes:

  - possible NPE in Logger.elements [ijuma]
  - various timezone bugs in tests
  - be less aggressive about clearing out log nodes


It can't be a project without a website, so that's here:

  http://www.lag.net/configgy/

The github repository is here:

  http://github.com/robey/configgy/tree/master

And, for fans of maven/ivy, it should now be in the scala-tools maven
repository here:

  http://scala-tools.org/repo-releases/

as "net.lag.configgy".
