"""
Check the return in exec is illegal.
"""

import support

cmd = """print 'hi' 
return 2.3 
print 'fred'"""

try:
   co = compile(cmd, "<string>", "exec") 
except SyntaxError, e:
   pass
else:
   raise support.TestError("Should cause SyntaxError")


