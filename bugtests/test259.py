
import support


try:
   import test259s
except SyntaxError:
   pass
else:
   raise support.TestError, "Should raise a syntax error"

