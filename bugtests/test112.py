"""
Check slice [::-1]
"""

import support

if 'abcdef'[::-1] != 'fedcba':
    raise support.TestError("slice [::-1] failed")
