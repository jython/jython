"""
Test that the case of the module extension does not matter.
"""

import support

if support.UNIX:
  raise support.TestWarning("this will fail on unix platforms")

import test305m

