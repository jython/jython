"""
Check calling an instance method with a class instance.
"""

import support

class foo:
    def bar(): return "bar"

try:
    foo.bar()
except TypeError, e:
    support.compare(e, "with \w* ?instance")
else:
    raise support.TestError("Should raise TypeError")


