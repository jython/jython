"""
Test that file writing doesn't attempt to encode things by default and reading
doesn't decode things by default.
"""

import support

EURO_SIGN = u"\u20ac"
try:
    EURO_SIGN.encode()
    import sys
    raise support.TestError('Your default encoding, %s, can handle encoding the Euro sign.  This test needs the default encoding to be unable to handle on its test character' %
            sys.getdefaultencoding())
except UnicodeEncodeError:
    pass

f = open("test207.out", "w")
try:
    f.write(EURO_SIGN)
    raise support.TestError("Shouldn't be able to write out a Euro sign without first encoding")
except UnicodeEncodeError:
    pass
f.close()

f = open("test207.out", "w")
f.write(EURO_SIGN.encode('utf-8'))
f.close()

f = open("test207.out", "r")
encoded_euro = f.read()
f.close()
if encoded_euro != '\xe2\x82\xac' or encoded_euro.decode('utf-8') != EURO_SIGN:
   raise support.TestError("Read something other than the euro sign that we wrote out")
f.close()
