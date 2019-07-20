# Jython: Python for the Java Platform
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.python/jython-standalone/badge.svg)](https://search.maven.org/artifact/org.python/jython-standalone/)
[![Javadocs](https://www.javadoc.io/badge/org.python/jython-standalone.svg)](https://www.javadoc.io/doc/org.python/jython-standalone)

This is the development repository of Jython,
the implementation of Python 2.7 in Java.
Along with good (not perfect!) language
and runtime compatibility with CPython 2.7,
Jython 2.7 provides substantial support of the Python ecosystem.
This includes built-in support of *pip/setuptools*
(you can use `bin/pip` if the targets do not include `C` extensions)
and a native launcher for Windows (`bin/jython.exe`)
that works essentially as the `python` command.

Jim Baker presented a talk at PyCon 2015 about Jython 2.7,
including demos of new features: https://www.youtube.com/watch?v=hLm3garVQFo

See [ACKNOWLEDGMENTS](ACKNOWLEDGMENTS) for details about Jython's copyright,
license, contributors, and mailing lists.
Consult [NEWS](NEWS) for detailed release notes, including bugs fixed,
backwards breaking changes, and new features.
We sincerely thank all who contribute to Jython, by bug reports, patches,
pull requests, documentation changes and e-mail discussions.

## How to build Jython

The project uses Mercurial for version-control,
and the master repository is at https://hg.python.org/jython/,
while the repository on GitHub is just a mirror of that.
You may clone either repository to create a buildable copy of the latest state
of the Jython source.

### Build using `ant` for development

Jython is normally built using `ant`.
It is necessary to have Ant and at least a Java 7 SDK on the path.
To build Jython development use, we generally use the command:
```
ant
```
This leaves an executable in `dist/bin`
that you may run from the check-out root with:
```
dist/bin/jython
```
Other `ant` targets exist, notably `clean`, and `jar`.

You can test your build of Jython (by running the regression tests),
with the command:
```
dist/bin/jython -m test.regrtest -e -m regrtest_memo.txt
```

### Build an installer using `ant`

If you want to install a snapshot build of Jython, use the command:
```
ant installer
```
This will leave you with a snapshot installer JAR in `dist`,
that you can run with:
```
java -jar jython-installer.jar
```
for the graphical installer, or:
```
java -jar jython-installer.jar --console
```
For the console version. (A `--help` option gives you the full story.)

### Build a JAR using Gradle

Experimentally, we have a Gradle build that results in a family of JARs,
and a POM.
This is intended to provide the Jython core in a form that Gradle and Maven
users can consume as a dependency.
Invoke this with:
```
PS> .\gradlew publish
```
and a JAR and POM are delivered to ` .build2\repo` 

Whereas the JARs delivered by the installer are somewhat "fat",
embedding certain dependencies in shaded (renamed) form,
the JARs from the Gradle build are "spare"
and cite their dependencies externally through a POM.
The project would like to know if this is being done suitably
for downstream use.
