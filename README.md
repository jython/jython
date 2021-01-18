# Jython 3: Python 3 for the Java Platform

This is the development repository of Jython,
the implementation of Python in Java.

You are looking at the branch intended to support version 3.8 of Python:
it doesn't build anything useful right now.
Jython 3.x is not yet a viable product you can use in applications.
Head over to [the 2.7 branch](https://github.com/jython/jython/tree/master)
to find the current release.


## Target

Along with good language and runtime compatibility with CPython 3.8,
Jython 3.8 is intended to provide substantial support of the Python ecosystem,
and solid Java integration.

For more about the target see the
[Jython 3 MVP](https://www.jython.org/jython-3-mvp)
page.

See [ACKNOWLEDGMENTS](ACKNOWLEDGMENTS) for details about Jython's copyright,
license, contributors, and mailing lists.
Consult [NEWS](NEWS) for detailed release notes, including bugs fixed,
backwards breaking changes, and new features.
We are sincerely grateful to all who contribute to Jython, by bug reports, patches,
pull requests, documentation changes and e-mail discussions.


## How to build Jython

### Build using `Gradle` for development

Jython is normally built using `Gradle`.
```
$ ./gradlew build
```
In its present state, no executable is built.
Watch this space for further developments.
