"""
Test lost syntax error when auto importing submodules.
"""

import support

import test293p
try:
    test293p.submod.func()
except SyntaxError:
    pass
else:
    raise support.TestError('should raise a syntax error')
