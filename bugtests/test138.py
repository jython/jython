"""

"""

import support

msg = 'This is a test.'

try:
    msg.startsWith('This')
except AttributeError, e:
    pass
else:
    raise support.TestError("startsWith should not be defined")

if not msg.startswith('This'):
    raise support.TestError("startswith error")
