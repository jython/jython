# Source Code Structure

This section provides a view of the Jython code base
mostly from the perspective of the kind of object you might be looking for.

## Java Vintage

This is a single (i.e. monolithic) Java project
and so the bulk of the source is in package structure below `src/`,
or for unit tests in Java, below `tests/java`, 
in the root of the project.
Jython pre-dates the introduction of module structure to Java (project Jigsaw).

Note that Java source files may contain multiple classes.
(Some of this code also pre-dates nested classes.)

All file paths here are relative to the project root.
We may use Java package names as the navigation
in place of file paths.
The implied file path is then relative to `src` or `test

## Python Standard Library

We re-use as much as possible of the Python Standard Library (the "stdlib"),
but in many cases we have to modify a library module for Jython.
The modified ones are in `Lib`, as in the CPython project.
There are also a good few modules, especially amongs the unit tests,
that only exist for Jython.

Modules we re-use without modification are in `lib-python/2.7`.
These have been copied from CPython **and are never modified**.
They haven't all been copied from the same (patch) version of CPython.

During the build,
the standard library is copied from `lib-python/2.7` and `Lib` to `dist/Lib`,
with identically-named files in `Lib` (the Jython-specific ones)
taking precedence.



## Source Code Locations

### Built-in types

Built-in types,
meaning here those provided by Python and defined in Java,
are found in `src/org/python/core`.
For a built-in type, for example `float`,
the naming of classes and tests is:

* Principal implementation:
  [`src/org/python/core/PyFloat.java`](../src/org/python/core/PyFloat.java).
* [`src/templates/float.derived`](../src/templates/float.derived) specifies
  `src/org/python/core/PyFloatDerived.java`
  (see ([Generated Derived Classes](generated_derived_classes.md))).
* Tests:
  [`python-lib/2.7/test/test_float.py`](../lib-python/2.7/test/test_float.py)
  and [`Lib/test/test_float_jy.py`](../Lib/test/test_float_jy.py).

However, for historical reasons and to maintain similarity with CPython,
the naming is not always so consistent as `float->PyFloat`,
for example `int -> PyInteger`.


### Built-in functions

Build-in functions are implemented in a single source file (but many classes):
* Implementation: 
  [`src/org/python/core/__builtins__.java`](../src/org/python/core/__builtins__.java).
* Tests:
  [`lib-python/2.7/test/test_builtin.py`](../lib-python/2.7/test/test_builtin.py)
  and [`Lib/test/test_builtin_jy.py`](../Lib/test/test_builtin_jy.py).




### Built-in modules

A Python module defined in Java is typically defined in
[`org.python.modules`](../src/org/python/modules).
For example, for the `math` module:

* Implementation:
  [`/src/org/python/modules/math.java`](../src/org/python/modules/math.java)
* Tests: [`Lib/test/test_math.py`](../Lib/test/test_math.py)
  superseding the `lib-python` tests, 
  and [`Lib/test/test_math_jy.py`](../Lib/test/test_math_jy.py).

A Python module defined in Python is typically defined in `Lib`
or `lib-python/2.7` when we are able to use the Python implementation directly.
There may be a supporting module implemented in Java in `org.python.modules`
that will be referenced from the Python with an underscore.
(The author of the Python module thinks it is in C, but never mind.)

For example, for `codecs` module:

* Implementation:
  [`lib-python/2.7/codecs.py`](../lib-python/2.7/codecs.py).
* Supporting implementation in Java:
  [`org.python.modules._codecs`](../src/org/python/modules/_codecs.java)
* Tests: [`Lib/test/test_codecs.py`](../Lib/test/test_codecs.py)
  superseding the `lib-python` tests, 
  and [`Lib/test/test_codecs_jy.py`](../Lib/test/test_codecs_jy.py).

Many useful modules are implemented in pure Python and
come from the `lib-python` tree.
Where they need compiled support or tweaks to accommodate Jython,
there is quite a lot of variety in the implementation.
The pattern shown for (`codecs`) is normative but in the minority.
A mapping of built-in module names to implementations is found in
[`org.python.modules.Setup`](../src/org/python/modules/Setup.java).

For the documentation of modules, we rely almost entirely on
the widely-available module documentation published from
the CPython project.
Specifics of the Jython implementation of a module are
not (at the time of writing) comparably documented, 
but a sensible choice would be to mirror CPython and use:

* `Doc/library/<module>.rst`

