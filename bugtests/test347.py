"""
[ #490157 ] string.splitlines() - incorrectly splits
"""

import support

r = 'This is a\n multiline string\n'.splitlines()

if r != ['This is a', ' multiline string']:
    raise support.TestError("Wrong splitlines(): %s" % r)

