"""
Check slice [-1:1:-1].
"""

import support

if 'abcde'[-1:1:-1] != 'edc':
    raise support.TestError("slice [-1:1:-1] failed %s" % 'abcde'[-1:1:-1])

