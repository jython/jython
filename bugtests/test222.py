"""

"""

import support

try:
   import test222s
except SyntaxError:
   pass
else:
   raise support.TestError('Should raise SyntaxError #1')

ret = support.compileJPythonc("test222s.py", output="test222.err",
                              expectError=1)

if ret == 0:
   raise support.TestError('Should raise SyntaxError #2')
 
