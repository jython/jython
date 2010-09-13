Welcome to Jython 2.5.2 Beta2
=============================

This is the second beta release of the 2.5.2 version of Jython. Our
current plans is that this will be the last beta of 2.5.2, but this
will depend on bug report.

This release fixes bugs related to resource leaks, Java integration,
and a number of other issues. See the NEWS file for more details. In
particular, we did not completely fix #1327, "Classloaders cannot GC,
which exhausts permgen." Jython uses instances of ThreadLocal-managed
class, ThreadState, to manage its execution state, including frames,
exceptions, and the global namespace. The ThreadState also indirectly
refers to the ClassLoaders used by Jython. Such usage can cause
resource leaks when a Jython application is restarted under certain
app containers, because the ThreadState often may not cleaned up by
the app server's thread pool.

Fixing this problem without a breakwards breaking API change appears
to be difficult. Therefore we recommend exploring workarounds, such as
the one published in this blog post,
http://weblogs.java.net/blog/jjviana/archive/2010/06/09/dealing-glassfish-301-memory-leak-or-threadlocal-thread-pool-bad-ide

Jython 2.6 will introduce limited backwards breaking API changes, so
it will be possible to fully resolve this bug, and related issues, in
that version instead.

And -- last but not least -- please help spread the word:

Organizations using Jython 2.2.1, or earlier, should test their code
against 2.5.2 beta 2 now so that bug fixes and/or workarounds may be
identified. In particular, please note the following:

  * No additional work is anticipated on Jython 2.2.

  * Jython 2.5.2 is the last release in Jython 2.5.x series that will
    address non-severe issues, including Java integration issues.

  * Jython 2.6 development will begin immediately following the 2.5.2
    release. Jython 2.6 will require the use of JDK 6.

The release was compiled on Mac OS X with JDK 5 and requires JDK 5 to
run. Please try it out and report any bugs at http://bugs.jython.org.
