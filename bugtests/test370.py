"""
[ 562943 ] os.path.getmtime misbehaves on nonfile
"""

import support

import os.path
try:
    print os.path.getmtime('nonfile') 
except OSError:
    pass
else:
    raise support.TestError('Should raise an OSError')
