"""

"""

import support

try:
    import test280c
except UnboundLocalError:
    pass
else:
    raise support.TestError("Should raise UnboundLocalError")
