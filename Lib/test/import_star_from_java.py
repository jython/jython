# Test material for ImpTestCase.test_import_star (test_import_jy.py)
#
from java.util.regex import *   # Module: java.base
p = Pattern.compile("foo")
assert p.flags() == 0

from java.sql import *          # Module: java.sql
d = Date(1541492230300L)
assert str(d) == '2018-11-06'

from java.awt import *          # Module: java.desktop
assert Color(255,0,255) == Color.MAGENTA

