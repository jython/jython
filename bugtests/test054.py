"""

"""

import support


import time

class Foo:
    def __repr__(self):
        time.sleep(2.0)
        return 'Foo instance'

foo = Foo()
start = time.time()
try:
    foo.bar
except AttributeError:
    pass
end = time.time()

if end-start > 1:
    raise support.testError("Throwing AttributeError too slow")

