Welcome to Jython 2.5.3!
========================

This is the final release of Jython 2.5.3.

Thanks to Adconion Media Group (http://www.adconion.com/) for sponsoring this
release, and thanks to all who contribute to Jython.

This release fixes numerous bugs since the 2.5.2 release of Jython. Some
highlights include:

* File uploads where broken in Tomcat and Jetty.
* Imports sometimes blew the stack. This was seen in SQLAlchemy for example.
* Some race conditions and threading issues where fixed.
* Several JSR 223 problems have been fixed.

Please see the NEWS file for detailed release notes.  The release was compiled
on Ubuntu with JDK 6 and requires JDK 5 to run.

Please try this out and report any bugs at http://bugs.jython.org.
