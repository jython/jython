"""
Test some hex conversions.
"""

import support

support.compare('%x' % -1, "ffffffff")

support.compare(str(hex(-1l)), "-0x1L")

if int("0", 16) != 0:
    raise support.TestError("Should return 0")

#Test removed. %x can handle longs in 2.0
#try:
#    print '%x' % 0x100000000L
#except OverflowError, e:
#    support.compare(e, "long int too long")
#else:
#    raise support.TestError("Should raise a OverflowException")
