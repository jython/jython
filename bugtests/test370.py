"""
Basic test, just raises an TestError
"""

import support

import os.path
try:
    print os.path.getmtime('nonfile') 
except OSError:
    pass
else:
    raise support.TestError('Should raise an OSError')
