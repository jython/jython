"""
[ #475666 ] __nonzero__ exceptions must be ignored
"""

import support


msgs = []

class Foo:
    def __getattr__(self, key):
        msgs.append('getting %s' % key)
        raise KeyError, key

foo = Foo()
if not foo: print 'no foo'

class Foo:
    def __nonzero__(self):
        msgs.append("called __nonzero__")
        raise KeyError

foo = Foo()
try:
    if not foo: print 'no foo'
except KeyError:
    pass
else:
    raise support.TestError('Must raise a keyerror')

support.compare(msgs, "['getting __nonzero__', 'getting __len__', 'called __nonzero__']")

