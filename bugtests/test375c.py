"""
[ 631017 ] Private fields mismangled
"""

import support

class _A:
    __value = 1

class B(_A):
    _initial_value = 2
    def foo(self):
        assert self._A__value == 1
        assert self._initial_value == 2


B().foo()

#raise support.TestWarning('A test of TestWarning. It is not an error')
