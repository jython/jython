"""
  The traceback linenumber does not match the throw line.
"""
import support

def foo():
   assert 0

try:
   try:


      foo()



   finally:
      pass
except:
    import sys
    info = sys.exc_info()
    support.compare(info[2].tb_lineno, "13")
