"""
Unbound methods not indicated properly in repr
"""

import support

class Foo:
     def bar(s): pass
r = repr(Foo.bar)

support.compare(r, "<unbound method")

