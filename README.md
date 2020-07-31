# Jython 3: Python 3 for the Java Platform

This is the development repository of Jython,
the implementation of Python in Java.
You are looking at the branch intended to support version 3.8 of Python.

Jython 3.x is not yet a viable product you can use in applications.

Along with good (not perfect!) language
and runtime compatibility with CPython 3.8,
Jython 3.8 is intended to provide substantial support of the Python ecosystem.
This includes built-in support of *pip/setuptools*
(you can use `bin/pip` if the targets do not include `C` extensions)
and a native launcher that works essentially as the `python` command.

See [ACKNOWLEDGMENTS](ACKNOWLEDGMENTS) for details about Jython's copyright,
license, contributors, and mailing lists.
Consult [NEWS](NEWS) for detailed release notes, including bugs fixed,
backwards breaking changes, and new features.
We sincerely thank all who contribute to Jython, by bug reports, patches,
pull requests, documentation changes and e-mail discussions.

## How to build Jython

The project uses Git for version-control,
and the master repository is at https://github.com/jython/jython.
You should clone this repository to create a buildable copy of the latest state
of the Jython source.
The previously authoritative repository at https://hg.python.org/jython is not now in use,
remaining frozen at v2.7.2.

### Build using `Gradle` for development

Jython is normally built using `Gradle`.
It is necessary to have Ant and at least a Java 8 SDK on the path.
To build Jython in development, we generally use this command:
```
$ ./gradlew build
```
In its present state, the build doesn't produce anything useful.
Watch this space for further developments.

