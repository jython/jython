"""

"""

import support

if 'abc' is 'a'+'b'+'c':
    raise support.TestError("Test1")
if not intern('abc') is intern('a'+'b'+'c'):
    raise support.TestError("Test2")

