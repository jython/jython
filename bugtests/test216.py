"""

"""

import support

try:
    import test216s1
except SyntaxError:
    pass
else:
    raise support.TestError("Should raise SyntaxError")
