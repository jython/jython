"""

"""

import support


try:
    import test275s
except SyntaxError:
    pass
else:
    raise support.TestError("Should raise a syntax error")
