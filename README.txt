Welcome to Jython 2.1
=======================

Important: The format of both compiled ($py.class) and frozen module
has changed from version 2.0. It is necesary to delete existing
$py.class files and recompile frozen applications when upgrading
to Jython-2.1.

Other backward incompatible changes include:

- The case of module names are now important, even on case ignoring
  filesystems like windows. This matches CPython behaviour.

- The way .zip and .jar files is added to sys.path is changed from 2.1a3.
  Use the form: sys.path.append("/path/to/file.zip/Lib") to search for 
  modules with the zipped named of "Lib/module.py"

