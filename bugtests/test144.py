"""

"""

import support

class m:
    __var = 0

try:
    m.__var
except AttributeError, e:
    support.compare(e, "class 'm' has no attribute '__var'")
else: 
    raise support.TestError("Should raise")
