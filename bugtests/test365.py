"""
[ 508111 ] jythonc generates invalid statements
"""

import support

support.compileJPythonc("test365c.py", output="test365.err")

#raise support.TestWarning('A test of TestWarning. It is not an error')
