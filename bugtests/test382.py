"""
catching frame wasn't captured in a traceback
"""

import sys

def check(tb,expt_lines):
  assert tb.tb_frame is sys._getframe(1),"catching frame should be included"
  lines=[]
  while tb:
    lines.append(tb.tb_lineno)
    tb = tb.tb_next
  assert expt_lines==lines, "bogus line numbers: %s vs. expected %s" % (lines,expt_lines)

def f():
  try:
   raise KeyError # 17
  except:
    raise

try:
  f() # 22
except:
  t,e,tb = sys.exc_info()
  check(tb,[22,17])

try:
  f() # 28
except KeyError,e:
  t,e,tb = sys.exc_info()
  check(tb,[28,17])

try:
 1/0 # 34
except:
 t,e,tb = sys.exc_info()
 check(tb,[34])

try:
 try:
  1/0 # 41
 except:
  raise
except:
 t,e,tb = sys.exc_info()
 check(tb,[41])
