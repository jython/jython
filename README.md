[![APIdia](https://apidia.net/java/Jython/3/badge.svg)](https://apidia.net/java/Jython/3)

# Jython 3: Python 3 for the Java Platform

This is the development repository of Jython,
the implementation of Python in Java.

You are looking at the branch intended to support version 3.x of Python:
it doesn't build anything useful right now.
Jython 3.x is not yet a viable product you can use in applications.
Head over to [the 2.7 branch](https://github.com/jython/jython/tree/master)
to find the current release.


## Target

Along with good language and runtime compatibility with CPython 3.11,
Jython 3.11 is intended to provide substantial support of the Python ecosystem,
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


## Current focus of work

The current focus is to establish a foundation for the run-time
that makes good use of the dynamic language features of the JVM.
There is a growing set of classes here to test the architectural ideas
underpinning the new foundation.

The code of the Jython 2 implementation is also present on this branch,
waiting to be shifted onto the new foundations (or definitively dropped),
file by file. It does not participate in the build.


## How to build Jython

### Build using `Gradle` for development

Jython may be built using `Gradle`.
```
$ ./gradlew build
```
In its present state, no executable is built, although there is a JAR,
that in principle could be used in sample programs.

Jython is normally built only to run the unit tests (the `core:test` target).
The documentation built by the `core:javadoc` target may also be interesting.
Running the unit tests in `core/src/test/java`,
under a debugger in an IDE,
is perhaps the best way to explore how the code works.

Watch this space for further developments.
