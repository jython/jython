"""
Test for bug [608628] long(java.math.BigInteger) does not work.
"""

import support

# local name bugtests/test381.py
ns = '10000000000'
import java
ns2 = str(long(java.math.BigInteger(ns)))
assert ns == ns2, ns2



