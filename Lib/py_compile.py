"""\
Routine to compile a .py file to a .class file.

This is a JPython replacement of a file from the CPython standard lib
It uses intimate knowledge of how JPython's internals work to do the
job (just like the CPython version does) and many of the interfaces
it uses might be changed in future releases"""


from org.python.core import imp
from java.io import File

def compile(file):
	import os
	
	name = os.path.splitext(os.path.split(file)[-1])[0]

	imp.compile_source(name, File(file))
	
