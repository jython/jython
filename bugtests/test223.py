"""

"""

import support

try:
   import test223s
except SyntaxError:
   pass
else:
   raise support.TestError('Should raise SyntaxError #1')

ret = support.compileJPythonc("test223s.py", output="test223.err",
                              expectError=1)

if ret == 0:
   raise support.TestError('Should raise SyntaxError #2')
 
