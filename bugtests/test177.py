"""
Check that two equal, but diffrently sorted dict compare equal.
"""

import support

d1 = {'status':1, 'background':2, 'borderwidth':3, 'foreground':4}
d2 = {'status':1, 'background':2, 'foreground':4, 'borderwidth':3}

if d1 != d2:
   raise support.TestError('equal dicts does not compare equal.')

