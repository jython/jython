"""
[ 545235 ] unexpected match with re
"""

import support


import re
rt = re.compile(r'c[^a]*t', re.IGNORECASE)
if rt.match("cat") is not None:
    raise support.TestError('Should not match #1')
rs = re.compile(r'c[^a]t', re.IGNORECASE)
if rs.match('cat') is not None:
    raise support.TestError('Should not match #2')
