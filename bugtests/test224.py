"""

"""

import support

try:
   import test224s
except SyntaxError:
   pass
else:
   raise support.TestError('Should raise SyntaxError #1')

ret = support.compileJPythonc("test224s.py", output="test224.err",
                              expectError=1)

if ret == 0:
   raise support.TestError('Should raise SyntaxError #2')
 
