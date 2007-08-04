# 
# Test for bug 1758838
#
# execfile(<any file>) should not throw a NullPointerException
#
# The error only shows up in interactive interpretation (type "single" for the compilation).
# But we cannot use InteractiveInterpreter here since it catches all Exceptions,
# therefore we do the compilation 'by hand'.
#

from org.python.core import Py
from org.python.core import PySystemState
from org.python.util import PythonInterpreter

PySystemState.initialize()
interp = PythonInterpreter()
code = Py.compile_command_flags("execfile('test401/to_be_executed.py')", "<input>", "single", None, 1)
interp.exec(code)
