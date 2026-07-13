# Jython Project Processes

This is an incomplete guide for Jython developers and contributors,
to processes we apply during development.

We aspire to processes and principles from
the [Python Developer Guide](https://devguide.python.org/)
where they are generic enough to apply to us.
That guide, in its processes and cheat-sheets,
is oriented strongly towards the C implementation of Python.
Here we aim to provide versions of those things specific to Jython,
our repositories, and a Java implementation[^1].

It will not, to begin with,
contain sections for everything that could be translated to Java.
Where it falls short,
readers should make an intelligent interpretation of the (C)Python Dev Guide
for a Java context,
and consider contributing that here.


## Contents

1. Setup and building (adapt from CPython and Jython Wiki)
1. Git bootcamp (adapt from CPython)
1. [Contributor agreement](contributor_agreement.md)
1. [Coding standard](coding_standard.md)
1. Lifecycle of a change (adapt from CPython)
1. [Regenerating the Jython Windows Launcher](jython_exe.md)
1. [Releasing a version](releasing.md)


[^1]: We have several times tried to maintain a complete Jython Developer Guide
based on the [Python Developer Guide](https://devguide.python.org/),
and incorporating material from it.
Keeping common material in sync proved too difficult.
