The WEB-INF/lib-python directory, if it exists, is automatically added 
to sys.path. Adding jython modules into this directory will make them 
available for import into your application.

If you add your modules in a subdirectory, then be sure that that 
subdirectory contains an __init__.py file, so that the subdirectory 
is considered to be a package.

See here for more details.
http://www.rexx.com/~dkuhlman/python_101/python_101.html#SECTION004540000000000000000
http://www.python.org/doc/essays/packages.html
