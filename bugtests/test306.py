"""
Test normcase.
"""

import support
import os

if os.sep == '\\': #only do this test on windows.
    p1 = os.path.normpath('e:\\someDir\\packag/modul.py')
    if p1 != 'e:\\someDir\\packag\\modul.py':
        raise support.TestError('Wrong normpath %s' % p1)

