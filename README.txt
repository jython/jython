Jython: Python for the Java Platform
------------------------------------

Welcome to Jython @jython.version@.
@snapshot.banner@
This is @readme.release@ release of version @jython.version.short@ of Jython.

Along with language and runtime compatibility with CPython 2.7, Jython 2.7
provides substantial support of the Python ecosystem. This includes built-in
support of pip/setuptools (you can use with bin/pip) and a native launcher
for Windows (bin/jython.exe).

Jim Baker presented a talk at PyCon 2015 about Jython 2.7, including demos
of new features: https://www.youtube.com/watch?v=hLm3garVQFo

This release was compiled on @os.name@ using @java.vendor@ Java
version @java.version@ and requires a minimum of Java @jdk.target.version@ to run.

See ACKNOWLEDGMENTS for details about Jython's copyright, license,
contributors, and mailing lists; and NEWS for detailed release notes,
including bugs fixed, backwards breaking changes, and new features.

The developers extend their thanks to all who contributed to this release
of Jython, through bug reports, patches, pull requests, documentation
changes, email and conversation in any media. We are grateful to the PSF for
continuing practical help and support to the project.

Testing
-------
You can test your installation of Jython (not the standalone jar) by
running the regression tests, with the command:

jython -m test.regrtest -e

The regression tests can take about fifty minutes. At the time of writing,
these tests are known to fail (spuriously) on an installed Jython:
    test___all__
    test_java_visibility
    test_jy_internals
    test_ssl_jy
Please report reproducible failures at http://bugs.jython.org .

