"""
Test that the case of the module extension does not matter.
As the registry documentation says, this will only work if options.caseok is true.
"""

import support

if support.UNIX:
  raise support.TestWarning("this will fail on unix platforms")

from org.python.core import Options
switchedCase = 0
if not Options.caseok:
  switchedCase = 1
  Options.caseok = 1
try:
  import test305m  # the file is named test305m.PY
finally:
  if switchedCase:
    Options.caseok = 0

