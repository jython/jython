"""

"""

import support


try:
    [i for i in range(10)] = (1, 2, 3) 
except SyntaxError:
    pass
else:
    raise support.TestError("Should raise a syntax error")
